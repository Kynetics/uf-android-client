/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.configuration

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.communication.messenger.MessageHandler
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.update.CurrentUpdateState
import org.eclipse.hara.ddiclient.api.MessageListener

class AndroidMessageListener(private val service: UpdateFactoryService) : MessageListener {

    private val currentUpdateState = CurrentUpdateState(service.applicationContext)
    private val mNotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onMessage(message: MessageListener.Message) {
        Log.i(TAG, "Message received from Hara: $message")
        var shouldBroadcastTheMessage = true
        when (message) {
            is MessageListener.Message.Event.UpdateFinished,
            is MessageListener.Message.State.CancellingUpdate -> {
                currentUpdateState.clearState()
                MessengerHandler.onAction(MessageHandler.Action.UPDATE_FINISH)
            }
            is MessageListener.Message.Event.UpdateAvailable -> currentUpdateState.setCurrentUpdateId(message.id)
            is MessageListener.Message.Event.AllFilesDownloaded -> currentUpdateState.allFileDownloaded()
            is MessageListener.Message.Event.DeployFeedbackRequestResult -> {
                shouldBroadcastTheMessage = false
            }
            else -> {}
        }

        if (shouldBroadcastTheMessage) {
            MessengerHandler.notifyHaraMessage(message)
        }

        MessengerHandler.notifyInternalChannel(message)
        mNotificationManager.notify(UpdateFactoryService.NOTIFICATION_ID, service.getNotification(message.toString()))
    }

    companion object {
        val TAG: String = AndroidMessageListener::class.java.simpleName
    }
}
