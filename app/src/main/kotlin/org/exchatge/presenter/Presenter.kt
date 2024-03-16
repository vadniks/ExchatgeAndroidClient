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

import android.content.Intent
import android.os.Bundle
import org.exchatge.view.ConversationMessage
import org.exchatge.view.ConversationSetupDialogParameters
import org.exchatge.view.FileExchangeDialogParameters
import org.exchatge.view.User
import org.exchatge.view.View
import org.exchatge.view.ViewStub
import org.exchatge.view.pages.Pages
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface Presenter {
    val view: View?
    val currentPage: Pages
    val controlsEnabled: Boolean
    val loading: Boolean
    var username: String
    var password: String
    var host: String
    var port: Int
    var sskp: String
    val currentUser: String
    val admin: Boolean
    var broadcastMessage: String
    val maxBroadcastMessageSize: Int
    val opponentUsername: String
    var currentConversationMessage: String
    val conversationSetupDialogParameters: ConversationSetupDialogParameters?
    val showAdministrativeActions: Boolean
    val maxMessageTextSize: Int
    val fileExchangeDialogParameters: FileExchangeDialogParameters?

    fun onCreate(view: View, savedInstanceState: Bundle?) // TODO: handle config changes and process kill (save activity's state)
    fun onResume()
    fun onDestroy()
    fun logIn()
    fun register()
    fun settings()
    fun applySettings()
    fun updateUsersList()
    fun usersForEach(action: (User) -> Unit)
    fun administrate(done: Boolean)
    fun shutdownServer()
    fun sendBroadcast()
    fun conversation(id: Int, remove: Boolean)
    fun messagesForEach(action: (ConversationMessage) -> Unit)
    fun returnFromPage()
    fun fileChoose()
    fun sendMessage()
    fun onActivityResult(intent: Intent?, resultCode: Int)
}

private class StubPropertyDelegate<T : Any>(private val klass: KClass<T>) {

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = when (klass) {
        String::class -> ""
        Int::class -> 0
        Boolean::class -> false
        else -> Any()
    } as T

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {}
}

object PresenterStub : Presenter { // stub to make @Preview work; can be used to adjust the preview (set values to see how things change)
    private val stringStub = StubPropertyDelegate(String::class)
    private val intStub = StubPropertyDelegate(Int::class)

    override val view = ViewStub
    override val currentPage get() = Pages.LOG_IN_REGISTER
    override val controlsEnabled = true
    override val loading = true
    override var username by stringStub
    override var password by stringStub
    override var host by stringStub
    override var port by intStub
    override var sskp by stringStub
    override val currentUser = ""
    override val admin = false
    override var broadcastMessage by stringStub
    override val maxBroadcastMessageSize = 0
    override val opponentUsername = ""
    override var currentConversationMessage by stringStub
    override val conversationSetupDialogParameters: ConversationSetupDialogParameters? = null
    override val showAdministrativeActions = false
    override val maxMessageTextSize = 0
    override val fileExchangeDialogParameters: FileExchangeDialogParameters? = null

    override fun onCreate(view: View, savedInstanceState: Bundle?) {}
    override fun onResume() {}
    override fun onDestroy() {}
    override fun logIn() {}
    override fun register() {}
    override fun settings() {}
    override fun applySettings() {}
    override fun updateUsersList() {}
    override fun usersForEach(action: (User) -> Unit) {}
    override fun administrate(done: Boolean) {}
    override fun shutdownServer() {}
    override fun sendBroadcast() {}
    override fun conversation(id: Int, remove: Boolean) {}
    override fun messagesForEach(action: (ConversationMessage) -> Unit) {}
    override fun returnFromPage() {}
    override fun fileChoose() {}
    override fun sendMessage() {}
    override fun onActivityResult(intent: Intent?, resultCode: Int) {}
}
