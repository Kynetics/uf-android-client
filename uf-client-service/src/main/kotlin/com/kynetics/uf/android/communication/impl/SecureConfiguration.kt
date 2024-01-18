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

import com.kynetics.uf.android.api.UFServiceConfigurationV2
import com.kynetics.uf.android.configuration.ConfigurationHandler
import com.kynetics.uf.android.formatter.toPwdFormat

fun ConfigurationHandler.getSecureConfiguration():UFServiceConfigurationV2 =
    getCurrentConfiguration()
        .run{
            copy(
                targetToken = targetToken.toPwdFormat(),
                gatewayToken = gatewayToken.toPwdFormat()
            )
        }
