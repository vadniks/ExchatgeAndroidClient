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

@file:Suppress("DEPRECATION") // PresenterStub

package org.exchatge.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.exchatge.presenter.Presenter
import org.exchatge.presenter.PresenterImpl
import org.exchatge.presenter.PresenterStub
import org.exchatge.view.pages.ConversationPage
import org.exchatge.view.pages.LogInRegisterPage
import org.exchatge.view.pages.Pages
import org.exchatge.view.pages.UsersListPage

class Activity : ComponentActivity(), View {
    private lateinit var presenter: Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = PresenterImpl.instance(this, savedInstanceState)
        setContent { Content(presenter) }
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Content(
    presenter: Presenter = PresenterStub // is replaced at runtime
) = ExchatgeTheme/*(darkTheme = true)*/ {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (presenter.currentPage) {
            Pages.LOG_IN_REGISTER -> LogInRegisterPage(presenter)
            Pages.USERS_LIST -> UsersListPage(presenter)
            Pages.CONVERSATION -> ConversationPage(presenter)
        }
    }
}
