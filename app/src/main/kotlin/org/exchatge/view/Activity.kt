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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.exchatge.presenter.ActivityPresenter
import org.exchatge.presenter.ActivityPresenter.Companion.activityPresenter
import org.exchatge.view.pages.ConversationPage
import org.exchatge.view.pages.LogInRegisterPage
import org.exchatge.view.pages.UsersListPage

class Activity : ComponentActivity() {
    private lateinit var xPresenter: ActivityPresenter
    val presenter get() = xPresenter
    var currentPage by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        xPresenter = activityPresenter

        setContent {
            Preview(currentPage, this)
        }
    }

    override fun onDestroy() {
        xPresenter.onActivityDestroy()
        super.onDestroy()
    }
}

// TODO: make currentPage's type to be enum

//@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Preview(currentPage: Int, activity: Activity) = ExchatgeTheme(darkTheme = true) {
    Surface(
        modifier = Modifier.fillMaxSize(), //.border(1.0f.dp, color = Color.Black, RoundedCornerShape(1.0f.dp)),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentPage) {
            0 -> LogInRegisterPage(activity)
            1 -> UsersListPage(activity)
            2 -> ConversationPage(activity)
        }
    }
}
