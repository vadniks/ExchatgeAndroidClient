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

package org.exchatge.model.database

import android.content.Context
import androidx.room.DatabaseConfiguration
import androidx.room.Room
import androidx.room.RoomDatabase
import org.exchatge.model.Crypto
import org.exchatge.model.assert
import org.exchatge.model.log

@androidx.room.Database(version = 1, entities = [Conversation::class, Message::class])
abstract class Database : RoomDatabase() {
    abstract val conversationDao: ConversationDao
    abstract val messageDao: MessageDao
    private lateinit var encryptionKey: ByteArray
    private lateinit var crypto: Crypto

    override fun init(configuration: DatabaseConfiguration) {
        super.init(configuration)

        for (i in conversationDao.javaClass.superclass.declaredFields) i.apply {
            when {
                name.contains(conversationDao::encrypt.name) -> {
                    isAccessible = true
                    set(conversationDao, this@Database::encrypt)
                    isAccessible = false
                }
                name.contains(conversationDao::decrypt.name) -> {
                    isAccessible = true
                    set(conversationDao, this@Database::decrypt)
                    isAccessible = false
                }
            }
        }
    }

    fun postInit(encryptionKey: ByteArray, crypto: Crypto): Database {
        this.encryptionKey = encryptionKey
        this.crypto = crypto
        return this
    }

    private fun encrypt(bytes: ByteArray): ByteArray? = crypto.encryptSingle(encryptionKey, bytes)

    private fun decrypt(bytes: ByteArray): ByteArray? = crypto.decryptSingle(encryptionKey, bytes)

    override fun close() {
        super.close()
        initialized = false // TODO: zero out encryption key
    }

    companion object {
        @JvmStatic
        private var initialized = false

        @JvmStatic
        fun init(context: Context): Database {
            assert(!initialized)
            initialized = true

            return Room
                .databaseBuilder(context, Database::class.java, Database::class.simpleName)
                .setJournalMode(JournalMode.TRUNCATE)
                .build()
        }
    }
}
