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
import org.junit.Assert.assertNull
import java.nio.ByteOrder
import org.exchatge.model.assert
import org.exchatge.model.net.MESSAGE_HEAD_SIZE
import org.exchatge.model.net.NetMessage
import org.exchatge.model.net.TOKEN_SIZE
import org.exchatge.model.net.USERNAME_SIZE
import org.exchatge.model.net.USER_INFO_SIZE
import org.exchatge.model.net.UserInfo
import org.exchatge.model.net.boolean
import org.exchatge.model.net.byte
import org.exchatge.model.net.bytes
import org.exchatge.model.net.int
import org.exchatge.model.net.long
import kotlin.random.Random

class NetTest {

    @Test
    fun basic() {
        assertEquals(ByteOrder.nativeOrder(), ByteOrder.LITTLE_ENDIAN)
        @Suppress("KotlinConstantConditions")
        assert(Byte.SIZE_BYTES == 1 && Char.SIZE_BYTES == 2 && Int.SIZE_BYTES == 4 && Long.SIZE_BYTES == 8)
    }

    @Test
    fun booleanByte() {
        assertEquals(true.byte, 1.toByte())
        assertEquals(false.byte, 0.toByte())
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

    @Test
    fun messagePack() = booleanArrayOf(true, false).forEach { first ->
        val message = NetMessage(
            0, 1, 2, 3, 4, 5,
            ByteArray(TOKEN_SIZE) { 6 },
            if (first) ByteArray(10) { 7 } else null
        )
        assertEquals(message.size, if (first) message.body!!.size else 0)

        val packed = message.pack()
        assertEquals(packed.size, MESSAGE_HEAD_SIZE + message.size)

        assertEquals(packed.sliceArray(0 until 4).int, message.flag)
        assertEquals(packed.sliceArray(4 until (4 + 8)).long, message.timestamp)
        assertEquals(packed.sliceArray((4 + 8) until (4 * 2 + 8)).int, message.size)
        assertEquals(packed.sliceArray((4 * 2 + 8) until (4 * 3 + 8)).int, message.index)
        assertEquals(packed.sliceArray((4 * 3 + 8) until (4 * 4 + 8)).int, message.count)
        assertEquals(packed.sliceArray((4 * 4 + 8) until (4 * 5 + 8)).int, message.from)
        assertEquals(packed.sliceArray((4 * 5 + 8) until (4 * 6 + 8)).int, message.to)
        assertArrayEquals(packed.sliceArray((4 * 6 + 8) until (4 * 6 + 8 + TOKEN_SIZE)), message.token)

        if (first)
            assertArrayEquals(packed.sliceArray(MESSAGE_HEAD_SIZE until (MESSAGE_HEAD_SIZE + message.size)), message.body)
        else
            assertNull(message.body)
    }

    @Test
    fun messageUnpack() = booleanArrayOf(true, false).forEach { first ->
        val original = NetMessage(
            0, 1, 2, 3, 4, 5,
            ByteArray(TOKEN_SIZE) { 6 },
            if (first) ByteArray(10) { 7 } else null
        )

        val packed = original.pack()
        val unpacked = NetMessage.unpack(packed)

        assertEquals(original, unpacked)
    }

    @Test
    fun userInfoUnpack() {
        val original = UserInfo(1, false, Random.nextBytes(USERNAME_SIZE))

        val packed = ByteArray(USER_INFO_SIZE)
        System.arraycopy(original.id.bytes, 0, packed, 0, 4)
        packed[4] = original.connected.byte
        System.arraycopy(original.name, 0, packed, 5, USERNAME_SIZE)

        val unpacked = UserInfo.unpack(packed)
        assertEquals(original.id, unpacked.id)
        assertEquals(original.connected, unpacked.connected)
        assertArrayEquals(original.name, unpacked.name)
    }
}
