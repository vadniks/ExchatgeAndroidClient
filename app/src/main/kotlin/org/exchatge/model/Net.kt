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
import java.util.logging.Logger

class Net(private val kernel: Kernel) {

    init {
        Logger.getLogger("a").info("net init " + Thread.currentThread().name)
        if (!NetService.running)
            kernel.context.startService(Intent(kernel.context, NetService::class.java))!!
    }

    fun listen() {
        val start = System.currentTimeMillis()
        while (NetService.running && System.currentTimeMillis() - start < 10000) {
            Logger.getLogger("a").info("net listen " + Thread.currentThread().name)
            Thread.sleep(1000)
        }
    }

    fun onDestroy() {

    }
}
