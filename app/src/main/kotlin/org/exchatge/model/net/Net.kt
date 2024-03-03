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

package org.exchatge.model.net

import android.content.Intent
import org.exchatge.model.Crypto
import org.exchatge.model.Reference
import org.exchatge.model.Ternary
import org.exchatge.model.assert
import org.exchatge.model.log
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

private const val SERVER_ADDRESS = "192.168.1.57" // TODO: debug only

private val serverSignPublicKey = byteArrayOf( // TODO: debug only
    255.toByte(), 23, 21, 243.toByte(), 148.toByte(), 177.toByte(), 186.toByte(), 0,
    73, 34, 173.toByte(), 130.toByte(), 234.toByte(), 251.toByte(), 83, 130.toByte(),
    138.toByte(), 54, 215.toByte(), 5, 170.toByte(), 139.toByte(), 175.toByte(), 148.toByte(),
    71, 215.toByte(), 74, 172.toByte(), 27, 225.toByte(), 26, 249.toByte()
)

class Net(private val initiator: NetInitiator) {
    val running get() = NetService.running
    @Volatile private var socket: Socket? = null
    private val lock = this
    val connected get() = synchronized(lock) { socket != null && !socket!!.isClosed && socket!!.isConnected }
    private val crypto get() = initiator.crypto
    private val encryptedMessageMaxSize = crypto.encryptedSize(MAX_MESSAGE_SIZE)
    private var coders: Crypto.Coders? = null
    @Volatile private var authenticated = false // volatile: reads and writes to this field are atomic and writes are always made visible to other threads - just an atomic flag
    @Volatile var userId = FROM_ANONYMOUS; private set
    private val tokenAnonymous = ByteArray(TOKEN_SIZE)
    private val tokenUnsignedValue = ByteArray(TOKEN_UNSIGNED_VALUE_SIZE) { 255.toByte() }
    @Volatile private var token = tokenAnonymous.copyOf()
    @Volatile private var fetchingUsers = false
    @Volatile private var fetchingMessages = false
    @Volatile private var destroyed = false
    @Volatile private var settingUpConversation = false
    @Volatile private var exchangingFile = false
    private val conversationSetupMessages = ConcurrentLinkedQueue<NetMessage>()
    @Volatile private var inviteProcessingStartMillis = 0L

    init {
        log("net init")
        if (!NetService.running)
            initiator.context.startService(Intent(initiator.context, NetService::class.java))!! // TODO: start the service only if the user has logged in
    }

    fun onCreate() {}

    fun onPostCreate() {
        assert(!destroyed)

        synchronized(lock) {
            socket =
                try { Socket().apply { connect(InetSocketAddress(SERVER_ADDRESS, 8080), /*TODO: extract constant*/1000 * 60 * 60) } }
                catch (_: Exception) { null } // unable to connect
        }

        log("connected = " + socket?.isConnected)
        if (socket == null) {
            initiator.onConnectResult(false)
            return
        }
        synchronized(lock) { socket!!.soTimeout = 500 } // delay between socket read tries (while (open) { delay(500); tryRead() })

        val ready = initiateSecuredConnection()
        log("ready = $ready") // if (!ready) // error while connecting

        if (!ready) {
            socket!!.close()
            socket = null
            initiator.onConnectResult(false)
            destroyed = true
            return
        }

        initiator.onConnectResult(true)
    }

    private fun initiateSecuredConnection(): Boolean {
        fun read(buffer: ByteArray) = this.read(buffer) == Ternary.POSITIVE

        val signedServerPublicKey = ByteArray(Crypto.SIGNATURE_SIZE + Crypto.KEY_SIZE)
        if (!read(signedServerPublicKey)) return false

        val serverPublicKey = signedServerPublicKey.sliceArray(Crypto.SIGNATURE_SIZE until signedServerPublicKey.size)

        assert(crypto.checkServerSignedBytes(
            signedServerPublicKey.sliceArray(0 until Crypto.SIGNATURE_SIZE),
            serverPublicKey,
            serverSignPublicKey
        ))

        if (serverPublicKey contentEquals ByteArray(Crypto.KEY_SIZE) { 0 }) return false // denial of service

        val keys = crypto.exchangeKeys(serverPublicKey) ?: return false
        if (!write(crypto.clientPublicKey(keys))) return false

        val signedServerCoderHeader = ByteArray(Crypto.SIGNATURE_SIZE + Crypto.HEADER_SIZE)
        if (!read(signedServerCoderHeader)) return false

        val serverCoderHeader = signedServerCoderHeader.sliceArray(Crypto.SIGNATURE_SIZE until signedServerCoderHeader.size)

        assert(crypto.checkServerSignedBytes(
            signedServerCoderHeader.sliceArray(0 until Crypto.SIGNATURE_SIZE),
            serverCoderHeader,
            serverSignPublicKey
        ))

        coders = crypto.makeCoders()
        val clientCoderHeader = crypto.initializeCoders(coders!!, keys, serverCoderHeader) ?: return false
        return write(clientCoderHeader)
    }

    private fun read(buffer: ByteArray) =
        try {
            if (synchronized(lock) { socket!!.getInputStream().read(buffer) } == buffer.size)
                Ternary.POSITIVE
            else
                Ternary.NEGATIVE // disconnected
        }
        catch (e: SocketTimeoutException) { Ternary.NEUTRAL } // timeout
        catch (_: Exception) { Ternary.NEGATIVE } // error - disconnect

    private fun write(buffer: ByteArray) =
        try { synchronized(lock) { socket!!.getOutputStream().write(buffer) }; true }
        catch (_: Exception) { false }

    private fun receive(disconnected: Reference<Boolean>): NetMessage? {
        val sizeBytes = ByteArray(4)
        when (read(sizeBytes)) {
            Ternary.NEUTRAL -> return null // timeout - no new messages so far
            Ternary.NEGATIVE -> {
                disconnected.referenced = true // error or connection closed
                return null
            }
            Ternary.POSITIVE -> Unit // proceed
        }

        val size = sizeBytes.int
        assert(size in 0..encryptedMessageMaxSize)

        val buffer = ByteArray(size)
        when (read(buffer)) {
            Ternary.NEUTRAL -> return null // timeout - no new messages so far
            Ternary.NEGATIVE -> {
                disconnected.referenced = true // error or connection closed
                return null
            }
            Ternary.POSITIVE -> Unit // proceed
        }

        val decrypted = synchronized(lock) { crypto.decrypt(coders!!, buffer) }
        assert(decrypted != null)

        disconnected.referenced = false
        return NetMessage.unpack(decrypted!!)
    }

    private fun send(flag: Int, body: ByteArray?, to: Int): Boolean {
        assert(body != null && body.size in 1..MAX_MESSAGE_BODY_SIZE || body == null && flag != FLAG_PROCEED && flag != FLAG_BROADCAST)

        val message = NetMessage(flag, body, to, userId, token)

        val packedSize = MESSAGE_HEAD_SIZE + message.size
        val packed = message.pack()
        assert(packed.size == packedSize)

        val encrypted = synchronized(lock) { crypto.encrypt(coders!!, packed) }
        assert(encrypted != null)

        val encryptedSize = crypto.encryptedSize(packedSize)
        assert(encryptedSize <= encryptedMessageMaxSize)
        assert(encrypted!!.size == encryptedSize)

        val buffer = ByteArray(4 + encryptedSize)
        System.arraycopy(encryptedSize.bytes, 0, buffer, 0, 4)
        System.arraycopy(encrypted, 0, buffer, 4, encryptedSize)

        return write(buffer)
    }

    fun send(body: ByteArray, to: Int) {
        assert(running && connected && authenticated && !destroyed && body.isNotEmpty() && to >= 0)
        send(FLAG_PROCEED, body, to)
    }

    // TODO: add an 'exit' button to UI which will close the activity as well as the service to completely shutdown the whole app

    fun listen() {
        while (running && connected) {
            log("listen")
            if (tryReadMessage() == Ternary.NEGATIVE) break
        }
        log("disconnected") // disconnected - logging in is required to reconnect // TODO: handle disconnection
        // then the execution goes to onDestroy
    }

    private fun tryReadMessage(): Ternary {
        val disconnected = Reference(false)
        val received = receive(disconnected)

        return when {
            received != null -> {
                processMessage(received)
                Ternary.POSITIVE
            }
            disconnected.referenced -> Ternary.NEGATIVE
            else -> Ternary.NEUTRAL
        }
    }

    private fun processMessage(message: NetMessage) {
        assert(running && connected && !destroyed)
        log("message: $message")

        if (message.from == FROM_SERVER) {
            processMessageFromServer(message)
            return
        }

        assert(authenticated)

        when (message.flag) {
            FLAG_EXCHANGE_KEYS, FLAG_EXCHANGE_KEYS_DONE, FLAG_EXCHANGE_HEADERS, FLAG_EXCHANGE_HEADERS_DONE -> {
                if (message.flag == FLAG_EXCHANGE_KEYS && message.size == INVITE_ASK)
                    processConversationSetUpMessage(message)
                else
                    conversationSetupMessages.add(message)
            }
            FLAG_FILE_ASK -> {}
            FLAG_FILE -> {}
            FLAG_PROCEED -> {
                assert(message.body != null)
                if (!fetchingUsers && !fetchingMessages /*&& !ignoreUsualMessages*/) // TODO
                    initiator.onMessageReceived(message.timestamp, message.from, message.body!!)
            }
            FLAG_FETCH_MESSAGES -> onNextMessageFetched(message)
            else -> assert(false)
        }
    }

    private fun processMessageFromServer(message: NetMessage) {
        assert(crypto.checkServerSignedBytes(
            message.token.sliceArray(0 until Crypto.SIGNATURE_SIZE),
            tokenUnsignedValue,
            serverSignPublicKey
        ))

        when (message.flag) {
            FLAG_LOGGED_IN -> onLoggedIn(message)
            FLAG_REGISTERED -> log("register succeeded")
            FLAG_FETCH_USERS -> onNextUserInfosBundleFetched(message)
            FLAG_ERROR -> processErrors(message)
            FLAG_FETCH_MESSAGES -> onEmptyMessagesFetchReplyReceived(message)
            FLAG_BROADCAST -> {
                assert(message.size > 0)
                initiator.onBroadcastReceived(message.body!!)
            }
        }
    }

    private fun onLoggedIn(message: NetMessage) {
        assert(running && connected && !destroyed && !authenticated)
        log("log in succeeded")

        assert(message.body != null)
        authenticated = true
        userId = message.to
        token = message.body!!.sliceArray(0 until TOKEN_SIZE)

        initiator.onLogInResult(true)
    }

    private fun processErrors(message: NetMessage) {
        assert(running && connected && !destroyed && message.size == 4 && message.body != null)
        when (val flag = message.body!!.sliceArray(0 until 4).int) {
            FLAG_LOG_IN -> {
                log("log in failed")
                initiator.onLogInResult(false)
                disconnect()
            }
            FLAG_REGISTER -> log("register failed")
            FLAG_FETCH_MESSAGES -> log("fetch messages failed")
            else -> log("error $flag received")
        }
    }

    private fun onNextUserInfosBundleFetched(message: NetMessage) {
        assert(running && connected && authenticated && !destroyed && fetchingUsers)
        assert(message.body != null && message.size in 1..MAX_MESSAGE_BODY_SIZE)
        val last = message.index == message.count - 1

        for (i in 0 until message.size step USER_INFO_SIZE)
            initiator.onNextUserFetched(
                UserInfo.unpack(message.body!!.sliceArray(i until i + USER_INFO_SIZE)),
                last && i == message.size - USER_INFO_SIZE
            )

        if (!last) return
        log("users fetched")
        fetchingUsers = false
    }

    private fun onNextMessageFetched(message: NetMessage) {
        assert(running && connected && authenticated && !destroyed && fetchingMessages && message.body != null)
        val last = message.index == message.count - 1

        log("next message fetched $message") // TODO: handle
        if (last) fetchingMessages = false
    }

    private fun onEmptyMessagesFetchReplyReceived(message: NetMessage) {
        assert(running && connected && authenticated && !destroyed && message.body != null && message.count == 1)
        log("empty messages fetch reply $message " + message.body!!.sliceArray(1 + 8 until 1 + 8 + 4).int) // TODO: handle
        fetchingMessages = false
    }

    private fun processConversationSetUpMessage(message: NetMessage) {
        assert(running && connected && authenticated && !destroyed && message.body != null && message.flag == FLAG_EXCHANGE_KEYS && message.size == INVITE_ASK)
        if (settingUpConversation || exchangingFile) return

        settingUpConversation = true
        inviteProcessingStartMillis = System.currentTimeMillis()

        initiator.onConversationSetUpInviteReceived(message.from)
    }

    private fun makeCredentials(username: String, password: String): ByteArray {
        assert(username.length in 1..USERNAME_SIZE && password.length in 1..UNHASHED_PASSWORD_SIZE)
        val credentials = ByteArray(USERNAME_SIZE + UNHASHED_PASSWORD_SIZE)

        System.arraycopy(username.toByteArray(), 0, credentials, 0, username.length)
        System.arraycopy(password.toByteArray(), 0, credentials, USERNAME_SIZE, password.length)
        return credentials
    }

    fun disconnect() = synchronized(lock) { // ...and reset, after this the module should be recreated
        assert(running && connected && !destroyed)
        socket!!.close()
        socket = null
        destroyed = true
    }

    fun logIn(username: String, password: String) {
        assert(running && connected && !authenticated && !destroyed)
        send(FLAG_LOG_IN, makeCredentials(username, password), TO_SERVER)
    }

    fun register(username: String, password: String) {
        assert(running && connected && !authenticated && !destroyed)
        send(FLAG_REGISTER, makeCredentials(username, password), TO_SERVER)
    }

    fun shutdownServer() {
        assert(running && connected && authenticated && !destroyed)
        send(FLAG_SHUTDOWN, null, TO_SERVER)
    }

    fun fetchUsers() {
        assert(running && connected && authenticated && !destroyed && !fetchingUsers && !fetchingMessages)
        fetchingUsers = true
        send(FLAG_FETCH_USERS, null, TO_SERVER)
    }

    fun sendBroadcast(body: ByteArray) {
        assert(running && connected && authenticated && !destroyed && body.isNotEmpty())
        send(FLAG_BROADCAST, body, TO_SERVER)
    }

    fun fetchMessages(id: Int, afterTimestamp: Long) {
        assert(running && connected && authenticated && !destroyed && id >= 0 && afterTimestamp >= 0 && !fetchingUsers && !fetchingMessages)
        fetchingMessages = true

        val body = ByteArray(1 + 8 + 4)
        body[0] = 1
        System.arraycopy(afterTimestamp.bytes, 0, body, 1, 8)
        System.arraycopy(id.bytes, 0, body, 1 + 8, 4)

        send(FLAG_FETCH_MESSAGES, body, TO_SERVER)
    }

    private fun <T> Queue<T>.waitAndPop(timeout: Int = TIMEOUT): T? {
        assert(timeout >= 0)
        val started = System.currentTimeMillis()

        while (System.currentTimeMillis() - started < timeout) {
            val item = poll()
            if (item != null)
                return item
        }

        return null
    }

    private fun finishSettingUpConversation() {
        conversationSetupMessages.clear()
        settingUpConversation = false
    }

    fun createConversation(id: Int): Crypto.Coders? { // blocks the caller thread
        assert(running && connected && authenticated && !destroyed && id >= 0 && !settingUpConversation && !exchangingFile)
        settingUpConversation = true
        conversationSetupMessages.clear()

        if (!send(FLAG_EXCHANGE_KEYS, ByteArray(INVITE_ASK), id)) {
            finishSettingUpConversation()
            return null
        }

        var message: NetMessage?
        if (
            run { message = conversationSetupMessages.waitAndPop(); message } == null
            || message!!.flag != FLAG_EXCHANGE_KEYS
            || message!!.size != Crypto.KEY_SIZE
            || message!!.body == null
        ) {
            finishSettingUpConversation()
            return null
        }

        val akaServerPublicKey = ByteArray(Crypto.KEY_SIZE)
        System.arraycopy(message?.body!!, 0, akaServerPublicKey, 0, Crypto.KEY_SIZE)

        val keys = crypto.exchangeKeys(akaServerPublicKey)
        if (keys == null) {
            finishSettingUpConversation()
            return null
        }

        if (!send(FLAG_EXCHANGE_KEYS_DONE, crypto.clientPublicKey(keys), id)) {
            finishSettingUpConversation()
            return null
        }

        if (
            run { message = conversationSetupMessages.waitAndPop(); message } == null
            || message!!.flag != FLAG_EXCHANGE_HEADERS
            || message!!.size != Crypto.HEADER_SIZE
            || message!!.body == null
        ) {
            finishSettingUpConversation()
            return null
        }

        val akaServerStreamHeader = ByteArray(Crypto.HEADER_SIZE)
        System.arraycopy(message?.body!!, 0, akaServerStreamHeader, 0, Crypto.HEADER_SIZE)

        val coders = crypto.makeCoders()
        val akaClientStreamHeader = crypto.initializeCoders(coders, keys, akaServerStreamHeader)

        if (akaClientStreamHeader == null) {
            finishSettingUpConversation()
            return null
        }

        val result = send(FLAG_EXCHANGE_HEADERS_DONE, akaClientStreamHeader, id)
        finishSettingUpConversation()
        return if (result) coders else null
    }

    private fun inviteProcessingTimeoutExceeded() = System.currentTimeMillis() - inviteProcessingStartMillis > TIMEOUT

    fun replyToConversationSetUpInvite(accept: Boolean, fromId: Int): Crypto.Coders? { // blocks the caller thread
        assert(running && connected && authenticated && !destroyed && fromId >= 0 && settingUpConversation && !exchangingFile)
        conversationSetupMessages.clear()

        if (inviteProcessingTimeoutExceeded()) {
            finishSettingUpConversation()
            return null
        }

        if (!accept) {
            finishSettingUpConversation()
            send(FLAG_EXCHANGE_KEYS, ByteArray(INVITE_DENY), fromId)
            return null
        }

        val keys = crypto.makeKeys()
        val akaServerPublicKey = crypto.generateKeyPairAsServer(keys)

        if (!send(FLAG_EXCHANGE_KEYS, akaServerPublicKey, fromId)) {
            finishSettingUpConversation()
            return null
        }

        var message: NetMessage?
        if (
            run { message = conversationSetupMessages.waitAndPop(); message } == null
            || message!!.flag != FLAG_EXCHANGE_KEYS_DONE
            || message!!.size != Crypto.KEY_SIZE
            || message!!.body == null
        ) {
            finishSettingUpConversation()
            return null
        }

        val akaClientPublicKey = ByteArray(Crypto.KEY_SIZE)
        System.arraycopy(message?.body!!, 0, akaClientPublicKey, 0, Crypto.KEY_SIZE)

        if (!crypto.exchangeKeysAsServer(keys, akaClientPublicKey)) {
            finishSettingUpConversation()
            return null
        }

        val coders = crypto.makeCoders()
        val akaServerStreamHeader = crypto.createEncoderAsServer(keys, coders)

        if (akaServerStreamHeader == null) {
            finishSettingUpConversation()
            return null
        }

        if (!send(FLAG_EXCHANGE_HEADERS, akaServerStreamHeader, fromId)) {
            finishSettingUpConversation()
            return null
        }

        if (
            run { message = conversationSetupMessages.waitAndPop(); message } == null
            || message!!.flag != FLAG_EXCHANGE_HEADERS_DONE
            || message!!.size != Crypto.HEADER_SIZE
            || message!!.body == null
        ) {
            finishSettingUpConversation()
            return null
        }

        val akaClientStreamHeader = ByteArray(Crypto.HEADER_SIZE)
        System.arraycopy(message?.body!!, 0, akaClientStreamHeader, 0, Crypto.HEADER_SIZE)
        finishSettingUpConversation()

        return if (!crypto.createDecoderAsServer(keys, coders, akaClientStreamHeader)) null else coders
    }

    fun onDestroy() {
        log("close")
        assert(!running)
        synchronized(lock) {
            socket?.close()
            socket = null
        }
        destroyed = true
        initiator.onNetDestroy()
    }

    private companion object {
        private const val TIMEOUT = 15000

        private const val FROM_ANONYMOUS = 0xffffffff.toInt()
        private const val FROM_SERVER = 0x7fffffff

        private const val TO_SERVER = 0x7ffffffe

        private const val FLAG_PROCEED = 0x00000000
        private const val FLAG_BROADCAST = 0x10000000
        private const val FLAG_LOG_IN = 0x00000004
        private const val FLAG_LOGGED_IN = 0x00000005
        private const val FLAG_REGISTER = 0x00000006
        private const val FLAG_REGISTERED = 0x00000007
        private const val FLAG_ERROR = 0x00000009
        private const val FLAG_FETCH_USERS = 0x0000000c
        private const val FLAG_FETCH_MESSAGES = 0x0000000d
        private const val FLAG_EXCHANGE_KEYS = 0x000000a0
        private const val FLAG_EXCHANGE_KEYS_DONE = 0x000000b0
        private const val FLAG_EXCHANGE_HEADERS = 0x000000c0
        private const val FLAG_EXCHANGE_HEADERS_DONE = 0x000000d0
        private const val FLAG_FILE_ASK = 0x000000e0
        private const val FLAG_FILE = 0x000000f0
        private const val FLAG_SHUTDOWN = 0x7fffffff

        private const val INVITE_ASK = 1
        private const val INVITE_DENY = 2
    }
}
