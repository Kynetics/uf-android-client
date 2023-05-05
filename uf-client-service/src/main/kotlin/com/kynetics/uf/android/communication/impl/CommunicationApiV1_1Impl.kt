/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.communication.impl

import android.os.Message
import android.os.Messenger
import android.util.Log
import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.api.UFServiceConfigurationV2
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.android.client.RestartableClientService
import com.kynetics.uf.android.communication.CommunicationApiV1_1
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.configuration.AndroidDeploymentPermitProvider
import com.kynetics.uf.android.configuration.ConfigurationHandler

@Suppress("ClassName")
open class CommunicationApiV1_1Impl(configurationHandler: ConfigurationHandler,
                               ufService: RestartableClientService,
                               softDeploymentPermitProvider: AndroidDeploymentPermitProvider
):
    AbstractCommunicationApi(configurationHandler, ufService, softDeploymentPermitProvider),
    CommunicationApiV1_1 {
    override val api: ApiCommunicationVersion = ApiCommunicationVersion.V1_1

    override fun configureService(newConf: UFServiceConfigurationV2) {
        val currentConf = configurationHandler.getCurrentConfiguration()

        if (currentConf != newConf) {
            configurationHandler.saveServiceConfigurationToSharedPreferences(newConf)
            Log.i(TAG, "configuration updated")
        } else {
            Log.i(TAG, "new configuration equals to current configuration")
        }

        if (configurationHandler.needReboot(currentConf)) {
            ufService.restartService(configurationHandler)
            Log.i(TAG, "configuration updated - restarting service")
        } else {
            MessengerHandler.notifyMessage(UFServiceMessageV1.Event.Started(configurationHandler.getSecureConfiguration()))
            Log.i(TAG, "configuration updated - service not restarted")
        }
    }

    override fun onMessage(msg: Message) {
        when (msg.what) {
            Communication.V1.In.ConfigureServiceV2.ID -> configureService(msg)
            else -> super.onMessage(msg)
        }
    }

    override fun sync(messenger: Messenger?) {
        Log.i(TAG, "received sync request")

        if (messenger == null) {
            Log.i(TAG, "command ignored because field replyTo is null")
            return
        }

        MessengerHandler.response(
            configurationHandler.getSecureConfiguration().toJson(),
            Communication.V1.Out.CurrentServiceConfigurationV2.ID,
            messenger
        )

        if (MessengerHandler.hasMessage(api)) {
            MessengerHandler.response(
                MessengerHandler.getlastSharedMessage(api).messageToSendOnSync,
                Communication.V1.Out.ServiceNotification.ID,
                messenger
            )
        }
        Log.i(TAG, "client synced")
    }

    private fun configureService(msg: Message) {
        Log.i(TAG, "receive configuration update request")
        val configuration =  try{
            if(!msg.data.containsKey(Communication.V1.SERVICE_DATA_KEY)){
                Log.i(TAG, "Invalid configuration message received (no configuration found)")
                return
            }
            UFServiceConfigurationV2.fromJson(msg.data.getString(Communication.V1.SERVICE_DATA_KEY)!!)
        } catch (e:Throwable){
            Log.i(TAG, "Invalid configuration message received; Error on configuration deserialize.")
            return
        }
        configureService(configuration)
    }

    companion object{
        val TAG:String = CommunicationApiV1_1Impl::class.java.simpleName

    }
}