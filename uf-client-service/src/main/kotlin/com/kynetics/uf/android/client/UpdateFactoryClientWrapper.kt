/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.client

import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import org.eclipse.hara.ddiclient.api.*

class UpdateFactoryClientWrapper(@Volatile var delegate: HaraClient? = null): HaraClient {
    override fun forcePing() {
        delegate?.forcePing()
    }

    override fun init(haraClientData: HaraClientData,
                      directoryForArtifactsProvider: DirectoryForArtifactsProvider,
                      configDataProvider: ConfigDataProvider,
                      softDeploymentPermitProvider: DeploymentPermitProvider,
                      messageListeners: List<MessageListener>,
                      updaters: List<Updater>,
                      downloadBehavior: DownloadBehavior,
                      forceDeploymentPermitProvider: DeploymentPermitProvider,
                      httpBuilder: OkHttpClient.Builder,
                      scope:CoroutineScope) {
        delegate?.init(haraClientData,
            directoryForArtifactsProvider,
            configDataProvider,
            softDeploymentPermitProvider,
            messageListeners,
            updaters.toList(),
            downloadBehavior,
            forceDeploymentPermitProvider,
            httpBuilder,
            scope)
    }

    override fun startAsync() {
        delegate?.startAsync()
    }

    override fun stop() {
        delegate?.stop()
    }
}