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

package org.exchatge

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.exchatge.model.App
import org.exchatge.model.Crypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class CryptoTest {
    private val crypto = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App).kernel.crypto

    @Test
    fun keyExchange() {
        val serverKeys = crypto.makeKeys()
        val serverPublicKey = crypto.generateKeyPairAsServer(serverKeys)

        val clientKeys = crypto.exchangeKeys(serverPublicKey)
        assertNotNull(clientKeys != null)

        assertTrue(crypto.exchangeKeysAsServer(serverKeys, crypto.clientPublicKey(clientKeys!!)))

        assertTrue(crypto.clientKey(serverKeys) contentEquals crypto.clientKey(clientKeys))
        assertTrue(crypto.serverKey(serverKeys) contentEquals crypto.serverKey(clientKeys))
    }

    @Test
    fun signature() {
        val keys = crypto.makeSignKeys()

        val message = ByteArray(10)
        crypto.randomizeBuffer(message)

        val signed = crypto.sign(message, keys.second)
        assertTrue(crypto.checkServerSignedBytes(
            signed.sliceArray(0 until Crypto.SIGNATURE_SIZE),
            signed.sliceArray(Crypto.SIGNATURE_SIZE until signed.size),
            keys.first
        ))
    }

    @Test
    fun streamEncryption() {
        val key = ByteArray(Crypto.KEY_SIZE)
        crypto.randomizeBuffer(key)

        val keys = crypto.makeKeys()
        System.arraycopy(key, 0, crypto.clientKey(keys), 0, Crypto.KEY_SIZE)
        System.arraycopy(key, 0, crypto.serverKey(keys), 0, Crypto.KEY_SIZE)

        val coders = crypto.makeCoders()
        val streamHeader = crypto.createEncoderAsServer(keys, coders)
        assertNotNull(streamHeader)
        assertTrue(crypto.createDecoderAsServer(keys, coders, streamHeader!!))

        val original = ByteArray(10)
        crypto.randomizeBuffer(original)

        val encrypted = crypto.encrypt(coders, original)
        assertNotNull(encrypted)
        assertTrue(encrypted!!.size == crypto.encryptedSize(original.size))

        val decrypted = crypto.decrypt(coders, encrypted)
        assertNotNull(decrypted)

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun singleEncryption() {
        val password = ByteArray(10)
        for (i in password.indices)
            password[i] = Random.nextInt('a'.code, 'z'.code).toByte()

        val key = crypto.makeKey(password)
        assertTrue(key.size == Crypto.KEY_SIZE)

        val original = ByteArray(10)
        crypto.randomizeBuffer(original)

        val encrypted = crypto.encryptSingle(key, original)
        assertNotNull(encrypted)
        assertTrue(encrypted!!.size == crypto.singleEncryptedSize(original.size))

        val decrypted = crypto.decryptSingle(key, encrypted)
        assertNotNull(decrypted)

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun multipartHash() {
        val size = 1 shl 10
        val slice  = size / 8

        val bytes = ByteArray(size)
        crypto.randomizeBuffer(bytes)

        val state = crypto.hashMultipart(null, null)
        assertNotNull(state)

        for (i in 0 until size step slice)
            assertNull(crypto.hashMultipart(state, bytes.sliceArray(i until i + slice)))

        val hash = crypto.hashMultipart(state, null)
        assertNotNull(hash)
    }

    @Test
    fun padding() {
//        for (i in 0..1) {
            val first = true

            val size = if (first) 10 else Crypto.PADDING_BLOCK_SIZE
            val original = ByteArray(size)
            for (j in 0 until size) original[j] = 0x80.toByte()

            val padded = crypto.addPadding(original)
            assertTrue(padded.size % Crypto.PADDING_BLOCK_SIZE == 0 && padded.size > size)
//
//            val unpadded = crypto.removePadding(padded)
//            assertNotNull(unpadded)
//            assertTrue(unpadded!!.size == size)
//
//            assertArrayEquals(original, unpadded)
//        }
    }
}
