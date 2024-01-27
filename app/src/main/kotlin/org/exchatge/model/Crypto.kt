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

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import java.nio.charset.StandardCharsets

class Crypto {
    private val lazySodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.US_ASCII) // using ascii as he desktop client's ui cannot display anything other than ascii, but it can process non ascii though

    init {
        assert(!initialized)
        initialized = true
    }



    data class Keys(
        val serverPublicKey: ByteArray,
        val clientPublicKey: ByteArray,
        val clientSecretKey: ByteArray,
        val clientKey: ByteArray,
        val serverKey: ByteArray
    ) {

        init {
            assert(serverPublicKey.size == KEY_SIZE)
            assert(clientPublicKey.size == KEY_SIZE)
            assert(clientSecretKey.size == KEY_SIZE)
            assert(clientKey.size == KEY_SIZE)
            assert(serverKey.size == KEY_SIZE)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Keys

            if (!serverPublicKey.contentEquals(other.serverPublicKey)) return false
            if (!clientPublicKey.contentEquals(other.clientPublicKey)) return false
            if (!clientSecretKey.contentEquals(other.clientSecretKey)) return false
            if (!clientKey.contentEquals(other.clientKey)) return false
            if (!serverKey.contentEquals(other.serverKey)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = serverPublicKey.contentHashCode()
            result = 31 * result + clientPublicKey.contentHashCode()
            result = 31 * result + clientSecretKey.contentHashCode()
            result = 31 * result + clientKey.contentHashCode()
            result = 31 * result + serverKey.contentHashCode()
            return result
        }
    }

    private companion object {
        @JvmStatic
        private var initialized = false

        const val KEY_SIZE = 32
    }
}
