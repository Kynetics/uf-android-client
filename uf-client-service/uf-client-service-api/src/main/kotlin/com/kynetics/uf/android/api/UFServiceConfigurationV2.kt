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

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Suppress("KDocUnresolvedReference")
@Serializable
/**
 * This class represent the [com.kynetics.uf.android.UpdateFactoryService] configuration
 *
 * @property tenant the tenant
 * @property controllerId id of the controller
 * @property url of update server
 * @property targetToken target token
 * @property gatewayToken gateway token
 * @property isApiMode true to handle authorization via service messages
 *  [com.kynetics.uf.android.api.Communication.V1.In.AuthorizationResponse], false to use service
 *  built-in dialog
 * @property isEnable true to enable the service, false to disable it
 * @property isUpdateFactoryServe true when the server is Update Factory, false when the server
 *  is hawkBit
 * @property targetAttributes target metadata sent to the server
 * @property timeWindows configuration parameters for forced updates with time windows
 */
data class UFServiceConfigurationV2(
    /**
     * The tenant
     */
    val tenant: String,

    /**
     * Id of the controller
     */
    val controllerId: String,

    /**
     * The Update Server URL
     */
    val url: String,

    /**
     * Target token
     */
    val targetToken: String,

    /**
     * Gateway token
     */
    val gatewayToken: String,

    /**
     * true to handle authorization via service messages, false to use service built-in dialog
     *
     * @see com.kynetics.uf.android.api.Communication.V1.In.AuthorizationResponse
     */
    val isApiMode: Boolean,

    /**
     * true to enable the service, false otherwise
     */
    val isEnable: Boolean,

    /**
     * true when the server is Update Factory, false when the server is hawkBit
     */
    val isUpdateFactoryServe: Boolean,

    /**
     * target metadata sent to the server
     */
    val targetAttributes: Map<String, String> = emptyMap(),

    /**
     * configuration parameters for forced updates with time windows
     */
    val timeWindows: TimeWindows = TimeWindows()
){
    @Serializable
    /**
     * Class to configure the time windows behaviour. It defines when a forced update can be applied
     * based on a schedule defined as cron expression (QUARTZ) and a duration in seconds.
     *
     * @property cronExpression, a cron expression (QUARTZ) that defines the time windows
     *  beginning time.
     * @property duration, duration of time windows in seconds. Min value is 1.
     */
    data class TimeWindows(
        /**
         * A cron expression (QUARTZ) that defines when the time windows begin.
         * Default value: * * * ? * *
         */
        val cronExpression:String = ALWAYS,
        /**
         * Duration of time windows in seconds. Min value is 1.
         * Default value: 3600
         */
        val duration: Long = DEFAULT_WINDOW_DURATION
    ) {
        companion object{
            const val ALWAYS:String = "* * * ? * *"
            const val DEFAULT_WINDOW_DURATION: Long = 3600
        }
        val isValid:Boolean = duration > 1
    }

    /**
     * True if the configuration is valid
     */
    val isValid:Boolean by lazy{
        tenant.isNotEmpty() &&
                url.isNotEmpty() &&
                controllerId.isNotEmpty() &&
                (targetToken.isNotEmpty() || gatewayToken.isNotEmpty()) &&
                timeWindows.isValid
    }

    /**
     * Json serialization
     */
    fun toJson(): String {
        return json.encodeToString(serializer(), this)
    }

    companion object{
        private val json = Json { encodeDefaults = true }

        /**
         * Deserializes given json [data] into a corresponding object of type [UFServiceConfigurationV2].
         * @throws [JsonException] in case of malformed json
         * @throws [SerializationException] if given input can not be deserialized
         * @return the [UFServiceConfigurationV2] parsed from [data]
         */
        @JvmStatic
        fun fromJson(data: String): UFServiceConfigurationV2 {
            return json.decodeFromString(serializer(), data)
        }
    }
}