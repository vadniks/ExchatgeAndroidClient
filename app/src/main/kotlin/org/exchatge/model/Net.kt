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

package org.exchatge.model

import android.content.Intent

class Net(private val kernel: Kernel) {

    init {
        assert(!initialized)
        initialized = true

        if (!NetService.running)
            kernel.context.startService(Intent(kernel.context, NetService::class.java))!! // TODO: start the service only if the user has logged in
    }

    fun listen() { // TODO: add an 'exit' button to UI which will close the activity as well as the service to completely shutdown the whole app
        while (NetService.running) {
            Thread.sleep(500)
        }
    }

    fun onDestroy() {

    }

    private companion object {
        @JvmStatic
        private var initialized = false
    }
}
