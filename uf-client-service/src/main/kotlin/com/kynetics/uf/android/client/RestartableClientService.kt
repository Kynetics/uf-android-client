/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.client

import android.util.Log
import com.kynetics.uf.android.api.UFServiceConfigurationV2
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.android.communication.impl.getSecureConfiguration
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.configuration.AndroidForceDeploymentPermitProvider
import com.kynetics.uf.android.configuration.ConfigurationHandler
import com.kynetics.uf.android.cron.CronScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.eclipse.hara.ddiclient.api.DeploymentPermitProvider
import org.eclipse.hara.ddiclient.api.HaraClient
import org.eclipse.hara.ddiclient.api.MessageListener

class RestartableClientService constructor(
    private val client: UpdateFactoryClientWrapper,
    private val softDeploymentPermitProvider: DeploymentPermitProvider,

    listeners: List<MessageListener>): HaraClient by client{
    private var currentState:MessageListener.Message.State? = null
    private val _listeners:List<MessageListener> = listOf(
            object: MessageListener{
                override fun onMessage(message: MessageListener.Message) {
                    if(message is MessageListener.Message.State){
                        currentState = message
                    }
                }

            },
            *listeners.toTypedArray()
    )

    private val scope:CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val channel: Channel<ConfigurationHandler> = Channel(Channel.CONFLATED)
    private var currentSecureConf:UFServiceConfigurationV2? = null

    override fun startAsync() {
        if(currentSecureConf != null){
            MessengerHandler.notifyMessage(UFServiceMessageV1.Event.Started(currentSecureConf!!))
            client.startAsync()
        }
    }

    private val job:Job = scope.launch {
        for(conf in channel){
            runCatching {
                Log.i(TAG,"Try to restart the service")
                while (!serviceRestartable()) {
                    Log.i(TAG, "Service not restartable yet.")
                    MessengerHandler.notifyMessage(
                        UFServiceMessageV1.Event.CantBeStopped(currentSecureConf!!, RETRY_SERVICE_RESTART)
                    )
                    delay(RETRY_SERVICE_RESTART)
                }
                Log.d(TAG, "Restarting service")
                stop()
                currentSecureConf = conf.getSecureConfiguration()
                client.delegate = conf.buildServiceFromPreferences(softDeploymentPermitProvider,
                    AndroidForceDeploymentPermitProvider.build(CRON_TAG, conf.getCurrentConfiguration().timeWindows),
                    _listeners)
                startAsync()
                Log.d(TAG, "Service restarted")
            }.onFailure {
                Log.d(TAG, "Error on restarting service", it)
            }
        }
    }

    companion object{
        val TAG: String = RestartableClientService::class.java.simpleName
        fun newInstance(
            softDeploymentPermitProvider: DeploymentPermitProvider, listeners: List<MessageListener>): RestartableClientService {
            return RestartableClientService(
                    UpdateFactoryClientWrapper(null),
                    softDeploymentPermitProvider,
                    listeners)
        }

        const val CRON_TAG:String = "ForceDeploymentTag"
        private const val RETRY_SERVICE_RESTART:Long = 10000
    }

    fun restartService(conf:ConfigurationHandler) = channel.trySend(conf)

    override fun stop() {
        if(currentSecureConf!=null){
            MessengerHandler.notifyMessage(UFServiceMessageV1.Event.Stopped(currentSecureConf!!))
            client.stop()
            CronScheduler.removeScheduledJob(CRON_TAG)
        }
    }

    private fun serviceRestartable():Boolean{
        return currentState !=  MessageListener.Message.State.Updating
    }
}

