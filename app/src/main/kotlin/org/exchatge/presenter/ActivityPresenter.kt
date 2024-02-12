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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.exchatge.R
import org.exchatge.model.Kernel
import org.exchatge.model.kernel
import org.exchatge.model.log
import org.exchatge.model.runInMainThread
import org.exchatge.view.Activity
import java.util.concurrent.atomic.AtomicBoolean

class ActivityPresenter(private val kernel: Kernel) {
    private var activityGetter: (() -> Activity)? = null // not storing the activity directly to avoid memory leak as its storing is a memory leak
    val activityRunning get() = activityGetter != null
    private val loading = AtomicBoolean(false)
    private val activity get() = activityGetter?.invoke()

    init {
        assert(!initialized)
        initialized = true
    }

    fun onActivityCreate(activity: Activity) {
        activityGetter = { activity }
        kernel.onActivityCreate()
    }

    fun showSnackbar(text: String) {
        if (!activityRunning) return
        runInMainThread { kernel.toast(text) } // TODO: figure out how to show snackbar in compose
    }

    fun logIn(username: String, password: String) {
        if (!activityRunning) return
        loading.set(true)
         runBlocking { launch(Dispatchers.Default) { log("x ap login"); kernel.logIn(username, password) } }
    }

    fun onLogInResult(successful: Boolean) {
        if (!activityRunning) return

        if (successful) activity?.currentPage = 1
        else showSnackbar(activity!!.getString(R.string.error))

        loading.set(false)
    }

    fun onActivityDestroy() {
        activityGetter = null
        kernel.onActivityDestroy()
    }

    companion object {
        @JvmStatic
        private var initialized = false

        @JvmStatic
        val Activity.activityPresenter get() = kernel.presenter.also { it.onActivityCreate(this) }
    }
}
