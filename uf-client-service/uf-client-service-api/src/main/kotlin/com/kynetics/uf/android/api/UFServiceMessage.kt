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

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * This class represents the service state (API version 0.1)
 *
 * @see ApiCommunicationVersion.V0_1
 * @see com.kynetics.uf.android.api.v1.UFServiceMessageV1
 */
@Deprecated(message = "As of release 1.0.0 replaced by com.kynetics.uf.android.api.v1.UFServiceMessageV1")
class UFServiceMessage(
    val eventName: String,
    val oldState: String,
    val currentState: String,
    val suspend: Suspend
) : Serializable {
    val dateTime: String

    @Deprecated(message = "As of release 1.0.0")
    enum class Suspend {
        NONE, DOWNLOAD, UPDATE
    }

    init {
        val dateFormat = SimpleDateFormat("HH:mm:ss")
        this.dateTime = dateFormat.format(Date())
    }

    @Deprecated(message = "As of release 1.0.0")
    companion object {
        private const val serialVersionUID = -7571115123564137773L
    }
}
