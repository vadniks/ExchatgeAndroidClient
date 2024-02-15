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
import org.exchatge.model.net.Net
import org.exchatge.model.net.NetInitiator
import org.exchatge.presenter.PresenterImpl
import org.exchatge.presenter.PresenterInitiator
import java.util.concurrent.ConcurrentLinkedQueue

class Kernel(val context: Context) {
    val crypto = Crypto()
    @Volatile var net: Net? = null; private set
    val presenter = PresenterImpl(PresenterInitiatorImpl())
    private val pendingNetRequests = ConcurrentLinkedQueue<() -> Unit>()

    // TODO: add settings to ui to adjust options which will be stored as sharedPreferences

    init {
        assert(!initialized)
        initialized = true
    }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show().also { log(text) } // TODO: debug only

    fun initializeNet() {
        assert(net == null)
        net = Net(NetInitiatorImpl())
    }

    private inner class PresenterInitiatorImpl : PresenterInitiator {
        override fun onActivityCreate() {}

        override fun logIn(username: String, password: String) { // TODO: encrypt credentials in place
            assert(net == null)
            pendingNetRequests.add { runAsync { net?.logIn(username, password) } }
            initializeNet()
        }

        override fun onActivityDestroy() {}
    }

    private inner class NetInitiatorImpl : NetInitiator {
        override val context get() = this@Kernel.context
        override val crypto get() = this@Kernel.crypto

        override fun onConnectResult(successful: Boolean) {
            log("connected $successful")
            if (successful) while (!pendingNetRequests.isEmpty()) pendingNetRequests.poll()?.invoke()
        }

        override fun onNetDestroy() { net = null }

        override fun onLogInResult(successful: Boolean) { runInMain { toast("log in $successful") } }
    }

    private companion object {
        @JvmStatic
        private var initialized = false
    }
}
