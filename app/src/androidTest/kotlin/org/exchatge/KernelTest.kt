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
import org.exchatge.model.kernel
import org.exchatge.model.net.UNHASHED_PASSWORD_SIZE
import org.exchatge.model.net.USERNAME_SIZE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KernelTest {
    private val kernel = InstrumentationRegistry.getInstrumentation().targetContext.kernel

    @Test
    fun credentialsEncryption() = booleanArrayOf(true, false).forEach { first ->
        val original =
            if (first) "abcde" to "1234567"
            else String(ByteArray(USERNAME_SIZE) { 1 }) to String(ByteArray(UNHASHED_PASSWORD_SIZE) { 2 })

        val encrypted = kernel.encryptCredentials(original)
        val decrypted = kernel.decryptCredentials(encrypted)

        assertNotNull(decrypted)
        assertEquals(decrypted!!.first.length, USERNAME_SIZE)
        assertEquals(decrypted.second.length, UNHASHED_PASSWORD_SIZE)

        assertEquals(original.first, decrypted.first.slice(0 until original.first.length))
        assertEquals(original.second, decrypted.second.slice(0 until original.second.length))
    }
}
