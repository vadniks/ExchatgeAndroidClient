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

import org.exchatge.model.assert

const val MAX_MESSAGE_SIZE = 1 shl 8
const val TOKEN_TRAILING_SIZE = 16
const val TOKEN_UNSIGNED_VALUE_SIZE = 2 * 4
const val TOKEN_SIZE = TOKEN_UNSIGNED_VALUE_SIZE + 40 + TOKEN_TRAILING_SIZE
const val MESSAGE_HEAD_SIZE = 4 * 6 + 8 + TOKEN_SIZE
const val MAX_MESSAGE_BODY_SIZE = MAX_MESSAGE_SIZE - MESSAGE_HEAD_SIZE

data class NetMessage(
    val flag: Int,
    val timestamp: Long,
    val index: Int,
    val count: Int,
    val from: Int,
    val to: Int,
    val token: ByteArray,
    val body: ByteArray?
) {
    val size get() = body?.size ?: 0

    init {
        assert(
            timestamp >= 0
            && index >= 0
            && count >= 0
            && from >= 0
            && to >= 0
        )
        assert(token.size == TOKEN_SIZE)
        assert(body == null || body.size in 1..MAX_MESSAGE_BODY_SIZE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetMessage

        if (flag != other.flag) return false
        if (timestamp != other.timestamp) return false
        if (index != other.index) return false
        if (count != other.count) return false
        if (from != other.from) return false
        if (to != other.to) return false
        if (!token.contentEquals(other.token)) return false
        if (body != null) {
            if (other.body == null) return false
            if (!body.contentEquals(other.body)) return false
        } else if (other.body != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = flag
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + index
        result = 31 * result + count
        result = 31 * result + from
        result = 31 * result + to
        result = 31 * result + token.contentHashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }

//    fun pack(): ByteArray {
//        val bytes = ByteArray(MESSAGE_HEAD_SIZE + size)
//
//    }
}
