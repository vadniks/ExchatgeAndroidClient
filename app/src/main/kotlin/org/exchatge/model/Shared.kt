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
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("UnusedReceiverParameter") val Any.unit get() = Unit

fun assert(condition: Boolean) { if (!condition) throw IllegalStateException() }
fun assertNotMainThread() = assert(Looper.getMainLooper().thread !== Thread.currentThread())
fun log(vararg messages: Any?) = Log.d(null, messages.let { var s = ""; for (i in it) s += "$i "; s }).unit
val Context.kernel get() = (applicationContext as App).kernel
fun runInMain(action: () -> Unit) = Dispatchers.Main.dispatch(EmptyCoroutineContext) { action() }
fun runAsync(delay: Long = 0, action: () -> Unit) = Dispatchers.Default.dispatch(EmptyCoroutineContext) { Thread.sleep(delay); action() }
infix fun Int.untilSize(size: Int): IntRange { assert(size >= 0); return this until this + size } // TODO: replace all those

class Reference<T>(var referenced: T)
enum class Ternary { POSITIVE, NEUTRAL, NEGATIVE } // true, else, false
