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
import android.content.SharedPreferences
import android.provider.Settings.Secure
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.exchatge.model.database.Conversation
import org.exchatge.model.database.Database
import org.exchatge.model.database.Message
import org.exchatge.model.net.MAX_MESSAGE_BODY_SIZE
import org.exchatge.model.net.Net
import org.exchatge.model.net.NetInitiator
import org.exchatge.model.net.UNHASHED_PASSWORD_SIZE
import org.exchatge.model.net.USERNAME_SIZE
import org.exchatge.model.net.UserInfo
import org.exchatge.model.net.byte
import org.exchatge.model.net.bytes
import org.exchatge.presenter.PresenterImpl
import org.exchatge.presenter.PresenterInitiator
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

    // TODO: add settings to ui to adjust options which will be stored as sharedPreferences

    init {
        assert(!initialized)
        initialized = true

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

    private inner class PresenterInitiatorImpl : PresenterInitiator {
        @Volatile private var triedLogIn = false
        override val currentUserId get() = net!!.userId
        override val maxMessagePlainPayloadSize get() = (maxUnencryptedMessageBodySize / Crypto.PADDING_BLOCK_SIZE) * Crypto.PADDING_BLOCK_SIZE
        override val maxBroadcastMessageSize get() = 64

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

            val user = findUser(id) ?: return@runAsync
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

        override fun onActivityResume() =
            if (!wasLoggedIn || triedLogIn || net != null) false
            else { scheduleLogIn(); true }

        override fun onActivityDestroy() {}
    }

    private inner class NetInitiatorImpl : NetInitiator {
        override val context get() = this@Kernel.context
        override val crypto get() = this@Kernel.crypto

        override fun onConnectResult(successful: Boolean) = runAsync {
            log("222")
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
            val conversationExists = database!!.conversationDao.exists(user.id) // TODO: instead of getting users one by one, get all users in a bundle, this would eliminate thread race problem

            rwLock.writeLocked {
                if (user.id == 0) { // first
                    assert(!last)
                    users.clear()

                    assert(missingMessagesFetchers == 0)
                    userIdsToFetchMessagesFrom.clear()
                    net!!.ignoreUsualMessages = true
                }

                users.add(user) // TODO: maybe add sort()
                log("aaa", user)

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
            rwLock.readLocked { log(users) } // TODO: replace arrayList with vector
            presenter.showConversationSetUpDialog(false, fromId, String(findUser(fromId)!!.name))
        }

        override fun onMessageReceived(timestamp: Long, from: Int, body: ByteArray) = runAsync {
            assert(body.size - crypto.encryptedSize(0) in 1..maxUnencryptedMessageBodySize)
            val coders = crypto.recreateCoders(database!!.conversationDao.getCoders(from) ?: return@runAsync)

            val padded = crypto.decrypt(coders, body)!!
            val message = crypto.removePadding(padded)!!

            database!!.messagesDao.add(Message(timestamp, from, from, message))
            val user = findUser(from)
            presenter.onMessageReceived(timestamp, if (user!!.id == net!!.userId) null else String(user.name), String(message))
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
    }

    private companion object {
        @JvmStatic
        private var initialized = false

        private const val CREDENTIALS = "credentials"
    }
}
