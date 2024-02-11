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

val Boolean.byte get(): Byte = if (this) 1 else 0

val Byte.boolean get() = when (this) {
    1.toByte() -> true
    0.toByte() -> false
    else -> throw IllegalStateException()
}

val Int.bytes get() = byteArrayOf(
    this.toByte(),
    (this shr 8).toByte(),
    (this shr 16).toByte(),
    (this shr 24).toByte()
)

val ByteArray.int get(): Int {
    assert(size == 4)
    return (this[0].toInt() and 0xff) or
        ((this[1].toInt() and 0xff) shl 8) or
        ((this[2].toInt() and 0xff) shl 16) or
        (this[3].toInt() shl 24)
}

val Long.bytes get() = byteArrayOf(
    this.toByte(),
    (this shr 8).toByte(),
    (this shr 16).toByte(),
    (this shr 24).toByte(),
    (this shr 32).toByte(),
    (this shr 40).toByte(),
    (this shr 48).toByte(),
    (this shr 56).toByte(),
)

val ByteArray.long get(): Long {
    assert(size == 8)
    return (this[0].toLong() and 0xff) or
        ((this[1].toLong() and 0xff) shl 8) or
        ((this[2].toLong() and 0xff) shl 16) or
        ((this[3].toLong() and 0xff) shl 24) or
        ((this[4].toLong() and 0xff) shl 32) or
        ((this[5].toLong() and 0xff) shl 40) or
        ((this[6].toLong() and 0xff) shl 48) or
        (this[7].toLong() shl 56)
}
