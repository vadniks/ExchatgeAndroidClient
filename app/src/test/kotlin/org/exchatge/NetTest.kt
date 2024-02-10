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

import org.junit.Test
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import java.nio.ByteOrder
import org.exchatge.model.assert
import org.exchatge.model.net.boolean
import org.exchatge.model.net.byte
import org.exchatge.model.net.bytes
import org.exchatge.model.net.int
import org.exchatge.model.net.long

class NetTest {

    @Test
    fun basic() {
        assertEquals(ByteOrder.nativeOrder(), ByteOrder.LITTLE_ENDIAN)
        @Suppress("KotlinConstantConditions")
        assert(Byte.SIZE_BYTES == 1 && Char.SIZE_BYTES == 2 && Int.SIZE_BYTES == 4 && Long.SIZE_BYTES == 8)
    }

    @Test
    fun booleanByte() {
        assertEquals(true.byte, 1)
        assertEquals(false.byte, 0)
        assertEquals(0.toByte().boolean, false)
        assertEquals(1.toByte().boolean, true)
    }

    @Test
    fun intBytes() {
        var n = 0x01234567
        var bytes = n.bytes
        assertArrayEquals(bytes, byteArrayOf(0x67, 0x45, 0x23, 0x01))
        assertEquals(bytes.int, n)

        n = 0x76543210
        bytes = n.bytes
        assertArrayEquals(bytes, byteArrayOf(0x10, 0x32, 0x54, 0x76))
        assertEquals(bytes.int, n)
    }

    @Test
    fun longBytes() {
        var n = 0x0123456789abcdefUL.toLong()
        var bytes = n.bytes
        assertArrayEquals(bytes, byteArrayOf(0xef.toByte(), 0xcd.toByte(), 0xab.toByte(), 0x89.toByte(), 0x67, 0x45, 0x23, 0x01))
        assertEquals(bytes.long, n)

        n = 0xfedcba9876543210UL.toLong()
        bytes = n.bytes
        assertArrayEquals(bytes, byteArrayOf(0x10, 0x32, 0x54, 0x76, 0x98.toByte(), 0xba.toByte(), 0xdc.toByte(), 0xfe.toByte()))
        assertEquals(bytes.long, n)
    }
}
