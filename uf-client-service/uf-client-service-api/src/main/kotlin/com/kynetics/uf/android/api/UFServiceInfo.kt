/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.api

/**
 * Utility constants related to the Update Factory Service
 */
object UFServiceInfo {
    /**
     * Package name of the Update Factory Service
     */
    const val SERVICE_PACKAGE_NAME = "com.kynetics.uf.service"

    /**
     * Action to bind with the Update Factory Service
     */
    const val SERVICE_ACTION = "com.kynetics.action.BIND_UF_SERVICE"

    /**
     * Action to open the Update Factory Service settings
     */
    const val ACTION_SETTINGS = "com.kynetics.action.SETTINGS"
}
