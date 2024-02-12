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

const val USERNAME_SIZE = 16
const val UNHASHED_PASSWORD_SIZE = 16
const val USER_INFO_SIZE = 4 + 1 + USERNAME_SIZE

data class UserInfo(
    val id: Int,
    val connected: Boolean,
    val name: ByteArray
) {

    init { assert(id >= 0 && name.size in 1..USERNAME_SIZE) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserInfo

        if (id != other.id) return false
        if (connected != other.connected) return false
        if (!name.contentEquals(other.name)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + connected.hashCode()
        result = 31 * result + name.contentHashCode()
        return result
    }

    override fun toString() = "UserInfo(id=$id, connected=$connected, name=${name.contentToString()})"

    companion object {

        fun unpack(bytes: ByteArray): UserInfo {
            assert(bytes.size == USER_INFO_SIZE)

            return UserInfo(
                bytes.sliceArray(0 until 4).int,
                bytes[4].boolean,
                bytes.sliceArray(5 until (5 + USERNAME_SIZE))
            )
        }
    }
}
