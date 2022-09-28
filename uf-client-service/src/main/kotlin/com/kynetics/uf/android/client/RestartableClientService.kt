/*
 * Copyright © 2017-2022  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.client

import android.util.Log
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

    private var job:Job = scope.launch {
        for(conf in channel){
            runCatching {
                Log.i(TAG,"Try to restart the service")
                while (!serviceRestartable()) {
                    Log.i(TAG, "Service not restartable yet.")
                    delay(10000)
                }
                Log.d(TAG, "Restarting service")
                client.stop()
                CronScheduler.removeScheduledJob(CRON_TAG)
                client.delegate = conf.buildServiceFromPreferences(softDeploymentPermitProvider,
                    AndroidForceDeploymentPermitProvider.build(CRON_TAG, conf.getCurrentConfiguration().updateWindows),
                    _listeners)
                client.startAsync()
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

        private const val CRON_TAG = "ForceDeploymentTag"
    }

    fun restartService(conf:ConfigurationHandler) = channel.trySend(conf)

    override fun stop() {
        client.stop()
        scope.cancel()
    }

    private fun serviceRestartable():Boolean{
        return currentState !=  MessageListener.Message.State.Updating
    }
}

