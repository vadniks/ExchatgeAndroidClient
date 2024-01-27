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

import android.content.Context
import org.exchatge.presenter.ActivityPresenter

class Kernel(private val contextGetter: () -> Context) {
    val context get() = contextGetter() // getters are used instead of the object itself as the storing context smwhr is a memory leak
    val net = Net(this)
    val crypto = Crypto() // TODO: init the crypto only if the user has logged in
    val presenter = ActivityPresenter(this)

    init {
        assert(!initialized)
        initialized = true
    }

    private companion object {
        @JvmStatic
        private var initialized = false
    }
}
