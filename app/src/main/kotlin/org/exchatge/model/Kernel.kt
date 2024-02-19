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

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings.Secure
import androidx.annotation.VisibleForTesting
import org.exchatge.model.net.Net
import org.exchatge.model.net.NetInitiator
import org.exchatge.model.net.UNHASHED_PASSWORD_SIZE
import org.exchatge.model.net.USERNAME_SIZE
import org.exchatge.model.net.UserInfo
import org.exchatge.model.net.bytes
import org.exchatge.presenter.PresenterImpl
import org.exchatge.presenter.PresenterInitiator

class Kernel(val context: Context) {
    val crypto = Crypto()
    @Volatile var net: Net? = null; private set
    val presenter = PresenterImpl(PresenterInitiatorImpl())
    private val sharedPrefs = sharedPreferences(this::class.simpleName!!)
    private val encryptionKey: ByteArray
    private val wasLoggedIn get() = sharedPrefs.getString(CREDENTIALS, null) != null

    // TODO: add settings to ui to adjust options which will be stored as sharedPreferences

    init {
        assert(!initialized)
        initialized = true

        @SuppressLint("HardwareIds")
        encryptionKey = crypto.makeKey((
            Secure.getString(context.contentResolver, Secure.ANDROID_ID).hashCode()
            xor this::class.qualifiedName!!.hashCode()
        ).bytes)
    }

    private fun initializeNet() {
        assert(net == null)
        net = Net(NetInitiatorImpl())
    }

    fun sharedPreferences(name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun decryptCredentials(encrypted: String): Pair<String, String>? {
        val decrypted =
            crypto.decryptSingle(encryptionKey, crypto.hexDecode(encrypted) ?: return null)
            ?: return null

        return (
            String(decrypted.sliceArray(0 until USERNAME_SIZE))
            to String(decrypted.sliceArray(USERNAME_SIZE until USERNAME_SIZE + UNHASHED_PASSWORD_SIZE))
        )
    }

    private fun credentials() = sharedPrefs.getString(CREDENTIALS, null).let { if (it != null) decryptCredentials(it) else null }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun encryptCredentials(credentials: Pair<String, String>): String {
        val combined = ByteArray(USERNAME_SIZE + UNHASHED_PASSWORD_SIZE)
        credentials.first.toByteArray().let { System.arraycopy(it, 0, combined, 0, it.size) }
        credentials.second.toByteArray().let { System.arraycopy(it, 0, combined, USERNAME_SIZE, it.size) }

        val encrypted = crypto.encryptSingle(encryptionKey, combined)
        return crypto.hexEncode(encrypted!!)
    }

    @SuppressLint("ApplySharedPref")
    private fun setCredentials(credentials: Pair<String, String>?) = sharedPrefs.edit().apply {
        if (credentials == null) remove(CREDENTIALS) else putString(CREDENTIALS, encryptCredentials(credentials))
    }.commit() // TODO: fill with mess before drop

    private inner class PresenterInitiatorImpl : PresenterInitiator {
        @Volatile private var triedLogIn = false
        override val currentUserId get() = net!!.userId

        override fun onActivityCreate() {}

        override fun credentialsLengthCorrect(username: String, password: String) =
            username.length in 1..USERNAME_SIZE && password.length in 1..UNHASHED_PASSWORD_SIZE

        override fun scheduleLogIn() {
            triedLogIn = true
            runAsync(1000) { initializeNet() }
        }

        override fun scheduleUsersFetch() {
            assert(net != null)
            runAsync { net!!.fetchUsers() }
        }

        override fun admin(id: Int) = id == 0

        override fun scheduleLogOut() = runAsync {
            setCredentials(null)
            net!!.disconnect()
        }

        override fun onConversationSetupDialogAction(accepted: Boolean) {
            if (!accepted) presenter.hideConversationSetupDialog()
        }

        override fun onActivityResume() =
            if (!wasLoggedIn || triedLogIn || net != null) false
            else { scheduleLogIn(); true }

        override fun onActivityDestroy() {}
    }

    private inner class NetInitiatorImpl : NetInitiator {
        override val context get() = this@Kernel.context
        override val crypto get() = this@Kernel.crypto

        override fun onConnectResult(successful: Boolean) {
            if (successful) {
                val (username, password) = credentials() ?: presenter.credentials
                net!!.logIn(username, password)
            } else
                presenter.onConnectFail()
        }

        override fun onNetDestroy() {
            net = null
            presenter.onDisconnected()
        }

        override fun onLogInResult(successful: Boolean) {
            if (successful) { if (!wasLoggedIn) setCredentials(presenter.credentials) }
            else { if (wasLoggedIn) setCredentials(null) }

            presenter.onLogInResult(successful)
        }

        override fun onNextUserFetched(user: UserInfo, last: Boolean) = presenter.onNextUserFetched(user, last).also {
            // TODO: debug only
            if (!last) return@also
            runAsync(5000) {
                log("k onuf")
                presenter.showConversationSetUpDialog(false, 1, "user1")
//                val r = net!!.createConversation(1)
//                log("k onuf $r")
            }
        }

        override fun onConversationSetUpInviteReceived(fromId: Int) = runAsync {
            log("k ocsuir $fromId")
//            val r = net!!.replyToConversationSetUpInvite(false, fromId) // TODO: debug only
//            log("k ocsuir $r")
        }
    }

    private companion object {
        @JvmStatic
        private var initialized = false

        private const val CREDENTIALS = "credentials"
    }
}
