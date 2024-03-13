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

import org.exchatge.model.Options
import org.exchatge.model.database.Message

interface PresenterInitiator {
    val currentUserId: Int
    val maxMessagePlainPayloadSize: Int
    val maxBroadcastMessageSize: Int

    fun onActivityCreate()
    fun credentialsLengthCorrect(username: String, password: String): Boolean
    fun scheduleLogIn()
    fun scheduleRegister()
    fun scheduleUsersFetch()
    fun admin(id: Int): Boolean
    fun scheduleLogOut()
    fun onConversationSetupDialogAction(accepted: Boolean, requestedByHost: Boolean, opponentId: Int)
    fun shutdownServer()
    fun sendBroadcast(text: String)
    fun onConversationRequested(id: Int, remove: Boolean)
    fun sendMessage(to: Int, text: String, millis: Long)
    fun loadSavedMessages(conversation: Int): List<Message>
    fun username(id: Int): String?
    fun saveOptions(host: String, port: Int, sskp: String)
    fun loadOptions(): Options
    fun onActivityResume(): Boolean
    fun onActivityDestroy()
}
