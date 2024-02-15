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

package org.exchatge.presenter

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.exchatge.model.kernel
import org.exchatge.view.Activity
import org.exchatge.view.View
import org.exchatge.view.pages.Pages

class PresenterImpl(private val initiator: PresenterInitiator): Presenter {
    @Volatile private var view: View? = null
    val activityRunning get() = view != null
    override var currentPage by mutableStateOf(Pages.LOG_IN_REGISTER)
    override var username by mutableStateOf("")
    override var password by mutableStateOf("")
    override var currentUser = ""
    override var admin = false
    override var opponentUsername = ""
    override var currentConversationMessage by mutableStateOf("")

    init {
        assert(!initialized)
        initialized = true
    }

    override fun onCreate(view: View, savedInstanceState: Bundle?) {
        this.view = view
        initiator.onActivityCreate()
    }

    override fun onDestroy() {
        view = null
        initiator.onActivityDestroy()
    }

    override fun logInRequested() {}
    override fun registerRequested() {}

    override fun logOutRequested() {}
    override fun administrateRequested() {}
    override fun conversationRequested(id: Int, remove: Boolean) {}

    override fun returnFromPageRequested() {}
    override fun fileChooseRequested() {}
    override fun sendMessageRequested() {}

    companion object {
        @JvmStatic
        private var initialized = false

        @JvmStatic
        fun instance(activity: Activity, savedInstanceState: Bundle?) = activity
            .applicationContext
            .kernel
            .presenter
            .also { it.onCreate(activity, savedInstanceState) } as Presenter
    }
}
