/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.api

/**
 * This class contains all communication constants
 *
 * @see com.kynetics.uf.android.api.Communication
 * @see com.kynetics.uf.android.api.UFServiceInfo
 */
@Deprecated(message = "As of release 1.0.0-rc3 replaced by com.kynetics.uf.android.api.Communication and com.kynetics.uf.android.api.UFServiceInfo")
object UFServiceCommunicationConstants {

    const val MSG_CONFIGURE_SERVICE = 1

    const val MSG_REGISTER_CLIENT = 2

    const val MSG_UNREGISTER_CLIENT = 3

    const val MSG_SERVICE_STATUS = 4

    const val MSG_AUTHORIZATION_REQUEST = 5

    const val MSG_AUTHORIZATION_RESPONSE = 6

    const val MSG_RESUME_SUSPEND_UPGRADE = 7

    const val MSG_SYNC_REQUEST = 8

    const val MSG_SERVICE_CONFIGURATION_STATUS = 9

    const val MSG_FORCE_PING = 10

    const val SERVICE_PACKAGE_NAME = "com.kynetics.uf.service"

    const val SERVICE_ACTION = "com.kynetics.action.BIND_UF_SERVICE"

    const val SERVICE_DATA_KEY = "DATA_KEY"

    const val SERVICE_API_VERSION_KEY = "API_VERSION_KEY"

    const val ACTION_SETTINGS = "com.kynetics.action.SETTINGS"

    internal const val ACTION_DOWNLOAD_AUTHORIZATION = "com.kynetics.action.DOWNLOAD_AUTHORIZATION"

    internal const val ACTION_RESTART_AUTHORIZATION = "com.kynetics.action.RESTART_AUTHORIZATION"
}
