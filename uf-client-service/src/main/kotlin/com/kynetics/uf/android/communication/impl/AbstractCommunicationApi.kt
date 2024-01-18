/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

@file:Suppress("DEPRECATION")

package com.kynetics.uf.android.communication.impl

import android.os.Message
import android.os.Messenger
import android.util.Log
import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.api.UFServiceConfiguration
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.android.client.RestartableClientService
import com.kynetics.uf.android.communication.GenericCommunicationApi
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.configuration.toUFServiceConfiguration
import com.kynetics.uf.android.configuration.AndroidDeploymentPermitProvider
import com.kynetics.uf.android.configuration.ConfigurationHandler

abstract class AbstractCommunicationApi(
    protected val configurationHandler: ConfigurationHandler,
    protected val ufService:RestartableClientService,
    private val softDeploymentPermitProvider: AndroidDeploymentPermitProvider
): GenericCommunicationApi {

    protected abstract val api: ApiCommunicationVersion

    private val tag:String by lazy {
        "${AbstractCommunicationApi::class.java.simpleName}-${api.versionCode}"
    }

    override fun subscribeClient(messenger: Messenger?, apiVersion: ApiCommunicationVersion) {
        Log.i(tag, "receive subscription request")
        MessengerHandler.subscribeClient(messenger, apiVersion)
    }

    override fun unsubscribeClient(messenger: Messenger?) {
        Log.i(tag, "receive un-subscription request")
        MessengerHandler.unsubscribeClient(messenger)
    }

    override fun sync(messenger: Messenger?) {
        Log.i(tag, "received sync request")

        if (messenger == null) {
            Log.i(tag, "command ignored because field replyTo is null")
            return
        }

        MessengerHandler.response(
            configurationHandler.getSecureConfiguration().toUFServiceConfiguration(),
            Communication.V1.Out.CurrentServiceConfiguration.ID,
            messenger
        )

        if (MessengerHandler.hasMessage(api)) {
            MessengerHandler.response(
                MessengerHandler.getlastSharedMessage(api).messageToSendOnSync,
                Communication.V1.Out.ServiceNotification.ID,
                messenger
            )
        }
        Log.i(tag, "client synced")
    }

    override fun forcePing() {
        Log.i(tag, "receive request to resume suspend state")
        ufService.forcePing()
    }

    override fun configureService(newConf: UFServiceConfiguration) {
        val currentConf = configurationHandler.getCurrentConfiguration()

        if (currentConf != newConf.toUFServiceConfiguration()) {
            configurationHandler.saveServiceConfigurationToSharedPreferences(newConf.toUFServiceConfiguration(), true)
            Log.i(tag, "configuration updated")
        } else {
            Log.i(tag, "new configuration equals to current configuration")
        }

        if (configurationHandler.needReboot(currentConf)) {
            ufService.restartService(configurationHandler)
            Log.i(tag, "configuration updated - restarting service")
        } else {
            MessengerHandler.notifyMessage(UFServiceMessageV1.Event.ConfigurationUpdated(configurationHandler.getCurrentConfiguration()))
            Log.i(tag, "configuration updated - service not restarted")
        }
    }

    override fun authorizationResponse(msg: Message) {
        Log.i(tag, "receive authorization response")
        if(!msg.data.containsKey(Communication.V1.SERVICE_DATA_KEY)){
            Log.i(tag, "Invalid authorization response message received")
            return
        }
        val response = msg.data.getBoolean(Communication.V1.SERVICE_DATA_KEY)
        softDeploymentPermitProvider.allow(response)

        Log.i(tag, String.format("authorization %s", if (response) "granted" else "denied"))
    }

    override fun onMessage(msg: Message) {
        when (msg.what) {
            Communication.V1.In.ConfigureService.ID -> configureService(msg)

            Communication.V1.In.RegisterClient.ID -> subscribeClient(msg.replyTo, api)

            Communication.V1.In.UnregisterClient.ID -> unsubscribeClient(msg.replyTo)

            Communication.V1.In.AuthorizationResponse.ID -> authorizationResponse(msg)

            Communication.V1.In.ForcePing.id -> forcePing()

            Communication.V1.In.Sync.ID -> sync(msg.replyTo)

            else -> Log.i(tag, "Invalid message receive (what == ${msg.what})")
        }
    }

    private fun configureService(msg: Message) {
        Log.i(tag, "receive configuration update request")
        val configuration =  try{
            if(!msg.data.containsKey(Communication.V1.SERVICE_DATA_KEY)){
                Log.i(tag, "Invalid configuration message received (no configuration found)")
                return
            }
            msg.data.getSerializable(Communication.V1.SERVICE_DATA_KEY) as UFServiceConfiguration
        } catch (e:Throwable){
            Log.i(tag, "Invalid configuration message received; Error on configuration deserialize.")
            return
        }
        configureService(configuration)
    }
}