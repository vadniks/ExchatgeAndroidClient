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

package org.exchatge.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.exchatge.R

@Composable
fun ConversationSetupDialog(requestedByHost: Boolean, opponentId: Int, opponentName: String) = AlertDialog(
    onDismissRequest = {},
    confirmButton = {
        Text(stringResource(R.string.proceed))
    },
    dismissButton = {
        Text(stringResource(R.string.cancel))
    },
    title = {
        Text(stringResource(R.string.startConversation))
    },
    text = {
        val prefix = stringResource(if (requestedByHost) R.string.sendConversationSetupRequestTo else R.string.conversationSetupRequestReceivedFrom)
        Text("$prefix $opponentName (${stringResource(R.string.id)} $opponentId)")
    },
)

@Composable
fun FileExchangeDialog(
    opponentId: Int,
    opponentName: String,
    fileName: String,
    fileSize: Int
) = AlertDialog(
    onDismissRequest = {},
    confirmButton = {
        Text(stringResource(R.string.proceed))
    },
    dismissButton = {
        Text(stringResource(R.string.cancel))
    },
    title = {
        Text(stringResource(R.string.fileExchange))
    },
    text = {
        Text("""
            ${stringResource(R.string.fileExchangeRequestedByUser)} $opponentName (${stringResource(R.string.id)} $opponentId)
            ${stringResource(R.string.file).lowercase()} $fileName ${stringResource(R.string.withSizeOf)} $fileSize ${stringResource(R.string.bytes)}
        """.trimIndent())
    },
)
