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
import com.kynetics.uf.android.api.AggregationPolicy
import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.api.TargetAttributesWithPolicy
import com.kynetics.uf.android.client.RestartableClientService
import com.kynetics.uf.android.communication.CommunicationApiV1_2
import com.kynetics.uf.android.configuration.AndroidDeploymentPermitProvider
import com.kynetics.uf.android.configuration.ConfigurationHandler

@Suppress("ClassName")
class CommunicationApiV1_2Impl(configurationHandler: ConfigurationHandler,
                               ufService: RestartableClientService,
                               softDeploymentPermitProvider: AndroidDeploymentPermitProvider
):
    CommunicationApiV1_1Impl(configurationHandler, ufService, softDeploymentPermitProvider),
    CommunicationApiV1_2 {

    override val api: ApiCommunicationVersion = ApiCommunicationVersion.V1_2

    override fun onMessage(msg: Message) {
        @Suppress("UNCHECKED_CAST")
        when (msg.what) {
            Communication.V1.In.AddTargetAttributes.ID -> addTargetAttributes(TargetAttributesWithPolicy.parse(msg.data.getString(Communication.V1.SERVICE_DATA_KEY)!!) )
            else -> super.onMessage(msg)
        }
    }

    override fun addTargetAttributes(targetAttributesWithPolicy: TargetAttributesWithPolicy) {
        configurationHandler.saveAddTargetAttributes(targetAttributesWithPolicy.attributes, targetAttributesWithPolicy.policy == AggregationPolicy.MERGE)
    }

    companion object{
        val TAG:String = CommunicationApiV1_2Impl::class.java.simpleName

    }
}