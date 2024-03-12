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

package org.exchatge.presenter

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.exchatge.R
import org.exchatge.model.kernel
import org.exchatge.model.log
import org.exchatge.model.net.UserInfo
import org.exchatge.model.readLocked
import org.exchatge.model.runAsync
import org.exchatge.model.unit
import org.exchatge.model.writeLocked
import org.exchatge.view.Activity
import org.exchatge.view.ConversationMessage
import org.exchatge.view.ConversationSetupDialogParameters
import org.exchatge.view.User
import org.exchatge.view.View
import org.exchatge.view.pages.Pages
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.reflect.KProperty

class PresenterImpl(private val initiator: PresenterInitiator): Presenter {
    @Volatile override var view: View? = null; private set
    val activityRunning get() = view != null
    private val rwLock = ReentrantReadWriteLock()
    override var currentPage by SynchronizedMutableState(Pages.LOG_IN_REGISTER)
    override var controlsEnabled by SynchronizedMutableState(true)
    override var loading by SynchronizedMutableState(false)
    override var username by mutableStateOf("") // TODO: synchronize ui string fields too?
    override var password by mutableStateOf("")
    override var host by mutableStateOf("")
    override var port by mutableIntStateOf(8080)
    override var sskp by mutableStateOf("")
    private val users = SynchronizedMutableStateList<User>() as MutableList<User>
    val credentials get() = username to password
    override var currentUser by SynchronizedMutableState("")
    override var admin by SynchronizedMutableState(false)
    override var broadcastMessage by mutableStateOf("")
    override val maxBroadcastMessageSize = initiator.maxBroadcastMessageSize - 1
    override var opponentUsername by SynchronizedMutableState("")
    override var currentConversationMessage by mutableStateOf("")
    override var conversationSetupDialogParameters by SynchronizedMutableState<ConversationSetupDialogParameters?>(null)
    override var showAdministrativeActions by SynchronizedMutableState(false)
    private val messages = SynchronizedMutableStateList<ConversationMessage>()
    @Volatile private var opponentId = 0
    override val maxMessageTextSize get() = initiator.maxMessagePlainPayloadSize - 1

    init {
        assert(!initialized)
        initialized = true
    }

    override fun onCreate(view: View, savedInstanceState: Bundle?) {
        this.view = view

        view.addOnBackPressedCallback(object : OnBackPressedCallback(true)
        { override fun handleOnBackPressed() = this@PresenterImpl.handleOnBackPressed() })

        initiator.onActivityCreate()
        setUiLock(true)
    }

    private fun handleOnBackPressed() {
        if (!controlsEnabled) return
        when (currentPage) {
            Pages.CONVERSATION -> currentPage = Pages.USERS_LIST
            Pages.USERS_LIST -> {
                setUiLock(true)
                initiator.scheduleLogOut()
            }
            Pages.SETTINGS -> currentPage = Pages.LOG_IN_REGISTER
            Pages.LOG_IN_REGISTER -> view!!.finish()
        }
    }

    private fun setUiLock(lock: Boolean) {
        controlsEnabled = !lock
        loading = lock
    }

    override fun onResume() = setUiLock(initiator.onActivityResume())

    override fun onDestroy() {
        view = null
        initiator.onActivityDestroy()
    }

    override fun logIn() {
        if (!initiator.credentialsLengthCorrect(username, password)) {
            view!!.snackbar(view!!.string(R.string.incorrectCredentialsLength))
            return
        }

        setUiLock(true)
        initiator.scheduleLogIn()
    }

    fun onConnectFail() {
        if (!activityRunning) return
        view!!.snackbar(view!!.string(R.string.failedToConnect))
        setUiLock(false)
    }

    fun onLogInResult(successful: Boolean) {
        if (!activityRunning) return
        view!!.snackbar(view!!.string(if (successful) R.string.loggedInSuccessfully else R.string.failedToLogIn))

        if (successful) {
            username = ""; password = "" // TODO: fill with mess before drop
            users.clear()
            currentPage = Pages.USERS_LIST
            initiator.scheduleUsersFetch()
        } else
            setUiLock(false)
    }

    fun onNextUserFetched(userInfo: UserInfo, conversationExists: Boolean, last: Boolean) {
        if (userInfo.id == initiator.currentUserId) {
            currentUser = String(userInfo.name) // TODO: trim trailing zeroes
            admin = initiator.admin(userInfo.id)
        } else
            users.add(User(userInfo.id, String(userInfo.name), userInfo.connected, conversationExists))
    }

    fun onMessagesFetched(empty: Boolean) {
        setUiLock(false)
    }

    fun onErrorReceived() {
        if (!activityRunning) return
    }

    fun onDisconnected() {
        if (!activityRunning) return

        currentPage = Pages.LOG_IN_REGISTER
        setUiLock(false)
        view!!.snackbar(view!!.string(R.string.disconnected))
    }

    override fun register() {
        setUiLock(true)
        initiator.scheduleRegister()
    }

    override fun settings() {
        setUiLock(true)
        initiator.loadOptions().let {
            host = it.host
            port = it.port
            sskp = it.sskp
        }
        currentPage = Pages.SETTINGS
        setUiLock(false)
    }

    override fun applySettings() = runAsync {
        fun parseSskp(): ByteArray? {
            val splitted = sskp.split(' ')
            var matches = splitted.isNotEmpty()

            val bytes = ByteArray(splitted.size)
            var j = 0

            for (i in splitted) {
                matches = matches && i.toIntOrNull().let { it != null && it in 0..255 }
                if (matches) bytes[j++] = i.toInt().toByte()
                else return null
            }
            return bytes
        }

        val sskp = parseSskp()
        if (sskp == null) {
            view?.snackbar(view?.string(R.string.wrongFormatting) ?: return@runAsync)
            return@runAsync
        }

        initiator.saveOptions(host, port, this.sskp)
    }

    fun onRegisterResult(successful: Boolean) {
        if (!activityRunning) return
        setUiLock(false)
        view!!.snackbar(view!!.string(if (successful) R.string.registrationSucceeded else R.string.registrationFailed))
    }

    override fun updateUsersList() {
        setUiLock(true)
        users.clear()
        initiator.scheduleUsersFetch()
    }

    override fun administrate(done: Boolean) = this::showAdministrativeActions.set(!done)

    override fun shutdownServer() = initiator.shutdownServer()

    override fun sendBroadcast() {
        if (broadcastMessage.isEmpty()) return
        initiator.sendBroadcast(broadcastMessage)
        broadcastMessage = ""
    }

    override fun usersForEach(action: (User) -> Unit) = rwLock.readLocked { for (i in users) action(i) }

    override fun conversation(id: Int, remove: Boolean) = initiator.onConversationRequested(id, remove)

    override fun messagesForEach(action: (ConversationMessage) -> Unit) = rwLock.readLocked { for (i in messages) action(i) }

    override fun returnFromPage() = handleOnBackPressed()

    override fun fileChoose() {}

    override fun sendMessage() = System.currentTimeMillis().let {
        if (currentConversationMessage.isEmpty()) return@let

        messages.add(0, ConversationMessage(it, null, currentConversationMessage))
        initiator.sendMessage(opponentId, currentConversationMessage, it)
        currentConversationMessage = ""
    }

    fun showConversationSetUpDialog(requestedByHost: Boolean, opponentId: Int, opponentName: String) =
        this::conversationSetupDialogParameters.set(ConversationSetupDialogParameters(
            requestedByHost, opponentId, opponentName
        ) { initiator.onConversationSetupDialogAction(it, requestedByHost, opponentId) })

    fun hideConversationSetupDialog() = this::conversationSetupDialogParameters.set(null)

    fun onMessageReceived(from: Int, timestamp: Long, text: String) {
        if (from == opponentId)
            messages.add(0, ConversationMessage(timestamp, opponentUsername, text))
    }

    fun onSettingUpConversation(result: Boolean? = null) {
        if (!activityRunning) return
        setUiLock(result == null)

        view!!.snackbar(
            view!!.string(R.string.conversationSetup) + ' '
            + view!!.string(if (result ?: return) R.string.succeeded else R.string.failed)
        )

        if (result == true) updateUsersList()
    }

    fun notifyUserOpponentIsOffline() =
        if (activityRunning) view!!.snackbar(view!!.string(R.string.opponentIsOffline)).unit else Unit

    fun removeConversation(done: Boolean?) {
        if (!activityRunning) return
        setUiLock(done == null)

        if (done == true) {
            updateUsersList()
            view!!.snackbar(view!!.string(R.string.conversationWasDeleted))
        } else
            view!!.snackbar(view!!.string(R.string.conversationDoesntExist))
    }

    fun showConversation(id: Int, username: String) {
        currentPage = Pages.CONVERSATION
        opponentUsername = username
        opponentId = id

        setUiLock(true)
        runAsync {
            messages.clear()

            for (i in initiator.loadSavedMessages(id))
                messages.add(ConversationMessage(
                    i.timestamp,
                    if (i.from == initiator.currentUserId) null else initiator.username(i.from)!!,
                    String(i.text)
                ))

            setUiLock(false)
        }
    }

    fun onBroadcastReceived(text: String) = if (activityRunning) view!!.snackbar(text) else Unit

    private inner class SynchronizedMutableState<T>(initial: T) {
        private val delegate = mutableStateOf(initial)
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = rwLock.readLocked { delegate.getValue(thisRef, property) }
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = rwLock.writeLocked { delegate.setValue(thisRef, property, value) }
    }

    private inner class SynchronizedMutableStateList<T>: MutableList<T> {
        private val delegate = mutableStateListOf<T>()
        override val size = rwLock.readLocked { delegate.size }
        override fun clear() = rwLock.writeLocked { delegate.clear() }
        override fun addAll(elements: Collection<T>) = rwLock.writeLocked { delegate.addAll(elements) }
        override fun addAll(index: Int, elements: Collection<T>) = rwLock.writeLocked { delegate.addAll(index, elements) }
        override fun add(index: Int, element: T) = rwLock.writeLocked { delegate.add(index, element) }
        override fun add(element: T) = rwLock.writeLocked { delegate.add(element) }
        override fun get(index: Int) = rwLock.readLocked { delegate[index] }
        override fun isEmpty() = rwLock.readLocked { delegate.isEmpty() }
        override fun iterator() = object : MutableIterator<T> {
            private val delegate2 = delegate.iterator()
            override fun hasNext() = rwLock.readLocked { delegate2.hasNext() }
            override fun next() = rwLock.readLocked { delegate2.next() }
            override fun remove() = rwLock.writeLocked { delegate2.remove() }
        }
        private inner class ListIteratorWrapper(index: Int) : MutableListIterator<T> {
            private val delegate2 = if (index >= 0) delegate.listIterator(index) else delegate.listIterator()
            override fun add(element: T) = rwLock.writeLocked { delegate2.add(element) }
            override fun hasNext() = rwLock.readLocked { delegate2.hasNext() }
            override fun hasPrevious() = rwLock.readLocked { delegate2.hasPrevious() }
            override fun next() = rwLock.readLocked { delegate2.next() }
            override fun nextIndex() = rwLock.readLocked { delegate2.nextIndex() }
            override fun previous() = rwLock.readLocked { delegate2.previous() }
            override fun previousIndex() = rwLock.readLocked { delegate2.previousIndex() }
            override fun remove() = rwLock.writeLocked { delegate2.remove() }
            override fun set(element: T) = rwLock.writeLocked { delegate2.set(element) }
        }
        override fun listIterator() = ListIteratorWrapper(-1)
        override fun listIterator(index: Int) = ListIteratorWrapper(index)
        override fun removeAt(index: Int) = rwLock.writeLocked { delegate.removeAt(index) }
        override fun subList(fromIndex: Int, toIndex: Int) = rwLock.readLocked { delegate.subList(fromIndex, toIndex) }
        override fun set(index: Int, element: T) = rwLock.writeLocked { delegate.set(index, element) }
        override fun retainAll(elements: Collection<T>) = rwLock.writeLocked { delegate.retainAll(elements) }
        override fun removeAll(elements: Collection<T>) = rwLock.writeLocked { delegate.removeAll(elements) }
        override fun remove(element: T) = rwLock.writeLocked { delegate.remove(element) }
        override fun lastIndexOf(element: T) = rwLock.readLocked { delegate.lastIndexOf(element) }
        override fun indexOf(element: T) = rwLock.readLocked { delegate.indexOf(element) }
        override fun containsAll(elements: Collection<T>) = rwLock.readLocked { delegate.containsAll(elements) }
        override fun contains(element: T) = rwLock.readLocked { delegate.contains(element) }
    }

    companion object {
        @JvmStatic
        private var initialized = false

        @JvmStatic
        fun instance(activity: Activity, savedInstanceState: Bundle?) = activity
            .kernel
            .presenter
            .also { it.onCreate(activity, savedInstanceState) } as Presenter
    }
}
