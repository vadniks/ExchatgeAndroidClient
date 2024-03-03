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

package org.exchatge.view.pages

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exchatge.R
import org.exchatge.view.ConversationMessage
import org.exchatge.view.PagesShared
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationPage(pagesShared: PagesShared) = Scaffold(
    snackbarHost = { SnackbarHost(pagesShared.snackbarHostState) },
    topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.appName))
                    Text(
                        text = pagesShared.opponentUsername,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            navigationIcon = {
                IconButton(
                    onClick = pagesShared::returnFromPage,
                    enabled = pagesShared.controlsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = pagesShared::fileChoose,
                    enabled = pagesShared.controlsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.file)
                    )
                }
            }
        )
    }
) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
        if (pagesShared.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier.fillMaxHeight(
                if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) .9f else .8f
            ).fillMaxWidth()
        ) {
            pagesShared.messagesForEach {
                item {
                    Message(it)
                }
            }
        }
        Row(modifier = Modifier.fillMaxSize().padding(5.dp)) {
            TextField(
                value = pagesShared.currentConversationMessage,
                onValueChange = {
                    pagesShared.currentConversationMessage =
                        (if (it.length >= pagesShared.maxMessageTextSize)
                            it.slice(0 until pagesShared.maxMessageTextSize)
                        else
                            it).replace('\n', ' ') // 'cause the desktop client can't process new lines - additional logic to handle them isn't yet implemented
                },
                label = { Text(stringResource(R.string.message)) },
                singleLine = true,
                modifier = Modifier.fillMaxSize(),
                enabled = pagesShared.controlsEnabled,
                trailingIcon = {
                    IconButton(
                        onClick = pagesShared::sendMessage,
                        enabled = pagesShared.controlsEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = stringResource(R.string.send)
                        )
                    }
                }
            )
        }
    }
}

@SuppressLint("SimpleDateFormat")
@Composable
private fun Message(message: ConversationMessage) = Box(
    contentAlignment = if (message.from != null) Alignment.CenterStart else Alignment.CenterEnd,
    modifier = Modifier.fillMaxWidth().padding(5.dp)
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth(.48f).padding(5.dp),
            horizontalAlignment = if (message.from != null) Alignment.Start else Alignment.End
        ) {
            Text(
                text = message.text,
                textAlign = TextAlign.Justify
            )
            Spacer(modifier = Modifier.fillMaxWidth().height(5.dp))
            Text(
                text = SimpleDateFormat(stringResource(R.string.timestampFormat)).format(message.timestamp),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(end = 5.dp)
            )
        }
    }
}
