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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.exchatge.presenter.Presenter
import org.exchatge.presenter.PresenterImpl
import org.exchatge.presenter.PresenterStub
import org.exchatge.view.pages.ConversationPage
import org.exchatge.view.pages.LogInRegisterPage
import org.exchatge.view.pages.Pages
import org.exchatge.view.pages.UsersListPage
import org.exchatge.model.assert // TODO: move assert and runIn* from model to root package

class Activity : ComponentActivity(), View {
    private lateinit var presenter: Presenter
    @Volatile private var running = false
    private lateinit var showSnackbarImpl: suspend (String) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        running = true
        presenter = PresenterImpl.instance(this, savedInstanceState)
        setContent { Content(presenter) }
    }

    override fun setShowSnackbarImpl(impl: suspend (String) -> Unit) { showSnackbarImpl = impl }

    override fun snackbar(text: String) {
        assert(running)
        lifecycleScope.launch { showSnackbarImpl(text) }
    }

    override fun string(id: Int) = resources.getString(id)

    override fun onDestroy() {
        running = false
        presenter.onDestroy()
        super.onDestroy()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Content(
    presenter: Presenter = PresenterStub // gets replaced at runtime as in preview mode other modules and the activity itself are NOT even instantiated so stubs are needed
) = ExchatgeTheme(darkTheme = presenter is PresenterStub) {
    val xSnackbarHostState = remember { SnackbarHostState() }
    presenter.view!!.setShowSnackbarImpl(xSnackbarHostState::showSnackbar)

    val pagesSharedImpl = object : PagesShared, Presenter by presenter { // complex dependency injection
        override val snackbarHostState = xSnackbarHostState
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (presenter.currentPage) {
            Pages.LOG_IN_REGISTER -> LogInRegisterPage(pagesSharedImpl)
            Pages.USERS_LIST -> UsersListPage(pagesSharedImpl)
            Pages.CONVERSATION -> ConversationPage(pagesSharedImpl)
        }
    }
}
