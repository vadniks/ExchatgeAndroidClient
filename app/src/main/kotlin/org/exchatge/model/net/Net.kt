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
import kotlinx.coroutines.delay
import org.exchatge.model.Crypto
import org.exchatge.model.Kernel
import org.exchatge.model.assert
import org.exchatge.model.assertNotMainThread
import java.net.Socket

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

    init {
        assert(!initialized)
        initialized = true
    }

    fun startService() {
        if (!NetService.running)
            kernel.context.startService(Intent(kernel.context, NetService::class.java))!! // TODO: start the service only if the user has logged in
    }

    fun onCreate() {
        socket = try { Socket(SERVER_ADDRESS, 8080) }
        catch (_: Exception) { null } // unable to connect

        if (socket == null) return // unable to connect
        initiateSecuredConnection()
    }

    private fun initiateSecuredConnection(): Boolean {
        coders = crypto.makeCoders()

        val signedServerPublicKey = ByteArray(Crypto.SIGNATURE_SIZE + Crypto.KEY_SIZE)
        if (!waitForReceiveWithTimeout()) return false
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

    private fun read(buffer: ByteArray) =
        try { socket!!.getInputStream().read(buffer) == buffer.size }
        catch (_: Exception) { false }

    private fun write(buffer: ByteArray) =
        try { socket!!.getOutputStream().write(buffer); true }
        catch (_: Exception) { false }

    private fun hasSomethingToRead() = socket!!.getInputStream().available() > 0

    private fun waitForReceiveWithTimeout(): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < TIMEOUT)
            if (hasSomethingToRead())
                return true
        return false
    }

    private fun receive(): ByteArray? {
        val sizeBytes = ByteArray(4)
        if (read(sizeBytes)) return null // disconnected
        val size = sizeBytes.int
        assert(size in 0..encryptedMessageMaxSize)

        val buffer = ByteArray(size)
        if (read(buffer)) return null

        // TODO: decrypt
        return null
    }

    suspend fun listen() { // TODO: add an 'exit' button to UI which will close the activity as well as the service to completely shutdown the whole app
        while (NetService.running) {
            // TODO: check if db is opened
            assertNotMainThread()
            delay(500)
        }
    }

    fun onDestroy() {
        socket?.close()
        kernel.onNetDestroy()
    }

    private companion object {
        @JvmStatic
        private var initialized = false

        private const val TIMEOUT = 5000
    }
}
