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

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = Conversation.CONVERSATIONS,
    indices = [Index(value = [Conversation.USER], unique = true)]
)
data class Conversation(
    @PrimaryKey val user: Int,
    val coders: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Conversation

        if (user != other.user) return false
        if (!coders.contentEquals(other.coders)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user
        result = 31 * result + coders.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }

    companion object {
        const val CONVERSATION = "conversation"
        const val CONVERSATIONS = "conversations"
        const val USER = "user"
        const val CODERS = "coders"
        const val TIMESTAMP = "timestamp"
    }
}
