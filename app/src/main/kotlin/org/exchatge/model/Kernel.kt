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
import android.widget.Toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.exchatge.model.database.Database
import org.exchatge.model.net.Net
import org.exchatge.presenter.ActivityPresenter
import java.util.concurrent.ConcurrentLinkedQueue

@OptIn(DelicateCoroutinesApi::class)
class Kernel(private val contextGetter: () -> Context) {
    val context get() = contextGetter() // getters are used instead of the object itself as the storing context smwhr is a memory leak
    val crypto = Crypto()
    val database = Database.init(context)
    val net = Net(this)
    val presenter = ActivityPresenter(this)

    private val asyncActionsQueue = ConcurrentLinkedQueue<() -> Unit>()
    private val asyncActionsJob: Job

    // TODO: add asyncActionsThread
    // TODO: add settings to ui to adjust options which will be stored as sharedPreferences

    init {
        assert(!initialized)
        initialized = true

        asyncActionsJob = GlobalScope.launch {
            while (net.running)
                asyncActionsQueue.poll()?.invoke()
        }
    }

    fun async(action: () -> Unit) = asyncActionsQueue.add(action)

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show().also { log(text) } // TODO: debug only

    fun onActivityCreate() {
        net.startService()
    }

    fun onActivityDestroy() {
        database.close()
    }

    fun onNetDestroy() {
        runBlocking {
            asyncActionsJob.join()
        }
        database.close()
    }

    private companion object {
        @JvmStatic
        private var initialized = false
    }
}
