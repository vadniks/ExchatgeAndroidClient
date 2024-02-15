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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exchatge.R
import org.exchatge.view.PagesShared

private val paddingModifier = Modifier.padding(2.5f.dp)

@Composable
fun LogInRegisterPage(pagesShared: PagesShared) = Scaffold(
    snackbarHost = { SnackbarHost(pagesShared.snackbarHostState) }
) { paddingValues ->
    Column(
        modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        android.R.layout.activity_list_item
        Column(
            modifier = Modifier.fillMaxWidth(0.75f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.appName),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = paddingModifier
            )
            TextField(pagesShared.username, R.string.username, false) { pagesShared.username = it }
            TextField(pagesShared.password, R.string.password, true) { pagesShared.password = it }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = pagesShared::logIn,
                    modifier = paddingModifier
                ) {
                    Text(stringResource(R.string.logIn))
                }
                Button(
                    onClick = pagesShared::register,
                    modifier = paddingModifier
                ) {
                    Text(stringResource(R.string.register))
                }
            }
        }
    }
}

@Composable
private fun TextField(
    value: String,
    label: Int,
    password: Boolean,
    onValueChange: (String) -> Unit
) = TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = paddingModifier.fillMaxWidth(),
    label = { Text(stringResource(label)) },
    singleLine = true,
    visualTransformation = if (!password) VisualTransformation.None else PasswordVisualTransformation()
)
