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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exchatge.R
import org.exchatge.view.Activity
import java.text.SimpleDateFormat

private val opponentUsername = "User" // TODO: debug only
private var enteredText by mutableStateOf("")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationPage(activity: Activity) = Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.appName))
                    Text(
                        text = opponentUsername,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            navigationIcon = {
                IconButton(onClick = { activity.currentPage = 1 }) { // TODO: debug only
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.file)
                    )
                }
            }
        )
    }
) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight(.9f)
                .fillMaxWidth()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            items(10) { // TODO: debug only
                Message(System.currentTimeMillis(), if (it % 2 == 0) "User$it" else null, "Text$it")
            }
        }
        Row(modifier = Modifier.fillMaxSize().padding(5.dp)) {
            TextField(
                value = enteredText,
                onValueChange = { enteredText = it },
                label = { Text(stringResource(R.string.message)) },
                singleLine = false,
                modifier = Modifier.fillMaxSize(),
                trailingIcon = {
                    IconButton(onClick = {}) {
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
private fun Message(timestamp: Long, from: String?, text: String) = Box(
    contentAlignment = if (from != null) Alignment.CenterStart else Alignment.CenterEnd,
    modifier = Modifier.fillMaxWidth().padding(5.dp)
) {
    Column(
        modifier = Modifier.fillMaxWidth(.48f),
        horizontalAlignment = if (from != null) Alignment.Start else Alignment.End
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Justify
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = SimpleDateFormat("HH:mm:ss MMM-dd-yyyy").format(timestamp),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(end = 5.dp)
            )
            Text(
                text = from ?: stringResource(R.string.you),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 5.dp)
            )
        }
    }
}
