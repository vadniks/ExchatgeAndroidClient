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

import org.exchatge.model.kernel
import org.exchatge.view.Activity
import org.exchatge.view.View

class Presenter(private val initiator: PresenterInitiator) {
    @Volatile private var view: View? = null
    val activityRunning get() = view != null

    init {
        assert(!initialized)
        initialized = true
    }

    fun onActivityCreate(view: View) {
        this.view = view
        initiator.onActivityCreate()
    }

    fun onActivityDestroy() {
        view = null
        initiator.onActivityDestroy()
    }

    companion object {
        @JvmStatic
        private var initialized = false

        @JvmStatic
        fun instance(activity: Activity) = activity.applicationContext.kernel.presenter.also { it.onActivityCreate(activity) }
    }
}
