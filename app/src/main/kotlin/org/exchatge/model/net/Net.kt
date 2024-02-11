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
import org.exchatge.model.Kernel
import org.exchatge.model.assert
import org.exchatge.model.blockingWithLock
import org.exchatge.model.bypassMainThreadRestriction
import org.exchatge.model.log
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

private const val SERVER_ADDRESS = "192.168.1.57" // TODO: debug only

private val serverSignPublicKey = byteArrayOf( // TODO: debug only
    255.toByte(), 23, 21, 243.toByte(), 148.toByte(), 177.toByte(), 186.toByte(), 0,
    73, 34, 173.toByte(), 130.toByte(), 234.toByte(), 251.toByte(), 83, 130.toByte(),
    138.toByte(), 54, 215.toByte(), 5, 170.toByte(), 139.toByte(), 175.toByte(), 148.toByte(),
    71, 215.toByte(), 74, 172.toByte(), 27, 225.toByte(), 26, 249.toByte()
)

class Net(private val kernel: Kernel) {
    val running get() = NetService.running
    private var socket: Socket? = null
    private val crypto get() = kernel.crypto
    private val encryptedMessageMaxSize = crypto.encryptedSize(MAX_MESSAGE_SIZE)
    private var coders: Crypto.Coders? = null
    private val codersMutex = Mutex()
    private val authenticated = AtomicBoolean(false)
    private val userId = AtomicInteger(FROM_ANONYMOUS)
    private val tokenAnonymous = ByteArray(TOKEN_SIZE)
    private val tokenUnsignedValue = ByteArray(TOKEN_UNSIGNED_VALUE_SIZE) { 255.toByte() }
    private val token = AtomicReference(tokenAnonymous.copyOf())

    init {
        assert(!initialized)
        initialized = true
    }

    fun startService() {
        if (!NetService.running)
            kernel.context.startService(Intent(kernel.context, NetService::class.java))!! // TODO: start the service only if the user has logged in
    }

    fun onCreate() = bypassMainThreadRestriction {
        socket = try { Socket().apply { connect(InetSocketAddress(SERVER_ADDRESS, 8080), 1000 * 60 * 60) } }
        catch (_: Exception) { null } // unable to connect
        socket!!.soTimeout = 500 // delay between socket read tries (while (open) { delay(500); tryRead() })

        log("connected = " + socket?.isConnected)

        if (socket == null) return@bypassMainThreadRestriction // unable to connect
        val ready =
            try { initiateSecuredConnection() }
            catch (_: SocketTimeoutException) { false }
        log("ready = $ready") // if (!ready) // error while connecting
    }

    @Throws(SocketTimeoutException::class)
    private fun initiateSecuredConnection(): Boolean {
        coders = crypto.makeCoders()

        val signedServerPublicKey = ByteArray(Crypto.SIGNATURE_SIZE + Crypto.KEY_SIZE)
        if (!waitForReceiveWithTimeout()) return false // disconnected
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
        if (!waitForReceiveWithTimeout()) return false
        if (!read(signedServerCoderHeader)) return false

        val serverCoderHeader = signedServerCoderHeader.sliceArray(Crypto.SIGNATURE_SIZE until signedServerCoderHeader.size)

        assert(crypto.checkServerSignedBytes(
            signedServerCoderHeader.sliceArray(0 until Crypto.SIGNATURE_SIZE),
            serverCoderHeader,
            serverSignPublicKey
        ))

        val clientCoderHeader = crypto.initializeCoders(keys, serverCoderHeader) ?: return false
        return write(clientCoderHeader)
    }

    @Throws(SocketTimeoutException::class)
    private fun read(buffer: ByteArray) =
        try { socket!!.getInputStream().read(buffer) == buffer.size } // false if disconnected
        catch (e: SocketTimeoutException) { throw SocketTimeoutException() } // timeout
        catch (_: Exception) { false } // error - disconnect

    private fun write(buffer: ByteArray) =
        try { socket!!.getOutputStream().write(buffer).also { log("w " + buffer.size) }; true }
        catch (_: Exception) { false }

    @Deprecated("")
    private fun hasSomethingToRead() =
        try { socket!!.getInputStream().available() > 0 }
        catch (e: Exception) { log("e $e"); false }

    @Deprecated("no need")
    private fun waitForReceiveWithTimeout(): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < TIMEOUT)
            if (hasSomethingToRead())
                return true
        return false
    }

    @Throws(SocketTimeoutException::class)
    private fun receive(): NetMessage? {
        val sizeBytes = ByteArray(4)
        if (!read(sizeBytes)) return null // disconnected
        val size = sizeBytes.int
        assert(size in 0..encryptedMessageMaxSize)

        val buffer = ByteArray(size)
        if (!read(buffer)) return null

        var decrypted: ByteArray? = null
        codersMutex.blockingWithLock { decrypted = crypto.decrypt(coders!!, buffer) }
        assert(decrypted != null)

        return NetMessage.unpack(decrypted!!)
    }

    fun listen() { // TODO: add an 'exit' button to UI which will close the activity as well as the service to completely shutdown the whole app
        while (NetService.running && socket != null && !socket!!.isClosed && socket!!.isConnected) {
            // TODO: check if db is opened
            log("listen")
            try { if (tryReadMessage()) break }
            catch (_: SocketTimeoutException) { continue }
        }
        log("disconnected") // TODO: handle disconnection
    }

    @Throws(SocketTimeoutException::class)
    private fun tryReadMessage(): Boolean {
        processMessage(receive() ?: return true)
        return false
    }

    private fun processMessage(message: NetMessage) {
        log("message: $message")

        if (message.from == FROM_SERVER) {
            processMessageFromServer(message)
            return
        }

        assert(authenticated.get())

        when (message.flag) {
            FLAG_PROCEED -> {
                assert(message.body != null)
                // TODO: handle usual message
            }
            else -> Unit
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
                assert(message.body != null)
                authenticated.set(true)
                userId.set(message.to)
                token.set(message.body!!.sliceArray(0 until TOKEN_SIZE))

                // TODO: onLogInResult(true)
            }
            FLAG_REGISTERED -> Unit
            FLAG_FETCH_USERS -> {

            }
            FLAG_ERROR -> processErrors(message)
        }
    }

    private fun processErrors(message: NetMessage) {

    }

    fun onDestroy() {
        log("close")
        socket?.close()
        kernel.onNetDestroy()
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
