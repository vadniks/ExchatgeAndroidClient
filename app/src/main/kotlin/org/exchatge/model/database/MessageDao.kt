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

package org.exchatge.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.exchatge.model.database.Message.Companion.CONVERSATION
import org.exchatge.model.database.Message.Companion.MESSAGES
import org.exchatge.model.database.Message.Companion.TIMESTAMP

@Dao
interface MessageDao {

    @Insert
    fun add(message: Message)

    @Query("select * from $MESSAGES where $CONVERSATION = :$CONVERSATION order by $TIMESTAMP desc")
    fun getSeveral(conversation: Int): List<Message>

    @Query("delete from $MESSAGES where $CONVERSATION = :$CONVERSATION")
    fun removeSeveral(conversation: Int)

    @Query("select $TIMESTAMP from $MESSAGES where $CONVERSATION = :$CONVERSATION order by $TIMESTAMP desc limit 1")
    fun getMostRecentMessageTimestamp(conversation: Int): Long
}
