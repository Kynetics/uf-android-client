/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.clientexample.data

import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import java.text.DateFormat
import java.text.NumberFormat
import java.util.*

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 * TODO: Replace all uses of this class before publishing your app.
 */

fun Long.toDate(): String {
    val sdf = DateFormat.getDateTimeInstance()
    return sdf.format(Date(this))
}

fun Double.format(minFractionDigits: Int = 0): String {
    val format = NumberFormat.getNumberInstance()
    format.maximumFractionDigits = minFractionDigits
    format.minimumFractionDigits = minFractionDigits
    return format.format(this)
}

fun Double.percentFormat(): String {
    val format = NumberFormat.getPercentInstance()
    format.maximumFractionDigits = 2
    format.minimumFractionDigits = 2
    return format.format(this)
}

object MessageHistory {
    const val CAPACITY = 100
    /**
     * An array of sample (dummy) items.
     */
    val ITEMS: MutableList<StateEntry> = ArrayList(CAPACITY)

    /**
     * A map of sample (dummy) items, by ID.
     */
    val ITEM_MAP: MutableMap<Long, StateEntry> = HashMap(CAPACITY)

    /**
     * A dummy item representing a piece of content.
     */

    data class StateEntry(
        val id: Long = System.currentTimeMillis(),
        val state: UFServiceMessageV1.State,
        val events: MutableList<EventEntry> = ArrayList(CAPACITY),
        var unread: Int = 0
    ) {

        fun addEvent(item: UFServiceMessageV1.Event) {
            if (events.size == CAPACITY) {
                events.removeAt(CAPACITY - 1)
            }
            events.add(0,
                EventEntry(
                    System.currentTimeMillis().toDate(),
                    item
                )
            )
            unread++
        }

        fun printDate(): String {
            return id.toDate()
        }
    }

    data class EventEntry(val date: String, val event: UFServiceMessageV1.Event) {
        override fun toString(): String {
            val baseMessage = "$date - ${event.name}"
            return when (event) {
                is UFServiceMessageV1.Event.StartDownloadFile -> print(baseMessage, event)
                is UFServiceMessageV1.Event.FileDownloaded -> print(baseMessage, event)
                is UFServiceMessageV1.Event.UpdateFinished -> print(baseMessage, event)
                is UFServiceMessageV1.Event.Error -> print(baseMessage, event)
                is UFServiceMessageV1.Event.DownloadProgress -> print(baseMessage, event)
                is UFServiceMessageV1.Event.UpdateProgress -> print(baseMessage, event)
                else -> return baseMessage
            }
        }

        private fun print(infix: String, event: UFServiceMessageV1.Event.StartDownloadFile): String =
            "$infix \nFile Name: ${event.fileName}"
        private fun print(infix: String, event: UFServiceMessageV1.Event.FileDownloaded): String =
            "$infix \nFile Name: ${event.fileDownloaded}"
        private fun print(infix: String, event: UFServiceMessageV1.Event.UpdateFinished): String =
            "$infix \nUpdate Result: ${if (event.successApply) "applied" else "not applied" }\n" +
                event.details.joinToString("\n")
        private fun print(infix: String, event: UFServiceMessageV1.Event.Error): String =
            "$infix \n${event.details.joinToString("\n")}"
        private fun print(infix: String, event: UFServiceMessageV1.Event.DownloadProgress): String =
            "$infix \n${event.fileName} is downloaded at ${event.percentage.percentFormat()}"
        private fun print(infix: String, event: UFServiceMessageV1.Event.UpdateProgress): String =
            "$infix  \nPhase name: ${event.phaseName} ${if(event.percentage.isNaN() || event.percentage == 0.0) "" else "is at ${event.percentage.percentFormat()}"}"
    }

    fun appendEvent(event: UFServiceMessageV1.Event): Boolean {
        return if (ITEMS.isNotEmpty()) {
            ITEMS.first().addEvent(event)
            true
        } else {
            false
        }
    }

    fun addState(item: StateEntry) {
        if (ITEMS.isNotEmpty() && item.state == ITEMS[0].state) {
            return
        }
        if (ITEMS.size == CAPACITY) {
            val itemToRemove = ITEMS.removeAt(CAPACITY - 1)
            ITEM_MAP.remove(itemToRemove.id)
        }
        ITEMS.add(0, item)
        ITEM_MAP[item.id] = item
    }
}
