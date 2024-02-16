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
import org.exchatge.R
import org.exchatge.model.kernel
import org.exchatge.model.net.UNHASHED_PASSWORD_SIZE
import org.exchatge.model.net.USERNAME_SIZE
import org.exchatge.view.Activity
import org.exchatge.view.View
import org.exchatge.view.pages.Pages
import kotlin.reflect.KProperty

class PresenterImpl(private val initiator: PresenterInitiator): Presenter {
    @Volatile override var view: View? = null; private set
    val activityRunning get() = view != null
    override var currentPage by SynchronizedMutableState(Pages.LOG_IN_REGISTER, this)
    override var controlsEnabled by SynchronizedMutableState(true, this)
    override var loading by SynchronizedMutableState(false, this)
    override var username by mutableStateOf("")
    override var password by mutableStateOf("")
    override var currentUser = ""
    override var admin = false
    override var opponentUsername = ""
    override var currentConversationMessage by mutableStateOf("")
    val credentials get() = username to password

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

    override fun logIn() {
        if (username.length !in 1..USERNAME_SIZE || password.length !in 1..UNHASHED_PASSWORD_SIZE) {
            view!!.snackbar(view!!.string(R.string.incorrectCredentialsLength))
            return
        }

        controlsEnabled = false
        loading = true
        initiator.scheduleLogIn()
    }

    fun onConnectFail() {
        if (activityRunning) view!!.snackbar(view!!.string(R.string.failedToConnect))
        loading = false
        controlsEnabled = true
    }

    fun onLoginResult(successful: Boolean) {
        if (!activityRunning) return

        view!!.snackbar(view!!.string(if (successful) R.string.loggedInSuccessfully else R.string.failedToLogIn))
        if (successful) currentPage = Pages.USERS_LIST

        loading = false
        controlsEnabled = true
    }

    fun onErrorReceived() {
        if (!activityRunning) return
    }

    fun onDisconnected() {
        if (activityRunning) view!!.snackbar(view!!.string(R.string.disconnected))
    }

    override fun register() {}

    override fun logOut() {}

    override fun administrate() {}

    override fun conversation(id: Int, remove: Boolean) {}

    override fun returnFromPage() {}

    override fun fileChoose() {}

    override fun sendMessage() {}

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
