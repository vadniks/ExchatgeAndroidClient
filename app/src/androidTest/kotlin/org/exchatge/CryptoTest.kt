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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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
}
