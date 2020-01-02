/*
 * Copyright © 2017-2020  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.api

enum class ApiCommunicationVersion(val versionCode: Int, val versionName: String) {
    V0_1(0, "0.1"),
    V1(1, "1.0");

    companion object {
        fun fromVersionCode(versionCode: Int): ApiCommunicationVersion {
            return when (versionCode) {
                1 -> V1
                else -> V0_1
            }
        }
    }
}
