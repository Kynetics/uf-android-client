/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

@file:Suppress("ClassName", "DEPRECATION")

package com.kynetics.uf.android.communication

import android.os.Message
import android.os.Messenger
import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.api.UFServiceConfiguration

interface GenericCommunicationApi:CommunicationApi {
    fun subscribeClient(messenger: Messenger?, apiVersion: ApiCommunicationVersion)
    fun unsubscribeClient(messenger: Messenger?)
    fun sync(messenger: Messenger?)
    fun forcePing()
    fun configureService(newConf:UFServiceConfiguration)
    fun authorizationResponse(msg:Message)
}