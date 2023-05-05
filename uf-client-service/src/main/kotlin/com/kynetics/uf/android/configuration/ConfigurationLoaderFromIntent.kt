/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.configuration

import android.content.Intent
import android.util.Log
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.api.UFServiceConfigurationV2

@Suppress("DEPRECATION")
interface ConfigurationLoaderFromIntent {
    fun getServiceConfigurationFromIntent(intent: Intent): UFServiceConfigurationV2? {
        Log.i(TAG, "Loading new configuration from intent")
        val serializable = intent.getSerializableExtra(Communication.V1.SERVICE_DATA_KEY)
        val string = intent.getStringExtra(Communication.V1.SERVICE_DATA_KEY)
        return try {
            when {

                serializable is String ->  com.kynetics.uf.android.api.UFServiceConfiguration.fromJson(serializable)

                serializable is com.kynetics.uf.android.api.UFServiceConfiguration -> serializable

                string != null ->  com.kynetics.uf.android.api.UFServiceConfiguration.fromJson(string)

                else -> null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Deserialization error", e)
            null
        }.run {
            if(this != null){
                Log.i(TAG, "Loaded new configuration from intent")
            } else {
                Log.i(TAG, "No configuration found in intent")
            }
            this?.toUFServiceConfiguration()
        }
    }

    companion object{
        private val TAG = ConfigurationLoaderFromIntent::class.java.name
    }
}