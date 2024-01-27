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

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

class NetService : Service() {
    private lateinit var listenJob: Job
    private val kernel get() = (application as App).kernel

    override fun onCreate() {
        super.onCreate()
        xRunning.set(true)

        Logger.getLogger("a").info("net service create 1 " + Thread.currentThread().name)
        listenJob = GlobalScope.launch(Dispatchers.Default) {
            Logger.getLogger("a").info("net service create job 1 " + Thread.currentThread().name)
            kernel.net.listen()
            Logger.getLogger("a").info("net service create job 2 " + Thread.currentThread().name)
            listenJob.cancel()
            stopSelf()
        }
        Logger.getLogger("a").info("net service create 2 " + Thread.currentThread().name)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onBind(intent: Intent?) = null as IBinder?

    override fun onDestroy() {
        super.onDestroy()
        xRunning.set(false)

        Logger.getLogger("a").info("net service destroy 1 " + Thread.currentThread().name)
        runBlocking {
            listenJob.join()
        }
        Logger.getLogger("a").info("net service destroy 2 " + Thread.currentThread().name)

        kernel.net.onDestroy()
    }

    companion object {
        @JvmStatic
        private val xRunning = AtomicBoolean(false)

        @JvmStatic
        val running get() = xRunning.get()
    }
}
