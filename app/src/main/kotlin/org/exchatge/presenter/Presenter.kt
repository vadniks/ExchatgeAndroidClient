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
import org.exchatge.view.View
import org.exchatge.view.pages.Pages

interface Presenter {
    val currentPage: Pages

    var username: String
    var password: String

    val currentUser: String
    val admin: Boolean

    val opponentUsername: String
    var currentConversationMessage: String

    //

    fun onCreate(view: View, savedInstanceState: Bundle?) // TODO: handle config changes and process kill (save activity's state)
    fun onDestroy()

    fun logInRequested()
    fun registerRequested()

    fun logOutRequested()
    fun administrateRequested()
    fun conversationRequested(id: Int, remove: Boolean)

    fun returnFromPageRequested()
    fun fileChooseRequested()
    fun sendMessageRequested()
}

@Deprecated("stub to make @Preview work")
object PresenterStub : Presenter {
    override val currentPage get() = Pages.LOG_IN_REGISTER
    override var username get() = ""; set(_) {}
    override var password get() = ""; set(_) {}
    override val currentUser = ""
    override val admin = false
    override val opponentUsername = ""
    override var currentConversationMessage get() = ""; set(_) {}

    override fun onCreate(view: View, savedInstanceState: Bundle?) {}
    override fun onDestroy() {}
    override fun logInRequested() {}
    override fun registerRequested() {}
    override fun logOutRequested() {}
    override fun administrateRequested() {}
    override fun conversationRequested(id: Int, remove: Boolean) {}
    override fun returnFromPageRequested() {}
    override fun fileChooseRequested() {}
    override fun sendMessageRequested() {}
}
