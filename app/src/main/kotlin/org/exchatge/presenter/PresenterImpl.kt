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
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.exchatge.R
import org.exchatge.model.kernel
import org.exchatge.model.net.UserInfo
import org.exchatge.model.runInMain
import org.exchatge.view.Activity
import org.exchatge.view.ConversationSetupDialogParameters
import org.exchatge.view.User
import org.exchatge.view.View
import org.exchatge.view.pages.Pages
import kotlin.reflect.KProperty

class PresenterImpl(private val initiator: PresenterInitiator): Presenter {
    @Volatile override var view: View? = null; private set
    val activityRunning get() = view != null
    override var currentPage by SynchronizedMutableState(Pages.LOG_IN_REGISTER, this)
    override var controlsEnabled by SynchronizedMutableState(true, this)
    override var loading by SynchronizedMutableState(false, this)
    override var username by mutableStateOf("") // TODO: synchronize ui string fields too?
    override var password by mutableStateOf("")
    override val users = mutableStateListOf<User>()
    val credentials get() = username to password
    override var currentUser by mutableStateOf("")
    override var admin by mutableStateOf(false)
    override var opponentUsername = ""
    override var currentConversationMessage by mutableStateOf("")
    override var conversationSetupDialogParameters by SynchronizedMutableState<ConversationSetupDialogParameters?>(null, this)

    init {
        assert(!initialized)
        initialized = true
    }

    override fun onCreate(view: View, savedInstanceState: Bundle?) {
        this.view = view

        view.addOnBackPressedCallback(object : OnBackPressedCallback(true)
        { override fun handleOnBackPressed() = this@PresenterImpl.handleOnBackPressed() })

        initiator.onActivityCreate()
        setUiLock(true)
    }

    private fun handleOnBackPressed() {
        if (!controlsEnabled) return
        when (currentPage) {
            Pages.CONVERSATION -> currentPage = Pages.USERS_LIST
            Pages.USERS_LIST -> {
                setUiLock(true)
                initiator.scheduleLogOut()
            }
            Pages.LOG_IN_REGISTER -> view!!.finish()
        }
    }

    private fun setUiLock(lock: Boolean) {
        controlsEnabled = !lock
        loading = lock
    }

    override fun onResume() = setUiLock(initiator.onActivityResume())

    override fun onDestroy() {
        view = null
        initiator.onActivityDestroy()
    }

    override fun logIn() {
        if (!initiator.credentialsLengthCorrect(username, password)) {
            view!!.snackbar(view!!.string(R.string.incorrectCredentialsLength))
            return
        }

        setUiLock(true)
        initiator.scheduleLogIn()
    }

    fun onConnectFail() {
        if (!activityRunning) return
        view!!.snackbar(view!!.string(R.string.failedToConnect))
        setUiLock(false)
    }

    fun onLogInResult(successful: Boolean) {
        if (!activityRunning) return
        view!!.snackbar(view!!.string(if (successful) R.string.loggedInSuccessfully else R.string.failedToLogIn))

        if (successful) {
            username = ""; password = "" // TODO: fill with mess before drop
            users.clear()
            currentPage = Pages.USERS_LIST
            initiator.scheduleUsersFetch()
        } else
            setUiLock(false)
    }

    fun onNextUserFetched(userInfo: UserInfo, last: Boolean) {
        runInMain {
            synchronized(this) {
                if (userInfo.id == initiator.currentUserId) {
                    currentUser = String(userInfo.name) // TODO: trim trailing zeroes
                    admin = initiator.admin(userInfo.id)
                } else
                    users.add(User(userInfo.id, String(userInfo.name), userInfo.connected, false))
            }
        }

        if (!last) return
        setUiLock(false)
    }

    fun onErrorReceived() {
        if (!activityRunning) return
    }

    fun onDisconnected() {
        if (!activityRunning) return

        currentPage = Pages.LOG_IN_REGISTER
        setUiLock(false)
        view!!.snackbar(view!!.string(R.string.disconnected))
    }

    override fun register() {}

    override fun updateUsersList() {
        setUiLock(true)
        users.clear()
        initiator.scheduleUsersFetch()
    }

    override fun administrate() {}

    override fun conversation(id: Int, remove: Boolean) {}

    override fun returnFromPage() = handleOnBackPressed()

    override fun fileChoose() {}

    override fun sendMessage() {}

    fun showConversationSetUpDialog(requestedByHost: Boolean, opponentId: Int, opponentName: String) =
        this::conversationSetupDialogParameters.set(ConversationSetupDialogParameters(
            requestedByHost, opponentId, opponentName,
            initiator::onConversationSetupDialogAction,
        ))

    fun hideConversationSetupDialog() = this::conversationSetupDialogParameters.set(null)

    fun onReplyToConversationSetup(result: Boolean? = null) {
        if (!activityRunning) return
        setUiLock(result == null)
        view!!.snackbar("Conversation set up " + if (result ?: return) "succeeded" else "failed") // TODO: extract string
    }

    private class SynchronizedMutableState<T>(initial: T, private val lock: Any) {
        private val delegate = mutableStateOf(initial)

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
            synchronized(lock) { delegate.getValue(thisRef, property) }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
            synchronized(lock) { delegate.setValue(thisRef, property, value) }
    }

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
