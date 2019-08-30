package com.kynetics.uf.android.configuration

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.api.UFServiceCommunicationConstants
import com.kynetics.uf.android.communication.MessageHandler
import com.kynetics.uf.android.communication.MessengerHandler
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener

class AndroidMessageListener(private val service: UpdateFactoryService) : MessageListener {

    private val currentUpdateState = CurrentUpdateState(service.applicationContext)
    private val mNotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onMessage(message: MessageListener.Message) {
        when (message) {
            is MessageListener.Message.Event.UpdateFinished, is MessageListener.Message.State.CancellingUpdate -> {
                currentUpdateState.clearState()
                MessengerHandler.onAction(MessageHandler.Action.UPDATE_FINISH)
            }
        }

        MessengerHandler.onMessageReceived(message)
        MessengerHandler.sendMessage(UFServiceCommunicationConstants.MSG_SERVICE_STATUS)

        mNotificationManager.notify(UpdateFactoryService.NOTIFICATION_ID, service.getNotification(message.toString()))
        Log.i(TAG, message.toString())
    }

    companion object {
        val TAG: String = AndroidMessageListener::class.java.simpleName
    }
}