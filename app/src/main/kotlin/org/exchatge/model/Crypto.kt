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
import com.goterl.lazysodium.interfaces.SecretStream
import java.nio.charset.StandardCharsets

class Crypto {
    private val lazySodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.US_ASCII) // using ascii as he desktop client's ui cannot display anything other than ascii, but it can process non ascii though

    init {
        assert(!initialized)
        initialized = true
    }

    fun exchangeKeys(serverPublicKey: ByteArray): Keys? {
        assert(serverPublicKey.size == KEY_SIZE)

        val keys = Keys(serverPublicKey.copyOf())
        assert(lazySodium.cryptoKxKeypair(keys.clientPublicKey, keys.clientSecretKey))

        return if (lazySodium.cryptoKxClientSessionKeys(
            keys.clientKey,
            keys.serverKey,
            keys.clientPublicKey,
            keys.clientSecretKey,
            keys.serverPublicKey
        )) keys else null
    }

    fun initializeCoders(keys: Keys, serverStreamHeader: ByteArray): ByteArray? {
        assert(serverStreamHeader.size == HEADER_SIZE)
        assert(keys.valid)

        val coders = Coders()
        if (!lazySodium.cryptoSecretStreamInitPull(coders.decryptionState, serverStreamHeader, keys.serverKey))
            return null

        val clientStreamHeader = ByteArray(KEY_SIZE)
        if (!lazySodium.cryptoSecretStreamInitPush(coders.encryptionState, clientStreamHeader, keys.clientKey))
            return null

        return clientStreamHeader
    }

    fun checkServerSignedBytes(signature: ByteArray, unsigned: ByteArray, serverSignPublicKey: ByteArray): Boolean {
        assert(signature.size == SIGNATURE_SIZE && unsigned.isNotEmpty() && serverSignPublicKey.size == KEY_SIZE)

        val combined = ByteArray(SIGNATURE_SIZE + unsigned.size)
        System.arraycopy(signature, 0, combined, 0, SIGNATURE_SIZE)
        System.arraycopy(unsigned, 0, combined, SIGNATURE_SIZE, unsigned.size)

        val generatedUnsigned = ByteArray(unsigned.size)
        if (!lazySodium.cryptoSignOpen(generatedUnsigned, combined, combined.size.toLong(), serverSignPublicKey))
            return false

        assert(unsigned.contentEquals(generatedUnsigned))
        return true
    }

    fun makeKey(password: ByteArray): ByteArray {
        assert(password.isNotEmpty())

        val hash = ByteArray(KEY_SIZE)
        assert(lazySodium.cryptoGenericHash(hash, hash.size, password, password.size.toLong()))
        return hash
    }

    fun recreateCoders(serializedCoders: ByteArray): Coders {
        assert(serializedCoders.size == CODERS_SIZE)

        fun copyBytes(state: SecretStream.State, sourceOffset: Int) {
            System.arraycopy(serializedCoders, sourceOffset + 0, state.k, 0, SECRET_STREAM_K_SIZE)
            System.arraycopy(serializedCoders, sourceOffset + SECRET_STREAM_K_SIZE, state.nonce, 0, SECRET_STREAM_NONCE_SIZE)
            System.arraycopy(serializedCoders, sourceOffset + SECRET_STREAM_K_SIZE + SECRET_STREAM_NONCE_SIZE, state._pad, 0, SECRET_STREAM_PAD_SIZE)
        }

        val decryptionState = SecretStream.State.ByReference()
        copyBytes(decryptionState, 0)

        val encryptionState = SecretStream.State.ByReference()
        copyBytes(encryptionState, CODERS_SIZE / 2)

        return Coders(decryptionState, encryptionState)
    }

    fun exportCoders(coders: Coders): ByteArray {
        val bytes = ByteArray(CODERS_SIZE)

        fun copyBytes(state: SecretStream.State, destinationOffset: Int) {
            System.arraycopy(state.k, 0, bytes, destinationOffset + 0, SECRET_STREAM_K_SIZE)
            System.arraycopy(state.nonce, 0, bytes, destinationOffset + SECRET_STREAM_K_SIZE, SECRET_STREAM_NONCE_SIZE)
            System.arraycopy(state._pad, 0, bytes, destinationOffset + SECRET_STREAM_K_SIZE + SECRET_STREAM_NONCE_SIZE, SECRET_STREAM_PAD_SIZE)
        }

        copyBytes(coders.decryptionState, 0)
        copyBytes(coders.encryptionState, CODERS_SIZE / 2)

        return bytes
    }

    private val Keys.clientPublicKeyAsServer get() = serverPublicKey
    private val Keys.serverPublicKeyAsServer get() = clientPublicKey
    private val Keys.serverSecretKeyAsServer get() = clientSecretKey

    fun generateKeyPairAsServer(keys: Keys) = assert(lazySodium.cryptoKxKeypair(keys.serverPublicKeyAsServer, keys.serverSecretKeyAsServer))

    fun exchangeKeysAsServer(keys: Keys, clientPublicKey: ByteArray): Boolean {
        System.arraycopy(clientPublicKey, 0, keys.clientPublicKeyAsServer, 0, KEY_SIZE)

        return lazySodium.cryptoKxServerSessionKeys(
            keys.serverKey,
            keys.clientKey,
            keys.serverPublicKeyAsServer,
            keys.serverSecretKeyAsServer,
            keys.clientPublicKeyAsServer
        )
    }

    fun createEncoderAsServer(keys: Keys, coders: Coders): ByteArray? {
        val serverStreamHeader = ByteArray(HEADER_SIZE)

        return if (!lazySodium.cryptoSecretStreamInitPush(
            coders.encryptionState,
            serverStreamHeader,
            keys.serverKey
        )) null else serverStreamHeader
    }

    fun createDecoderAsServer(keys: Keys, coders: Coders, clientStreamHeader: ByteArray): Boolean {
        assert(clientStreamHeader.size == HEADER_SIZE)
        return lazySodium.cryptoSecretStreamInitPull(coders.decryptionState, clientStreamHeader, keys.clientKey)
    }

    fun encryptedSize(unencryptedSize: Int) = unencryptedSize + ADDITIONAL_BYTES_SIZE

    fun encrypt(coders: Coders, bytes: ByteArray): ByteArray? {
        assert(bytes.isNotEmpty())

        val encryptedSize = encryptedSize(bytes.size)
        val generatedEncryptedSizeAddress = LongArray(1) // 'cause long arr[1] is the same as long* to the arr's first element
        val encrypted = ByteArray(encryptedSize)

        if (!lazySodium.cryptoSecretStreamPush(
            coders.encryptionState,
            encrypted,
            generatedEncryptedSizeAddress, // long* a = (long[1]) {0}; *a = 1;
            bytes,
            bytes.size.toLong(),
            TAG_INTERMEDIATE
        )) return null

        assert(generatedEncryptedSizeAddress[0] == encryptedSize.toLong())
        return encrypted
    }

    fun decrypt(coders: Coders, bytes: ByteArray): ByteArray? {
        assert(bytes.size > ADDITIONAL_BYTES_SIZE)

        val decryptedSize = bytes.size - ADDITIONAL_BYTES_SIZE
        val generatedDecryptedSizeAddress = LongArray(1)
        val decrypted = ByteArray(decryptedSize)
        val tagAddress = ByteArray(1)

        if (!lazySodium.cryptoSecretStreamPull(
            coders.decryptionState,
            decrypted,
            generatedDecryptedSizeAddress, // long* a = (long[1]) {0}; *a = 1;
            tagAddress,
            bytes,
            bytes.size.toLong(),
            null,
            0
        )) return null

        if (tagAddress[0] != TAG_INTERMEDIATE) return null
        assert(generatedDecryptedSizeAddress[0] == decryptedSize.toLong())
        return decrypted
    }

    fun randomizeBuffer(buffer: ByteArray) {
        assert(buffer.isNotEmpty())
        System.arraycopy(lazySodium.randomBytesBuf(buffer.size), 0, buffer, 0, buffer.size)
    }

    fun singleEncryptedSize(unencryptedSize: Int) = MAC_SIZE + unencryptedSize + NONCE_SIZE

    fun encryptSingle(key: ByteArray, bytes: ByteArray): ByteArray? {
        assert(key.size == KEY_SIZE && bytes.isNotEmpty())

        val encryptedSize = singleEncryptedSize(bytes.size)
        val encrypted = ByteArray(encryptedSize)

        val nonceStart = encryptedSize - NONCE_SIZE
        System.arraycopy(lazySodium.randomBytesBuf(NONCE_SIZE), 0, encrypted, nonceStart, NONCE_SIZE)

        if (!lazySodium.cryptoSecretBoxEasy(
            encrypted,
            bytes,
            bytes.size.toLong(),
            encrypted.sliceArray(nonceStart..encryptedSize),
            key
        )) return null

        return encrypted
    }

    fun decryptSingle(key: ByteArray, bytes: ByteArray): ByteArray? {
        assert(key.size == KEY_SIZE && bytes.size > singleEncryptedSize(0))

        val decryptedSize = bytes.size - MAC_SIZE - NONCE_SIZE
        val decrypted = ByteArray(decryptedSize)
        val encryptedAndTagSize = bytes.size - NONCE_SIZE

        if (!lazySodium.cryptoSecretBoxOpenEasy(
            decrypted,
            bytes,
            encryptedAndTagSize.toLong(),
            bytes.sliceArray(encryptedAndTagSize..(bytes.size)),
            key
        )) return null

        return decrypted
    }

    data class Coders(
        val decryptionState: SecretStream.State = SecretStream.State.ByReference(),
        val encryptionState: SecretStream.State = SecretStream.State.ByReference()
    )

    data class Keys(
        val serverPublicKey: ByteArray = ByteArray(KEY_SIZE),
        val clientPublicKey: ByteArray = ByteArray(KEY_SIZE),
        val clientSecretKey: ByteArray = ByteArray(KEY_SIZE),
        val clientKey: ByteArray = ByteArray(KEY_SIZE),
        val serverKey: ByteArray = ByteArray(KEY_SIZE)
    ) {
        val valid get() =
            serverPublicKey.size == KEY_SIZE &&
            clientPublicKey.size == KEY_SIZE &&
            clientSecretKey.size == KEY_SIZE &&
            clientKey.size == KEY_SIZE &&
            serverKey.size == KEY_SIZE

        init {
            assert(valid)
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
        const val HEADER_SIZE = 24
        const val SIGNATURE_SIZE = 64
        const val CODERS_SIZE = 104
        const val HASH_SIZE = 32
        const val PADDING_BLOCK_SIZE = 8
        private const val ADDITIONAL_BYTES_SIZE = 17
        private const val SECRET_STREAM_K_SIZE = 32
        private const val SECRET_STREAM_NONCE_SIZE = 12
        private const val SECRET_STREAM_PAD_SIZE = 8
        private const val TAG_INTERMEDIATE: Byte = 0
        private const val MAC_SIZE = 16
        private const val NONCE_SIZE = 24
    }
}
