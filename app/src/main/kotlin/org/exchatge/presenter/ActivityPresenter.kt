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

import org.exchatge.model.Kernel
import org.exchatge.model.kernel
import org.exchatge.view.Activity

class ActivityPresenter(private val kernel: Kernel) {
    private var activityGetter: (() -> Activity)? = null // not storing the activity directly to avoid memory leak as its storing is a memory leak
    val activityRunning get() = activityGetter != null

    init {
        assert(!initialized)
        initialized = true
    }

    fun onActivityCreate(activity: Activity) {
        activityGetter = { activity }
        kernel.onActivityCreate()
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
