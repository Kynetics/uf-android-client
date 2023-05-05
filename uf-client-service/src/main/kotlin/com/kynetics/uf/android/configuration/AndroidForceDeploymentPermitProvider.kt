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

import com.kynetics.uf.android.cron.CronScheduler
import com.kynetics.uf.android.api.UFServiceConfigurationV2
import com.kynetics.uf.android.cron.notifyScheduleStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.eclipse.hara.ddiclient.api.DeploymentPermitProvider

interface AndroidForceDeploymentPermitProvider: DeploymentPermitProvider {

    companion object{

        fun build(tag:String, timeWindows: UFServiceConfigurationV2.TimeWindows):AndroidForceDeploymentPermitProvider{
            return object : AndroidForceDeploymentPermitProvider{

                private var forceResponse = CompletableDeferred<Boolean>()

                override fun updateAllowed(): Deferred<Boolean> {
                    forceResponse.complete(false)
                    forceResponse = CompletableDeferred()

                    CronScheduler.schedule(tag, timeWindows){
                        forceResponse.complete(true)
                    }.notifyScheduleStatus()

                    return forceResponse
                }
            }
        }
    }
}