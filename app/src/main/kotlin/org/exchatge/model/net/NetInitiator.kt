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

import android.content.Context
import org.exchatge.model.Crypto
import org.exchatge.model.Options

interface NetInitiator {
    val context: Context
    val crypto: Crypto

    fun loadOptions(): Options
    fun onConnectResult(successful: Boolean)
    fun onNetDestroy()
    fun onLogInResult(successful: Boolean)
    fun onRegisterResult(successful: Boolean)
    fun onNextUserFetched(user: UserInfo, last: Boolean)
    fun onConversationSetUpInviteReceived(fromId: Int)
    fun onMessageReceived(timestamp: Long, from: Int, body: ByteArray)
    fun onBroadcastReceived(body: ByteArray)
    fun onNextMessageFetched(from: Int, timestamp: Long, body: ByteArray?, last: Boolean)
    fun nextFileChunkSupplier(index: Int, buffer: ByteArray): Int
    fun onFileExchangeInviteReceived(from: Int, fileSize: Int, hash: ByteArray, filename: ByteArray)
    fun nextFileChunkReceiver(from: Int, index: Int, receivedBytesCount: Int, buffer: ByteArray)
}
