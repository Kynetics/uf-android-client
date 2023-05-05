/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

@file:Suppress("DEPRECATION")

package com.kynetics.uf.android.configuration

import com.kynetics.uf.android.api.UFServiceConfigurationV2

fun com.kynetics.uf.android.api.UFServiceConfiguration.toUFServiceConfiguration(): UFServiceConfigurationV2{
    @Suppress("USELESS_ELVIS")
    return     UFServiceConfigurationV2(
        tenant = tenant,
        controllerId = controllerId,
        url = url,
        targetToken = targetToken,
        gatewayToken = gatewayToken,
        isApiMode = isApiMode(),
        isEnable = isEnable(),
        isUpdateFactoryServe = isUpdateFactoryServe,
        // targetAttributes could be null when the UFServiceConfiguration is provided from ConfigureService message
        targetAttributes = targetAttributes ?: emptyMap()
    )
}

fun UFServiceConfigurationV2.toUFServiceConfiguration(): com.kynetics.uf.android.api.UFServiceConfiguration =
    com.kynetics.uf.android.api.UFServiceConfiguration.builder()
        .withTenant(tenant)
        .withControllerId(controllerId)
        .withUrl(url)
        .withTargetToken(targetToken)
        .withGatewayToken(gatewayToken)
        .withEnable(isEnable)
        .withApiMode(isApiMode)
        .withIsUpdateFactoryServer(isUpdateFactoryServe)
        .withTargetAttributes(targetAttributes)
        .build()