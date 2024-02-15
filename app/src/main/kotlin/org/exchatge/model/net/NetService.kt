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

package org.exchatge.model.net

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.exchatge.model.assert
import org.exchatge.model.kernel
import org.exchatge.model.log

class NetService : Service() {
    private lateinit var listenJob: Job

    private val net get(): Net {
        if (kernel.net == null) kernel.initializeNet()
        return kernel.net!!
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        assert(!running)
        super.onCreate()

        running = true

        net.onCreate {
            running = false
            stopSelf()
        }

        listenJob = GlobalScope.launch(Dispatchers.IO) {
            net.onPostCreate()
            net.listen()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onBind(intent: Intent?) = null as IBinder?

    override fun onDestroy() {
        running = false
        runBlocking { listenJob.join() }
        net.onDestroy()
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        @Volatile // reads and writes to this field are atomic and writes are always made visible to other threads
        var running = false; private set
    }
}
