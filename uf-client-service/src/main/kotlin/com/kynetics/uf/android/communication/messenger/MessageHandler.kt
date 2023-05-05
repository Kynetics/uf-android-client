/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
@file:Suppress("DEPRECATION")

package com.kynetics.uf.android.communication.messenger

import android.util.Log
import com.kynetics.uf.android.api.UFServiceMessage
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.android.converter.toUFMessage
import org.eclipse.hara.ddiclient.api.MessageListener
import java.io.Serializable

interface MessageHandler<out T : Serializable?> {

    enum class Action {
        FORCE_PING,
        AUTH_DOWNLOAD_DENIED,
        AUTH_DOWNLOAD_GRANTED,
        AUTH_UPDATE_DENIED,
        AUTH_UPDATE_GRANTED,
        UPDATE_FINISH
    }

    val messageToSendOnSync: T
    val currentMessage: T

    fun hasMessage(): Boolean = messageToSendOnSync != null

    fun onAction(action: Action): MessageHandler<T>? {
        return this
    }

    fun onMessage(msg: MessageListener.Message): MessageHandler<T>? {
        return this
    }

    fun onAndroidMessage(msg: UFServiceMessageV1): MessageHandler<T>? {
        return this
    }

    fun onConfigurationError(details: List<String>): MessageHandler<T>?
}

data class V0(
    override val currentMessage: UFServiceMessage? = null,
    private val suspend: UFServiceMessage.Suspend = UFServiceMessage.Suspend.NONE
) : MessageHandler<UFServiceMessage?> {
    override val messageToSendOnSync: UFServiceMessage? = currentMessage

    override fun onAction(action: MessageHandler.Action): MessageHandler<UFServiceMessage?> {
        return when (action) {
            MessageHandler.Action.AUTH_DOWNLOAD_DENIED -> copy(suspend = UFServiceMessage.Suspend.DOWNLOAD)
            MessageHandler.Action.AUTH_UPDATE_DENIED -> copy(suspend = UFServiceMessage.Suspend.UPDATE)
            else -> copy(suspend = UFServiceMessage.Suspend.NONE)
        }
    }

    override fun onMessage(msg: MessageListener.Message): MessageHandler<UFServiceMessage?> {
        val newSuspendValue = when(msg){
            is MessageListener.Message.State.WaitingDownloadAuthorization -> UFServiceMessage.Suspend.DOWNLOAD
            is MessageListener.Message.State.WaitingUpdateAuthorization -> UFServiceMessage.Suspend.UPDATE
            else -> suspend
        }
        return copy(currentMessage = UFServiceMessage("", "", msg.toString(), newSuspendValue))
    }

    override fun onAndroidMessage(msg: UFServiceMessageV1): MessageHandler<UFServiceMessage?> {
        val newSuspendValue = when(msg){
            is UFServiceMessageV1.State.WaitingDownloadAuthorization -> UFServiceMessage.Suspend.DOWNLOAD
            is UFServiceMessageV1.State.WaitingUpdateAuthorization -> UFServiceMessage.Suspend.UPDATE
            else -> suspend
        }
        return copy(currentMessage = UFServiceMessage("", "", msg.toString(), newSuspendValue))
    }

    override fun onConfigurationError(details: List<String>): MessageHandler<UFServiceMessage?> {
        val state = UFServiceMessageV1.State.ConfigurationError(details).toString()
        return copy(currentMessage = UFServiceMessage("", "", state, suspend))
    }
}

data class V1x(
    override val messageToSendOnSync: String? = null,
    override val currentMessage: String? = null,
    val msgMapper: (UFServiceMessageV1) -> UFServiceMessageV1? = {msg -> msg}
) : MessageHandler<String?> {

    override fun onMessage(msg: MessageListener.Message): MessageHandler<String?>? {
        return onAndroidMessage(msg.toUFMessage())
    }

    override fun onConfigurationError(details: List<String>): MessageHandler<String?> {
        val state = UFServiceMessageV1.State.ConfigurationError(details).toJson()
        return copy(messageToSendOnSync = state, currentMessage = state)
    }

    override fun onAndroidMessage(msg: UFServiceMessageV1): MessageHandler<String?>? {
        Log.i("v1", "onAndroidMessage $msg")
        return when (val finalMsg = msgMapper(msg)) {
            is UFServiceMessageV1.Event -> {
                copy(currentMessage = finalMsg.toJson())
            }

            is UFServiceMessageV1.State -> {
                val currentMessage = finalMsg.toJson()
                copy(messageToSendOnSync = currentMessage, currentMessage = currentMessage)
            }

            null -> null
        }
    }
}

object MessageHandlerFactory{
    fun newV0(): V0 = V0()

    fun newV1():V1x = V1x(
        msgMapper = { msg:UFServiceMessageV1 ->
            when(msg){
                is UFServiceMessageV1.State.WaitingUpdateWindow -> UFServiceMessageV1.State.WaitingUpdateAuthorization
                is UFServiceMessageV1.Event.Stopped, is UFServiceMessageV1.Event.Started, is UFServiceMessageV1.Event.CantBeStopped, is UFServiceMessageV1.Event.ConfigurationUpdated, is UFServiceMessageV1.Event.NewTargetTokenReceived -> null
                else -> msg
            }.also { newMsg -> Log.i("v1", "mapping $msg to $newMsg") }
        }
    )

    @Suppress("FunctionName")
    fun newV1_1():V1x = V1x(
        msgMapper = { msg:UFServiceMessageV1 ->
            when(msg){
                is UFServiceMessageV1.Event.Stopped, is UFServiceMessageV1.Event.Started, is UFServiceMessageV1.Event.CantBeStopped, is UFServiceMessageV1.Event.ConfigurationUpdated, is UFServiceMessageV1.Event.NewTargetTokenReceived -> null
                else -> msg
            }.also { newMsg -> Log.i("v1.1", "mapping $msg to $newMsg") }
        }
    )

    @Suppress("FunctionName")
    fun newV1_2():V1x = V1x()
}