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

package org.exchatge.view

import androidx.activity.OnBackPressedCallback

interface View {
    fun addOnBackPressedCallback(callback: OnBackPressedCallback)
    fun finish()
    fun setShowSnackbarImpl(impl: suspend (String) -> Unit)
    fun snackbar(text: String)
    fun string(id: Int): String
    fun launchInLifecycleScope(action: suspend () -> Unit)
}

object ViewStub : View { // stub to make @Preview work
    override fun addOnBackPressedCallback(callback: OnBackPressedCallback) {}
    override fun finish() {}
    override fun setShowSnackbarImpl(impl: suspend (String) -> Unit) {}
    override fun snackbar(text: String) {}
    override fun string(id: Int) = ""
    override fun launchInLifecycleScope(action: suspend () -> Unit) {}
}
