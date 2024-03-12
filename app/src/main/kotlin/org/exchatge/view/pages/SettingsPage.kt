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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.exchatge.R
import org.exchatge.view.PagesShared

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(pagesShared: PagesShared) = Scaffold(
    snackbarHost = { SnackbarHost(pagesShared.snackbarHostState) }, // TODO: extract host
    topBar = {
        TopAppBar(
            title = {
                Text(stringResource(R.string.settings))
            },
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
            }
        )
    }
) { paddingValues ->
    Column(
        Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val padding = Modifier.padding(5.dp)
        TextField(
            value = pagesShared.host,
            onValueChange = { pagesShared.host = it },
            enabled = pagesShared.controlsEnabled,
            modifier = padding,
            label = {
                Text(stringResource(R.string.host))
            }
        )
        TextField(
            value = pagesShared.port.toString(),
            onValueChange = { pagesShared.port = it.toIntOrNull() ?: return@TextField },
            enabled = pagesShared.controlsEnabled,
            modifier = padding,
            label = {
                Text(stringResource(R.string.port))
            }
        )
        TextField(
            value = pagesShared.sskp,
            onValueChange = { pagesShared.sskp = it },
            enabled = pagesShared.controlsEnabled,
            modifier = padding,
            label = {
                Text(stringResource(R.string.sskp))
            }
        )
        Button(
            onClick = pagesShared::applySettings,
            enabled = pagesShared.controlsEnabled
        ) {
            Text(stringResource(R.string.apply))
        }
    }
}
