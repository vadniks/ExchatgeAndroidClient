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
import kotlinx.coroutines.sync.Mutex
import org.exchatge.model.Crypto
import org.exchatge.model.Reference
import org.exchatge.model.Ternary
import org.exchatge.model.assert
import org.exchatge.model.withLockBlocking
import org.exchatge.model.log
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

private const val SERVER_ADDRESS = "192.168.1.57" // TODO: debug only

private val serverSignPublicKey = byteArrayOf( // TODO: debug only
    255.toByte(), 23, 21, 243.toByte(), 148.toByte(), 177.toByte(), 186.toByte(), 0,
    73, 34, 173.toByte(), 130.toByte(), 234.toByte(), 251.toByte(), 83, 130.toByte(),
    138.toByte(), 54, 215.toByte(), 5, 170.toByte(), 139.toByte(), 175.toByte(), 148.toByte(),
    71, 215.toByte(), 74, 172.toByte(), 27, 225.toByte(), 26, 249.toByte()
)

private const val USERNAME = "admin" // TODO: debug only
private const val PASSWORD = "admin"

class Net(private val initiator: NetInitiator) {
    val running get() = NetService.running
    private lateinit var destroy: () -> Unit // goes to onDestroy
    private var socket: Socket? = null
    private val crypto get() = initiator.crypto
    private val encryptedMessageMaxSize = crypto.encryptedSize(MAX_MESSAGE_SIZE)
    private var coders: Crypto.Coders? = null
    private val mutex = Mutex()
    @Volatile private var authenticated = false
    @Volatile var userId = FROM_ANONYMOUS; private set
    private val tokenAnonymous = ByteArray(TOKEN_SIZE)
    private val tokenUnsignedValue = ByteArray(TOKEN_UNSIGNED_VALUE_SIZE) { 255.toByte() }
    @Volatile private var token = tokenAnonymous.copyOf()
    @Volatile private var fetchingUsers = false
    @Volatile private var fetchingMessages = false
    private val userInfos = ArrayList<UserInfo>()

    init {
        assert(!initialized)
        initialized = true

        if (!NetService.running)
            initiator.context.startService(Intent(initiator.context, NetService::class.java))!! // TODO: start the service only if the user has logged in
    }

    fun onCreate(destroy: () -> Unit) { this.destroy = destroy }

    fun onPostCreate() {
        mutex.withLockBlocking {
            socket =
                try { Socket().apply { connect(InetSocketAddress(SERVER_ADDRESS, 8080), /*TODO: extract constant*/1000 * 60 * 60) } }
                catch (_: Exception) { null } // unable to connect
        }

        log("connected = " + socket?.isConnected)
        if (socket == null) {
            destroy()
            return
        }
        mutex.withLockBlocking { socket!!.soTimeout = 500 } // delay between socket read tries (while (open) { delay(500); tryRead() })

        val ready = initiateSecuredConnection()
        log("ready = $ready") // if (!ready) // error while connecting

        if (!ready) {
            destroy()
            return
        }

        logIn() // TODO: debug only
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
            if (mutex.withLockBlocking { socket!!.getInputStream().read(buffer) } == buffer.size)
                Ternary.POSITIVE
            else
                Ternary.NEGATIVE // disconnected
        }
        catch (e: SocketTimeoutException) { Ternary.NEUTRAL } // timeout
        catch (_: Exception) { Ternary.NEGATIVE } // error - disconnect

    private fun write(buffer: ByteArray) =
        try { mutex.withLockBlocking { socket!!.getOutputStream().write(buffer).also { log("write " + buffer.size) } }; true }
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

        val decrypted = mutex.withLockBlocking { crypto.decrypt(coders!!, buffer) }
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

        val encrypted = mutex.withLockBlocking { crypto.encrypt(coders!!, packed) }
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
        assert(running && body.isNotEmpty() && to >= 0)
        send(FLAG_PROCEED, body, to)
    }

    // TODO: add an 'exit' button to UI which will close the activity as well as the service to completely shutdown the whole app

    fun listen() {
        while (NetService.running && mutex.withLockBlocking { socket != null && !socket!!.isClosed && socket!!.isConnected }) {
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
        log("message: $message")

        if (message.from == FROM_SERVER) {
            processMessageFromServer(message)
            return
        }

        assert(authenticated)

        when (message.flag) {
            FLAG_EXCHANGE_KEYS -> {}
            FLAG_EXCHANGE_KEYS_DONE, FLAG_EXCHANGE_HEADERS, FLAG_EXCHANGE_HEADERS_DONE -> {}
            FLAG_FILE_ASK -> {}
            FLAG_FILE -> {}
            FLAG_PROCEED -> {
                assert(message.body != null)
                // TODO: handle usual message
            }
            FLAG_FETCH_MESSAGES -> onNextMessageFetched(message)
        }
    }

    private fun processMessageFromServer(message: NetMessage) {
        assert(crypto.checkServerSignedBytes(
            message.token.sliceArray(0 until Crypto.SIGNATURE_SIZE),
            tokenUnsignedValue,
            serverSignPublicKey
        ))

        when (message.flag) {
            FLAG_LOGGED_IN -> {
                log("log in succeeded")

                assert(message.body != null)
                authenticated = true
                userId = message.to
                token = message.body!!.sliceArray(0 until TOKEN_SIZE)

                // TODO: onLogInResult(true)
                fetchUsers() // TODO: debug only
            }
            FLAG_REGISTERED -> log("register succeeded")
            FLAG_FETCH_USERS -> onNextUserInfosBundleFetched(message)
            FLAG_ERROR -> processErrors(message)
            FLAG_FETCH_MESSAGES -> onEmptyMessagesFetchReplyReceived(message)
            FLAG_BROADCAST -> log("broadcast received ${message.body!!}")
        }
    }

    private fun processErrors(message: NetMessage) {
        assert(message.size == 4 && message.body != null)
        when (val flag = message.body!!.sliceArray(0 until 4).int) {
            FLAG_LOG_IN -> log("log in failed") // TODO: handle
            FLAG_REGISTER -> log("register failed")
            FLAG_FETCH_MESSAGES -> log("fetch messages failed")
            else -> log("error $flag received")
        }
    }

    private fun onNextUserInfosBundleFetched(message: NetMessage) {
        assert(running && fetchingUsers)
        assert(message.body != null && message.size in 1..MAX_MESSAGE_BODY_SIZE)
        if (message.index == 0) userInfos.clear()

        for (i in 0 until message.size step USER_INFO_SIZE)
            userInfos.add(UserInfo.unpack(message.body!!.sliceArray(i until i + USER_INFO_SIZE)))

        if (message.index < message.count - 1) return

        // TODO: onUsersFetched
        log("users fetched $userInfos")
        userInfos.clear()
        fetchingUsers = false
    }

    private fun onNextMessageFetched(message: NetMessage) {
        assert(running && fetchingMessages && message.body != null)
        val last = message.index == message.count - 1

        log("next message fetched $message") // TODO: handle
        if (last) fetchingMessages = false
    }

    private fun onEmptyMessagesFetchReplyReceived(message: NetMessage) {
        assert(running && message.body != null && message.count == 1)
        log("empty messages fetch reply $message " + message.body!!.sliceArray(1 + 8 until 1 + 8 + 4).int) // TODO: handle
        fetchingMessages = false
    }

    private fun makeCredentials(username: String, password: String): ByteArray {
        assert(username.length in 1..USERNAME_SIZE && password.length in 1..UNHASHED_PASSWORD_SIZE)
        val credentials = ByteArray(USERNAME_SIZE + UNHASHED_PASSWORD_SIZE)

        System.arraycopy(username.toByteArray(), 0, credentials, 0, username.length)
        System.arraycopy(password.toByteArray(), 0, credentials, USERNAME_SIZE, password.length)
        return credentials
    }

    fun logIn(username: String = USERNAME, password: String = PASSWORD) {
        assert(running)
        send(FLAG_LOG_IN, makeCredentials(username, password), TO_SERVER)
    }

    fun register(username: String = USERNAME, password: String = PASSWORD) {
        assert(running)
        send(FLAG_REGISTER, makeCredentials(username, password), TO_SERVER)
    }

    fun shutdownServer() {
        assert(running)
        send(FLAG_SHUTDOWN, null, TO_SERVER)
    }

    fun fetchUsers() {
        assert(running && !fetchingUsers && !fetchingMessages)
        fetchingUsers = true
        send(FLAG_FETCH_USERS, null, TO_SERVER)
    }

    fun sendBroadcast(body: ByteArray) {
        assert(running && body.isNotEmpty())
        send(FLAG_BROADCAST, body, TO_SERVER)
    }

    fun fetchMessages(id: Int, afterTimestamp: Long) {
        assert(running && id >= 0 && afterTimestamp >= 0 && !fetchingUsers && !fetchingMessages)
        fetchingMessages = true

        val body = ByteArray(1 + 8 + 4)
        body[0] = 1
        System.arraycopy(afterTimestamp.bytes, 0, body, 1, 8)
        System.arraycopy(id.bytes, 0, body, 1 + 8, 4)

        send(FLAG_FETCH_MESSAGES, body, TO_SERVER)
    }

    fun onDestroy() {
        log("close")
        mutex.withLockBlocking { socket?.close() }
        initiator.onNetDestroy()
    }

    private companion object {
        @JvmStatic
        private var initialized = false

        private const val TIMEOUT = 5000

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
    }
}
