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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.locks.ReadWriteLock
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("UnusedReceiverParameter") val Any?.unit get() = Unit

@OptIn(ExperimentalCoroutinesApi::class)
private val backgroundDispatcher = Dispatchers.IO.limitedParallelism(1)

fun assert(condition: Boolean) { if (!condition) throw IllegalStateException() }

fun assertNotMainThread() = assert(Looper.getMainLooper().thread !== Thread.currentThread())

fun log(vararg messages: Any?) {
    var string = ""

    for ((j, i) in messages.withIndex()) {
        string += i
        if (j < messages.size - 1)
            string += ' '
    }

    Log.d(null, string)
}

val Context.kernel get() = (applicationContext as App).kernel

fun runInMain(action: () -> Unit) = Dispatchers.Main.dispatch(EmptyCoroutineContext) { action() }

fun runAsync(action: () -> Unit) = backgroundDispatcher.dispatch(EmptyCoroutineContext) { action() }

fun runAsync(delay: Long, action: () -> Unit) = backgroundDispatcher.dispatch(EmptyCoroutineContext) {
    Thread.sleep(delay)
    action()
}

fun <T> runDeferredAsync(thread: Threads, delay: Long = 0, action: () -> T): CompletableDeferred<T> {
    val deferred = CompletableDeferred<T>(null)
    (if (thread == Threads.Background) backgroundDispatcher else Dispatchers.Main)
        .dispatch(EmptyCoroutineContext) {
            if (delay > 0) Thread.sleep(delay)
            deferred.complete(action())
        }
    return deferred
}

infix fun Int.untilSize(size: Int): IntRange { // TODO: replace all those
    assert(size >= 0)
    return this until this + size
}

fun <T> ReadWriteLock.readLocked(action: () -> T): T =
    try {
        readLock().lock()
        action()
    } finally {
        readLock().unlock()
    }

fun <T> ReadWriteLock.writeLocked(action: () -> T): T =
    try {
        writeLock().lock()
        action()
    } finally {
        writeLock().unlock()
    }

class Reference<T>(var referenced: T)

enum class Ternary(val value: Boolean?) { POSITIVE(true), NEUTRAL(null), NEGATIVE(false) }

val Boolean?.ternary get() = when (this) { true -> Ternary.POSITIVE; null -> Ternary.NEUTRAL; false -> Ternary.NEGATIVE }

data class Options(val host: String, val port: Int, val sskp: String)

enum class Threads { Main, Background }
