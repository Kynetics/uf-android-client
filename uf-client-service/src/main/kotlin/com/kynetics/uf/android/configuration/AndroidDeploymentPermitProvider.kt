/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.configuration

import android.app.NotificationManager
import android.content.Intent
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.ui.MainActivity
import com.kynetics.uf.android.update.CurrentUpdateState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.eclipse.hara.ddiclient.api.DeploymentPermitProvider

interface AndroidDeploymentPermitProvider : DeploymentPermitProvider {
    fun allow(isAllowed: Boolean)

    companion object {
        fun build(
            configurationHandler: ConfigurationHandler,
            mNotificationManager: NotificationManager,
            service: UpdateFactoryService
        ): AndroidDeploymentPermitProvider {
            return object : AndroidDeploymentPermitProvider {

                private var authResponse = CompletableDeferred<Boolean>()

                private fun allowedAsync(auth: UpdateFactoryService.Companion.AuthorizationType): Deferred<Boolean> {
                    if (configurationHandler.apiModeIsEnabled()) {
                        MessengerHandler.sendBroadcastMessage(Communication.V1.Out.AuthorizationRequest.ID, auth.name)
                    } else {
                        showAuthorizationDialog(auth)
                        mNotificationManager.notify(UpdateFactoryService.AUTHORIZATION_GRANT_NOTIFICATION_ID,
                            service.getNotification(auth.event, true))
                    }

                    authResponse.complete(false)
                    authResponse = CompletableDeferred()
                    authResponse.invokeOnCompletion {
                        if (authResponse.getCompleted()) {
                            MessengerHandler.onAction(auth.toActionOnGranted)
                        } else {
                            MessengerHandler.onAction(auth.toActionOnDenied)
                        }
                    }

                    return authResponse
                }

                override fun allow(isAllowed: Boolean) {
                    if (isAllowed)
                        authResponse.complete(true)
                }

                override fun downloadAllowed(): Deferred<Boolean> {
                    val currentUpdateState = CurrentUpdateState(service)
                    return if (currentUpdateState.isAllFileDownloaded()) {
                        CompletableDeferred(true)
                    } else {
                        allowedAsync(UpdateFactoryService.Companion.AuthorizationType.DOWNLOAD)
                    }
                }

                override fun updateAllowed(): Deferred<Boolean> {
                    val currentUpdateState = CurrentUpdateState(service)
                    return if(currentUpdateState.isUpdateStart()){
                        CompletableDeferred(true)
                    } else {
                        allowedAsync(UpdateFactoryService.Companion.AuthorizationType.UPDATE)
                    }
                }

                private fun showAuthorizationDialog(authorization: UpdateFactoryService.Companion.AuthorizationType) {
                    val intent = Intent(service, MainActivity::class.java)
                    intent.putExtra(MainActivity.INTENT_TYPE_EXTRA_VARIABLE, authorization.extra)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    service.startActivity(intent)
                }
            }
        }
    }
}
