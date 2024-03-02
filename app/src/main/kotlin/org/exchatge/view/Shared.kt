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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Popup
import org.exchatge.R

data class User(val id: Int, val name: String, val online: Boolean, val conversationExists: Boolean)

data class ConversationMessage(val timestamp: Long, val from: String?, val text: String)

@Composable
private fun PopupOverlay(content: @Composable () -> Unit) = Popup {
    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent.copy(alpha = .25f))) { content() }
}

data class ConversationSetupDialogParameters(
    val requestedByHost: Boolean,
    val opponentId: Int,
    val opponentName: String,
    val onAction: (Boolean) -> Unit // proceed - true, dismiss - false
)

@Composable
fun ConversationSetupDialog(parameters: ConversationSetupDialogParameters) = PopupOverlay { // TODO: move in separate file (Dialogs.kt)
    AlertDialog(
        onDismissRequest = { parameters.onAction(false) },
        confirmButton = {
            Text(
                text = stringResource(R.string.proceed),
                modifier = Modifier.clickable { parameters.onAction(true) }
            )
        },
        dismissButton = {
            Text(
                text = stringResource(R.string.cancel),
                modifier = Modifier.clickable { parameters.onAction(false) }
            )
        },
        title = {
            Text(stringResource(R.string.startConversation))
        },
        text = {
            Text(
                text = buildAnnotatedString {
                    append(stringResource(
                        if (parameters.requestedByHost) R.string.sendConversationSetupRequestTo
                        else R.string.conversationSetupRequestReceivedFrom
                    ))
                    append(' ')
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(parameters.opponentName) }
                    append(' ')
                    append("(${stringResource(R.string.id)} ${parameters.opponentId})")
                },
                textAlign = TextAlign.Justify
            )
        }
    )
}

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
