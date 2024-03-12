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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exchatge.R
import org.exchatge.view.PagesShared
import org.exchatge.view.User
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListPage(pagesShared: PagesShared) = Scaffold(
    snackbarHost = { SnackbarHost(pagesShared.snackbarHostState) },
    topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.appName))
                    Text(
                        text = pagesShared.currentUser,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        fontWeight = if (!pagesShared.admin) FontWeight.Normal else FontWeight.Bold
                    )
                }
            },
            colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            navigationIcon = {
                IconButton(
                    onClick = pagesShared::returnFromPage,
                    enabled = pagesShared.controlsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.logOut)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = pagesShared::updateUsersList,
                    enabled = pagesShared.controlsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.update)
                    )
                }
                if (pagesShared.admin) IconButton(
                    onClick = { pagesShared.administrate(false) },
                    enabled = pagesShared.controlsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.administrate)
                    )
                }
            }
        )
    }
) { paddingValues ->
    Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
        if (pagesShared.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            pagesShared.usersForEach {
                item {
                    UserInfo(pagesShared, it)
                }
            }
        }
        if (pagesShared.showAdministrativeActions) AdminActionsBottomSheet(pagesShared)
    }
}

@Composable
private fun UserInfo(
    pagesShared: PagesShared,
    user: User
) = ListItem(
    leadingContent = {
        Box(modifier = Modifier.fillMaxWidth(.05f)) {
            Box(modifier = Modifier
                .size((LocalConfiguration.current.let { min(it.screenWidthDp, it.screenHeightDp) } * .04f).dp)
                .clip(CircleShape)
                .background(if (user.online) Color.Green else Color.Transparent)
                .border(.01.dp, Color.Gray, CircleShape)) {}
        }
    },
    headlineContent = {
        Text(
            text = user.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(.60f)
        )
    },
    supportingContent = {
        Text(
            text = stringResource(R.string.idEqualsTo) + ' ' + user.id.toString(),
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.fillMaxWidth(.60f)
        )
    },
    trailingContent = {
        Row(horizontalArrangement = Arrangement.Center) {
            IconButton(
                onClick = { pagesShared.conversation(user.id, false) },
                enabled = pagesShared.controlsEnabled
            ) {
                Icon(
                    imageVector = if (user.conversationExists) Icons.Filled.Send else Icons.Filled.Add,
                    contentDescription = stringResource(if (user.conversationExists) R.string.continueConversation else R.string.startConversation)
                )
            }
            if (user.conversationExists) IconButton(
                onClick = { pagesShared.conversation(user.id, true) },
                enabled = pagesShared.controlsEnabled
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.deleteConversation)
                )
            }
        }
    },
    modifier = Modifier.fillMaxWidth()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminActionsBottomSheet(pagesShared: PagesShared) {
    val state = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { pagesShared.administrate(true) },
        sheetState = state
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(.4f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = pagesShared::shutdownServer) {
                Text(stringResource(R.string.shutdownServer))
            }
            Spacer(modifier = Modifier.padding(5.dp))
            TextField(
                value = pagesShared.broadcastMessage,
                onValueChange = {
                    if (it.length <= pagesShared.maxBroadcastMessageSize)
                        pagesShared.broadcastMessage = it
                },
                label = {
                    Text(stringResource(R.string.broadcastMessage))
                },
                singleLine = true,
                supportingText = {
                    Text(
                        text = stringResource(R.string.broadcastMessageHint),
                        textAlign = TextAlign.Justify
                    )
                },
                trailingIcon = {
                    IconButton(onClick = pagesShared::sendBroadcast) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = stringResource(R.string.send)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(.75f)
            )
        }
    }
}
