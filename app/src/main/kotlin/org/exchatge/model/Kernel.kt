/*
 * Exchatge - a secured realtime message exchanger (Android client).
 * Copyright (C) 2023-2024  Vadim Nikolaev (https://github.com/vadniks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.exchatge.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings.Secure
import androidx.annotation.VisibleForTesting
import org.exchatge.model.database.Conversation
import org.exchatge.model.database.Database
import org.exchatge.model.database.Message
import org.exchatge.model.net.MAX_MESSAGE_BODY_SIZE
import org.exchatge.model.net.Net
import org.exchatge.model.net.Net.Companion.MAX_FILENAME_SIZE
import org.exchatge.model.net.NetInitiator
import org.exchatge.model.net.UNHASHED_PASSWORD_SIZE
import org.exchatge.model.net.USERNAME_SIZE
import org.exchatge.model.net.UserInfo
import org.exchatge.model.net.byte
import org.exchatge.model.net.bytes
import org.exchatge.presenter.PresenterImpl
import org.exchatge.presenter.PresenterInitiator
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.locks.ReentrantReadWriteLock

class Kernel(val context: Context) {
    val crypto = Crypto()
    @Volatile var net: Net? = null; private set
    val presenter = PresenterImpl(PresenterInitiatorImpl())
    private val sharedPrefs = sharedPreferences(this::class.simpleName!!)
    private val encryptionKey: ByteArray
    private val wasLoggedIn get() = sharedPrefs.getString(CREDENTIALS, null) != null
    private val users = ArrayList<UserInfo>()
    private val rwLock = ReentrantReadWriteLock()
    @Volatile var database = null as Database?; private set
    private val maxUnencryptedMessageBodySize get() = MAX_MESSAGE_BODY_SIZE - crypto.encryptedSize(0) // strange bug occurs without get() - runtime value becomes zero regardless of assigned value
    @Volatile private var registrationPending = false
    private val userIdsToFetchMessagesFrom = LinkedList<Int>() as Queue<Int>
    @Volatile private var missingMessagesFetchers = 0
    private val options = loadOptions()
    private var fileInputStream: InputStream? = null
    @Volatile private var toUserId = 0
    @Volatile private var fileBytesCounter = 0
    private var fileOutputStream: OutputStream? = null
    private var fileHashState: ByteArray? = null

    // TODO: add settings to ui to adjust options which will be stored as sharedPreferences

    init {
        assert(!initialized)
        initialized = true

        println( // Figlet
            """
                       _______ _      _ _______ _     _ _______ _______  ______ _______            
                       |______  \\___/  |       |_____| |_____|    |    |  ____ |______           
                       |______ _/   \\_ |_____  |     | |     |    |    |_____| |______           
            Exchatge (Client) Copyright (C) 2023-2024  Vadim Nikolaev (https://github.com/vadniks)
                               This program comes with ABSOLUTELY NO WARRANTY;                    
            This is free software, and you are welcome to redistribute it under certain conditions""".trimIndent(),
        )

        @SuppressLint("HardwareIds")
        encryptionKey = crypto.makeKey((
            Secure.getString(context.contentResolver, Secure.ANDROID_ID).hashCode()
            xor this::class.qualifiedName!!.hashCode()
        ).bytes)
    }

    private fun initializeNet() {
        assert(net == null)
        net = Net(NetInitiatorImpl())
    }

    fun sharedPreferences(name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun decryptCredentials(encrypted: String): Pair<String, String>? {
        val decrypted =
            crypto.decryptSingle(encryptionKey, crypto.hexDecode(encrypted) ?: return null)
            ?: return null

        return (
            String(decrypted.sliceArray(0 until USERNAME_SIZE))
            to String(decrypted.sliceArray(USERNAME_SIZE until USERNAME_SIZE + UNHASHED_PASSWORD_SIZE))
        )
    }

    private fun credentials() = sharedPrefs.getString(CREDENTIALS, null).let { if (it != null) decryptCredentials(it) else null }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun encryptCredentials(credentials: Pair<String, String>): String {
        val combined = ByteArray(USERNAME_SIZE + UNHASHED_PASSWORD_SIZE)
        credentials.first.toByteArray().let { System.arraycopy(it, 0, combined, 0, it.size) }
        credentials.second.toByteArray().let { System.arraycopy(it, 0, combined, USERNAME_SIZE, it.size) }

        val encrypted = crypto.encryptSingle(encryptionKey, combined)
        return crypto.hexEncode(encrypted!!)
    }

    @SuppressLint("ApplySharedPref")
    private fun setCredentials(credentials: Pair<String, String>?) = sharedPrefs.edit().apply {
        if (credentials == null) remove(CREDENTIALS) else putString(CREDENTIALS, encryptCredentials(credentials))
    }.commit() // TODO: fill with mess before drop

    private fun findUser(id: Int) = rwLock.readLocked {
        val index = Collections.binarySearch(users as List<Any>, id) { user, xId ->
            user as UserInfo
            xId as Int // bypass type safety for a moment to do a little hack, porting this from low level C code after all
            // https://en.cppreference.com/w/c/algorithm/bsearch see example section
            (user.id > xId).byte - (user.id < xId).byte
        }

        if (index >= 0) users[index] else null
    }

    private fun loadOptions() = sharedPrefs.run {
        Options(
            getString(HOST, null) ?: DEFAULT_HOST,
            getInt(PORT, DEFAULT_PORT),
            (getString(SSKP, null) ?: DEFAULT_SSKP)
        )
    }

    private inner class PresenterInitiatorImpl : PresenterInitiator {
        @Volatile private var triedLogIn = false
        override val currentUserId get() = net!!.userId
        override val maxMessagePlainPayloadSize get() = (maxUnencryptedMessageBodySize / Crypto.PADDING_BLOCK_SIZE) * Crypto.PADDING_BLOCK_SIZE
        override val maxBroadcastMessageSize get() = 64
        private val maxFileSize = 1024 * 1024 * 20

        override fun onActivityCreate() {}

        override fun credentialsLengthCorrect(username: String, password: String) =
            username.length in 1..USERNAME_SIZE && password.length in 1..UNHASHED_PASSWORD_SIZE

        override fun scheduleLogIn() {
            triedLogIn = true
            assert(net == null)
            runAsync(1000, this@Kernel::initializeNet)
        }

        override fun scheduleRegister() = runAsync {
            registrationPending = true
            assert(net == null)
            runAsync(1000, this@Kernel::initializeNet)
        }

        override fun scheduleUsersFetch() {
            assert(net != null)
            runAsync(action = net!!::fetchUsers)
        }

        override fun admin(id: Int) = id == 0

        override fun scheduleLogOut() = runAsync {
            setCredentials(null)
            net!!.disconnect()
        }

        override fun onConversationSetupDialogAction(accepted: Boolean, requestedByHost: Boolean, opponentId: Int) {
            presenter.hideConversationSetupDialog()
            if (requestedByHost && !accepted) return
            presenter.onSettingUpConversation(null)

            runAsync {
                val coders =
                    if (!requestedByHost) net!!.replyToConversationSetUpInvite(accepted, opponentId)
                    else net!!.createConversation(opponentId)

                if (database!!.conversationDao.exists(opponentId))
                    database!!.conversationDao.remove(opponentId)

                if (coders != null)
                    database!!.conversationDao.add(Conversation(opponentId, crypto.exportCoders(coders)))

                presenter.onSettingUpConversation(coders != null)
            }
        }

        override fun shutdownServer() = runAsync { net!!.shutdownServer() }

        override fun sendBroadcast(text: String) = runAsync {
            net!!.sendBroadcast(text.toByteArray().also { assert(it.size in 1 .. maxBroadcastMessageSize) })
        }

        private fun removeConversation(id: Int, conversationExists: Boolean) {
            presenter.removeConversation(null)

            if (conversationExists) {
                database!!.conversationDao.remove(id)
                database!!.messagesDao.removeSeveral(id)
                presenter.removeConversation(true)
            } else
                presenter.removeConversation(false)
        }

        override fun onConversationRequested(id: Int, remove: Boolean) = runAsync {
            val conversationExists = database!!.conversationDao.exists(id)

            if (remove) {
                removeConversation(id, conversationExists)
                return@runAsync
            }

            val user = findUser(id)!!
            val username = String(user.name)

            if (conversationExists) {
                presenter.showConversation(id, username)
                return@runAsync
            }

            if (user.connected) // TODO: rename to online
                presenter.showConversationSetUpDialog(true, id, username)
            else
                presenter.notifyUserOpponentIsOffline()
        }

        override fun onFileChosen(intent: Intent, opponentId: Int) = runAsync {
            fileBytesCounter = 0

            if (findUser(opponentId)?.connected != true) {
                presenter.onFileSendResult(null)
                return@runAsync
            }

            val resolver = context.contentResolver
            val size = resolver.let {
                val descriptor = it.openFileDescriptor(intent.data ?: return@runAsync , "r") ?: return@runAsync
                descriptor.statSize.also { descriptor.close() }
            }

            val result = sendFile(
                opponentId,
                intent.getStringExtra(Intent.EXTRA_TITLE) ?: System.currentTimeMillis().toString(),
                size
            ) { resolver.openInputStream(intent.data ?: return@sendFile null) }

            log("ofc", result)
            presenter.onFileSendResult(result)
        }

        private fun sendFile(opponentId: Int, filename: String, size: Long, inputStreamOpener: () -> InputStream?): Boolean {
            log("sf", size, filename.length, filename)

            if (size > maxFileSize || filename.length > MAX_FILENAME_SIZE) return false
            fileInputStream = inputStreamOpener() ?: return false

            val checksum = calculateFileChecksum(fileInputStream!!)
            fileInputStream!!.close()
            if (checksum == null) return false

            fileInputStream = inputStreamOpener() ?: return false
            toUserId = opponentId

            val result = net!!.exchangeFile(opponentId, size.toInt(), checksum, filename.toByteArray())
            fileInputStream!!.close()

            return result
        }

        private fun calculateFileChecksum(inputStream: InputStream): ByteArray? {
            val bufferSize = maxUnencryptedMessageBodySize
            val buffer = ByteArray(bufferSize)

            val state = crypto.hashMultipart(null, null)
            var read = false
            var count: Int

            while (inputStream.read(buffer).also { count = it } > 0) {
                read = true
                crypto.hashMultipart(state, buffer.sliceArray(0 until count))
                for (i in buffer.indices) buffer[i] = 0
            }

            return if (!read) null else crypto.hashMultipart(state, null)
        }

        override fun sendMessage(to: Int, text: String, millis: Long) = runAsync {
            val bytes = text.toByteArray()
            assert(bytes.isNotEmpty())
            database!!.messagesDao.add(Message(millis, to, currentUserId, bytes))

            val padded = crypto.addPadding(bytes)
            assert(crypto.encryptedSize(padded.size) <= MAX_MESSAGE_BODY_SIZE)

            val encrypted = crypto.encrypt(crypto.recreateCoders(database!!.conversationDao.getCoders(to)!!), padded)!!
            net!!.send(encrypted, to)
        }

        override fun loadSavedMessages(conversation: Int) = database!!.messagesDao.getSeveral(conversation)

        override fun username(id: Int) = findUser(id)?.name?.let { String(it) }

        @SuppressLint("ApplySharedPref")
        override fun saveOptions(host: String, port: Int, sskp: String) = sharedPrefs.edit().apply {
            putString(HOST, host)
            putInt(PORT, port)
            putString(SSKP, sskp)
        }.commit().unit

        override fun loadOptions() = options.let { Options(it.host, it.port, it.sskp) }

        override fun tryScheduleAutoLogIn() =
            if (!wasLoggedIn || triedLogIn || net != null) false
            else { scheduleLogIn(); true }

        override fun onFileExchangeDialogAction(accepted: Boolean, opponentId: Int, fileSize: Int, fileName: String) = runAsync {
            val dir = context.getExternalFilesDir(null)
            if (dir == null) {
                net!!.replyToFileExchangeInvite(opponentId, fileSize, false)
                presenter.onFileExchangeDone(false)
                return@runAsync
            }

            val file = File(dir, fileName)
            fileOutputStream = FileOutputStream(file)
            val result = net!!.replyToFileExchangeInvite(opponentId, fileSize, accepted)

            fileOutputStream!!.close()
            fileOutputStream = null
            if (!result) file.delete()

            presenter.onFileExchangeDone(result)
        }

        override fun onActivityDestroy() {}
    }

    private inner class NetInitiatorImpl : NetInitiator {
        override val context get() = this@Kernel.context
        override val crypto get() = this@Kernel.crypto

        override fun loadOptions(): Options = options

        override fun onConnectResult(successful: Boolean) = runAsync {
            if (successful) {
                val (username, password) = credentials() ?: presenter.credentials

                if (!registrationPending)
                    net!!.logIn(username, password)
                else
                    net!!.register(username, password)

                registrationPending = false
            } else
                presenter.onConnectFail()
        }

        override fun onNetDestroy() {
            database?.close()
            database = null
            log("ond db close")
            net = null
            runAsync(action = presenter::onDisconnected)

            fileInputStream?.close()
            fileInputStream = null
            fileOutputStream?.close()
            fileOutputStream = null
        }

        override fun onLogInResult(successful: Boolean) = runAsync {
            if (successful) {
                if (!wasLoggedIn) setCredentials(presenter.credentials)

                assert(database == null)
                database = Database.init(context, encryptionKey.copyOf(), this@Kernel.crypto)
            } else {
                if (wasLoggedIn) setCredentials(null)
            }

            presenter.onLogInResult(successful)
        }

        override fun onRegisterResult(successful: Boolean) = presenter.onRegisterResult(successful)

        private fun fetchMissingMessagesFromUser(id: Int) {
            missingMessagesFetchers++

            val timestamp = database!!.messagesDao.getMostRecentMessageTimestamp(id)
            val minimalPossibleTimestamp = database!!.conversationDao.getTimestamp(id)!! + 1

            assert(timestamp < System.currentTimeMillis())
            net!!.fetchMessages(id, if (timestamp < minimalPossibleTimestamp) minimalPossibleTimestamp else timestamp)
        }

        override fun onNextUserFetched(user: UserInfo, last: Boolean) = runAsync { // TODO: test with large amount of users
            val conversationExists = database!!.conversationDao.exists(user.id)

            rwLock.writeLocked {
                if (user.id == 0) { // first
                    assert(!last)
                    users.clear()

                    assert(missingMessagesFetchers == 0)
                    userIdsToFetchMessagesFrom.clear()
                    net!!.ignoreUsualMessages = true
                }

                users.add(user)

                if (conversationExists)
                    userIdsToFetchMessagesFrom.add(user.id)
            }

            presenter.onNextUserFetched(user, conversationExists, last)
            if (!last) return@runAsync

            val nextIdToFetchMessagesFrom = rwLock.writeLocked { userIdsToFetchMessagesFrom.poll() }
            if (nextIdToFetchMessagesFrom == null) {
                net!!.ignoreUsualMessages = false
                presenter.onMessagesFetched(true)
            } else
                fetchMissingMessagesFromUser(nextIdToFetchMessagesFrom)
        }

        override fun onConversationSetUpInviteReceived(fromId: Int) = runAsync {
            presenter.showConversationSetUpDialog(false, fromId, String(findUser(fromId)!!.name))
        }

        override fun onMessageReceived(timestamp: Long, from: Int, body: ByteArray) = runAsync {
            assert(body.size - crypto.encryptedSize(0) in 1..maxUnencryptedMessageBodySize)
            val coders = crypto.recreateCoders(database!!.conversationDao.getCoders(from) ?: return@runAsync)

            val padded = crypto.decrypt(coders, body)!!
            val message = crypto.removePadding(padded)!!

            database!!.messagesDao.add(Message(timestamp, from, from, message))
            presenter.onMessageReceived(from, timestamp, String(message))
        }

        override fun onBroadcastReceived(body: ByteArray) = presenter.onBroadcastReceived(String(body))

        override fun onNextMessageFetched(from: Int, timestamp: Long, body: ByteArray?, last: Boolean) = runAsync {
            assert(body != null && body.isNotEmpty() || body == null)
            if ((body?.size ?: 0) > 0) onMessageReceived(timestamp, from, body!!)

            assert(missingMessagesFetchers > 0)
            if (!last) return@runAsync

            missingMessagesFetchers--
            assert(missingMessagesFetchers >= 0)

            val nextIdToFetchMessagesFrom = rwLock.writeLocked { userIdsToFetchMessagesFrom.poll() }
            if (nextIdToFetchMessagesFrom != null)
                runAsync { fetchMissingMessagesFromUser(nextIdToFetchMessagesFrom) }

            assert(missingMessagesFetchers == rwLock.readLocked { userIdsToFetchMessagesFrom.size })
            if (missingMessagesFetchers > 0) return@runAsync

            userIdsToFetchMessagesFrom.clear()
            net!!.ignoreUsualMessages = false
            presenter.onMessagesFetched(false)
        }

        override fun nextFileChunkSupplier(index: Int, buffer: ByteArray): Int {
            assert(fileInputStream != null)

            val targetSize = maxUnencryptedMessageBodySize
            val unencryptedBuffer = ByteArray(targetSize)
            val actualSize = fileInputStream!!.read(unencryptedBuffer)

            if (actualSize > 0) {
                val coders = crypto.recreateCoders(database!!.conversationDao.getCoders(toUserId)!!)
                val encryptedSize = crypto.encryptedSize(actualSize)
                assert(encryptedSize <= MAX_MESSAGE_BODY_SIZE)

                val encryptedChunk = crypto.encrypt(coders, unencryptedBuffer.sliceArray(0 until actualSize))!!
                System.arraycopy(encryptedChunk, 0, buffer, 0, encryptedSize)

                if (index == 0) assert(fileBytesCounter == 0)
                fileBytesCounter += actualSize
                return encryptedSize
            }

            assert(index > 0)
            return 0
        }

        override fun onFileExchangeInviteReceived(from: Int, fileSize: Int, hash: ByteArray, filename: ByteArray) = runAsync {
            val user = findUser(from)
            if (user == null) {
                net!!.replyToFileExchangeInvite(from, fileSize, false)
                return@runAsync
            }

            fileBytesCounter = 0
            presenter.onFileExchangeInviteReceived(String(user.name), user.id, fileSize, String(filename))
        }

        override fun nextFileChunkReceiver(from: Int, index: Int, receivedBytesCount: Int, buffer: ByteArray) {
            assert(fileOutputStream != null)

            val decryptedSize = receivedBytesCount - crypto.encryptedSize(0)
            assert(decryptedSize in 1..maxUnencryptedMessageBodySize)

            val coders = crypto.recreateCoders(database!!.conversationDao.getCoders(from)!!)

            val decrypted = crypto.decrypt(coders, buffer)!!
            fileOutputStream!!.write(decrypted)

            if (index == 0) {
                assert(fileHashState == null)
                fileHashState = crypto.hashMultipart(null, null)
            }
            crypto.hashMultipart(fileHashState!!, decrypted)

            if (index == 0) assert(fileBytesCounter == 0)
            fileBytesCounter += decryptedSize
        }
    }

    private companion object {
        @JvmStatic
        private var initialized = false

        private const val CREDENTIALS = "credentials"
        private const val HOST = "host"
        private const val PORT = "port"
        private const val SSKP = "sskp"

        private const val DEFAULT_HOST = "192.168.1.57"
        private const val DEFAULT_PORT = 8080
        private const val DEFAULT_SSKP = "255 23 21 243 148 177 186 0 73 34 173 130 234 251 83 130 138 54 215 5 170 139 175 148 71 215 74 172 27 225 26 249"
    }
}
