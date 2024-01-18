/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.communication.impl

import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.client.RestartableClientService
import com.kynetics.uf.android.communication.CommunicationApiV1
import com.kynetics.uf.android.configuration.AndroidDeploymentPermitProvider
import com.kynetics.uf.android.configuration.ConfigurationHandler

class CommunicationApiV1Impl(configurationHandler: ConfigurationHandler,
                             ufService: RestartableClientService,
                             softDeploymentPermitProvider: AndroidDeploymentPermitProvider
):
    AbstractCommunicationApi(configurationHandler, ufService, softDeploymentPermitProvider),
    CommunicationApiV1 {
    override val api: ApiCommunicationVersion = ApiCommunicationVersion.V1
}