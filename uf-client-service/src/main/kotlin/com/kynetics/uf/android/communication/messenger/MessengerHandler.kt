/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.communication.messenger

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.android.converter.toUFMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.eclipse.hara.ddiclient.api.MessageListener
import java.io.Serializable

object MessengerHandler {

    private val TAG = MessengerHandler::class.java.simpleName
    private const val CHANNEL_BUFFER = 10

    private val lastSharedMessagesByVersion = mutableMapOf(
            ApiCommunicationVersion.V0_1 to MessageHandlerFactory.newV0(),
            ApiCommunicationVersion.V1 to MessageHandlerFactory.newV1(),
            ApiCommunicationVersion.V1_1 to MessageHandlerFactory.newV1_1(),
            ApiCommunicationVersion.V1_2 to MessageHandlerFactory.newV1_2(),
    )

    private val mClients = mutableMapOf<Messenger, ApiCommunicationVersion>()
    val internalChannel: Channel<UFServiceMessageV1> = Channel(
        CHANNEL_BUFFER, BufferOverflow.DROP_OLDEST
    )
    fun getlastSharedMessage(version: ApiCommunicationVersion) = lastSharedMessagesByVersion.getValue(version)

    fun hasMessage(version: ApiCommunicationVersion): Boolean {
        return lastSharedMessagesByVersion.getValue(version).hasMessage()
    }

    fun onAction(action: MessageHandler.Action) = updateMessage { mh -> mh.onAction(action) }

    fun notifyHaraMessage(msg: MessageListener.Message) = updateMessageAndNotify { mh -> mh.onMessage(msg) }

    fun notifyConfigurationError(details: List<String>) = updateMessageAndNotify{ mh -> mh.onConfigurationError(details)}

    fun notifyMessage(msg: UFServiceMessageV1) = updateMessageAndNotify { mh -> mh.onAndroidMessage( msg) }

    private fun updateMessage(map: (MessageHandler<Serializable?>) -> MessageHandler<Serializable?>?) = lastSharedMessagesByVersion.forEach {
        map(it.value)?.let{ newMessage ->  lastSharedMessagesByVersion[it.key] = newMessage }
    }

    fun notifyInternalChannel(msg: MessageListener.Message) {
        internalChannel.trySend(msg.toUFMessage())
    }

    private fun updateMessageAndNotify(map: (MessageHandler<Serializable?>) -> MessageHandler<Serializable?>?) =
        updateMessage(map).also { sendBroadcastMessage(Communication.V1.Out.ServiceNotification.ID) }

    internal fun response(messageContent: Serializable?, code: Int, replyTo: Messenger?) {
        if (replyTo == null) {
            Log.i(TAG, "Response isn't' sent because there isn't a receiver (replyTo is null)")
            return
        }
        val message = getMessage(messageContent, code)
        try {
            replyTo.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    internal fun sendBroadcastMessage(messageCode: Int, message: Serializable? = null) {
        mClients.keys.filter { hasMessage(mClients.getValue(it)) }
                .forEach { messenger ->
                    try {
                        val apiCommunicationVersion = mClients.getValue(messenger)
                        messenger.send(
                                getMessage(
                                        message
                                        ?: lastSharedMessagesByVersion.getValue(
                                            apiCommunicationVersion).currentMessage,
                                    messageCode)
                        )
                    } catch (e: RemoteException) {
                        mClients.remove(messenger)
                    }
                }
    }

    internal fun subscribeClient(messenger: Messenger?, apiVersion: ApiCommunicationVersion) {
        if (messenger != null) {
            mClients[messenger] = apiVersion
            Log.i(TAG, "client subscription")
        } else {
            Log.i(TAG, "client subscription ignored. Field replyTo mustn't be null")
        }
    }

    internal fun unsubscribeClient(messenger: Messenger?) {
        if (messenger != null) {
            mClients.remove(messenger)
            Log.i(TAG, "client unsubscription")
        } else {
            Log.i(TAG, "client unsubscription ignored. Field replyTo mustn't be null")
        }
    }
    private fun getMessage(messageContent: Serializable?, messageCode: Int): Message {
        val message = Message.obtain(null, messageCode)
        val data = Bundle()
        data.putSerializable(Communication.V1.SERVICE_DATA_KEY, messageContent)
        message.data = data
        return message
    }
}
