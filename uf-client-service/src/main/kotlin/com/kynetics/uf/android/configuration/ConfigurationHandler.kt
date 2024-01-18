/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.configuration

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.api.UFServiceConfigurationV2
import com.kynetics.uf.android.api.UFServiceConfigurationV2.TimeWindows.Companion.ALWAYS
import com.kynetics.uf.android.api.UFServiceConfigurationV2.TimeWindows.Companion.DEFAULT_WINDOW_DURATION
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.content.SharedPreferencesWithObject
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.uf.android.update.application.ApkUpdater
import com.kynetics.uf.android.update.system.OtaUpdater
import com.kynetics.uf.ddiclient.HaraClientFactory
import com.kynetics.uf.ddiclient.TargetTokenFoundListener
import org.eclipse.hara.ddiclient.api.*
import java.io.File
import java.util.*

data class ConfigurationHandler (
    private val context: UpdateFactoryService,
    private val sharedPreferences: SharedPreferencesWithObject
):ConfigurationLoaderFromIntent, TargetAttributesHandler by TargetAttributesHandlerImpl(
    sharedPreferences,
    SharedPreferencesKeys.getInstance(context),
    getTimeWindows(sharedPreferences, SharedPreferencesKeys.getInstance(context))) {

    fun getConfigurationFromFile(): UFServiceConfigurationV2? = configurationFile.newFileConfiguration

    override fun getServiceConfigurationFromIntent(intent: Intent): UFServiceConfigurationV2? {
        Log.i(TAG, "Loading new configuration from intent")
        val string = intent.getStringExtra(Communication.V1.SERVICE_DATA_KEY)
        return try {
            when {
                string != null -> UFServiceConfigurationV2.fromJson(string)
                else -> super.getServiceConfigurationFromIntent(intent)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Deserialization error", e)
            null
        }.apply {
            if(this != null){
                Log.i(TAG, "Loaded new configuration from intent")
            } else {
                Log.i(TAG, "No configuration found in intent")
            }
        }
    }

    fun saveServiceConfigurationToSharedPreferences(
        configuration: UFServiceConfigurationV2?,
        configurationLoadedFromApi: Boolean = false
    ) {
        if (configuration == null) {
            return
        }
        sharedPreferences.edit().apply {
            if(isTargetTokenReceivedFromServerOld(configuration)){
                remove(keys.sharedPreferencesTargetTokenReceivedFromServer)
            }

            putBoolean(keys.sharedPreferencesConfigurationLoadedFromApi, configurationLoadedFromApi)

            putString(keys.sharedPreferencesControllerIdKey, configuration.controllerId)
            putString(keys.sharedPreferencesTenantKey, configuration.tenant)
            putString(keys.sharedPreferencesServerUrlKey, configuration.url)
            putString(keys.sharedPreferencesGatewayToken, configuration.gatewayToken)
            putString(keys.sharedPreferencesTargetToken, configuration.targetToken)
            putString(keys.sharedPreferencesCronExpression, configuration.timeWindows.cronExpression)
            putString(keys.sharedPreferencesTimeWindowsDuration, "${configuration.timeWindows.duration}")
            putBoolean(keys.sharedPreferencesApiModeKey, configuration.isApiMode)
            putBoolean(keys.sharedPreferencesServiceEnableKey, configuration.isEnable)
            putBoolean(keys.sharedPreferencesIsUpdateFactoryServerType, configuration.isUpdateFactoryServe)
            apply()
        }

        saveConfigurationTargetAttributes(configuration.targetAttributes)
    }

    fun getCurrentConfiguration(): UFServiceConfigurationV2 {
        return with(sharedPreferences){
            UFServiceConfigurationV2(
                tenant = getString(keys.sharedPreferencesTenantKey, "")!!,
                controllerId = getString(keys.sharedPreferencesControllerIdKey, "")!!,
                url = getString(keys.sharedPreferencesServerUrlKey, "")!!,
                targetToken = getTargetToken(),
                gatewayToken = getString(keys.sharedPreferencesGatewayToken, "")!!,
                isApiMode = getBoolean(keys.sharedPreferencesApiModeKey, true),
                isEnable = getBoolean(keys.sharedPreferencesServiceEnableKey, false),
                isUpdateFactoryServe = getBoolean(keys.sharedPreferencesIsUpdateFactoryServerType, true),
                targetAttributes = getConfigurationTargetAttributes(),
                timeWindows = getTimeWindows(sharedPreferences, keys)
            )
        }
    }

    private fun getTargetToken():String{
        val targetToken = sharedPreferences.getString(keys.sharedPreferencesTargetToken,"")
        return if(targetToken == null || targetToken == ""){
            sharedPreferences.getString(keys.sharedPreferencesTargetTokenReceivedFromServer, "")!!
        } else {
            targetToken
        }
    }

    private fun isTargetTokenReceivedFromServerOld(newConf: UFServiceConfigurationV2):Boolean{
        val currentConf = getCurrentConfiguration()
        return currentConf.controllerId != newConf.controllerId
                || currentConf.tenant != newConf.tenant
                || currentConf.url != newConf.url
    }

    fun apiModeIsEnabled() = sharedPreferences.getBoolean(keys.sharedPreferencesApiModeKey, false)

    fun buildServiceFromPreferences(
        softDeploymentPermitProvider: DeploymentPermitProvider,
        forceDeploymentPermitProvider: DeploymentPermitProvider,
        listeners: List<MessageListener>
    ): HaraClient? {
        val serviceConfiguration = getCurrentConfiguration()
        var newService: HaraClient? = null
        if (serviceConfiguration.isEnable) {
            try {
                newService = serviceConfiguration.toService(softDeploymentPermitProvider, forceDeploymentPermitProvider, listeners)
            } catch (e: RuntimeException) {
                newService = null
                MessengerHandler.notifyConfigurationError(listOf(e.message ?: "Error"))
                Log.e(TAG, e.message, e)
            }
        }
        return newService
    }

    fun needReboot(oldConf: UFServiceConfigurationV2?): Boolean {
        val newConf = getCurrentConfiguration()
        return newConf.copy(targetAttributes = mutableMapOf()) !=
                oldConf?.copy(targetAttributes = mutableMapOf())
    }

    private fun getTargetTokenListener(): TargetTokenFoundListener {
        return object : TargetTokenFoundListener {
            override fun onFound(targetToken: String){
                Log.d(TAG, "New target token received")
                with(sharedPreferences){
                    if(targetToken != getString(keys.sharedPreferencesTargetTokenReceivedFromServer, null)){
                        edit()
                            .putString(keys.sharedPreferencesTargetTokenReceivedFromServer, targetToken)
                            .apply()
                        MessengerHandler.notifyMessage(UFServiceMessageV1.Event.NewTargetTokenReceived)
                    }
                }
            }
        }
    }


    private fun UFServiceConfigurationV2.toService(
        deploymentPermitProvider: DeploymentPermitProvider,
        forceDeploymentPermitProvider: DeploymentPermitProvider,
        listeners: List<MessageListener>
    ): HaraClient {
        return if(isUpdateFactoryServe){
            HaraClientFactory.newUFClient(
                    toClientData(),
                    object : DirectoryForArtifactsProvider {
                        override fun directoryForArtifacts(): File = currentUpdateState.rootDir()
                    },
                    newConfigDataProvider(),
                    deploymentPermitProvider,
                    listeners,
                    listOf(OtaUpdater(context),ApkUpdater(context)),
                    forceDeploymentPermitProvider,
                    getTargetTokenListener()
            )
        } else {
            HaraClientFactory.newHawkbitClient(
                    toClientData(),
                    object : DirectoryForArtifactsProvider {
                        override fun directoryForArtifacts(): File = currentUpdateState.rootDir()
                    },
                    newConfigDataProvider(),
                    deploymentPermitProvider,
                    listeners,
                    forceDeploymentPermitProvider,
                    listOf(OtaUpdater(context),ApkUpdater(context))
            )
        }
    }

    private fun UFServiceConfigurationV2.toClientData(): HaraClientData {
        return HaraClientData(
                tenant,
                controllerId,
                url,
                gatewayToken,
                targetToken
        )
    }

    private val configurationFile = ConfigurationFileLoader(sharedPreferences, UF_CONF_FILE, context)


    private val currentUpdateState: CurrentUpdateState = CurrentUpdateState(context)
    private val keys:SharedPreferencesKeys = SharedPreferencesKeys.getInstance(context)

    companion object {
        @SuppressLint("SdCardPath")
        private const val UF_CONF_FILE = "/sdcard/UpdateFactoryConfiguration/ufConf.conf"
        private val TAG: String = ConfigurationHandler::class.java.simpleName

        private fun getTimeWindows(sharedPreferences:SharedPreferences, keys:SharedPreferencesKeys):UFServiceConfigurationV2.TimeWindows =
            with(sharedPreferences){
                UFServiceConfigurationV2.TimeWindows(getString(keys.sharedPreferencesCronExpression,
                    ALWAYS
                )!!, getString(keys.sharedPreferencesTimeWindowsDuration, "$DEFAULT_WINDOW_DURATION")!!.toLong())
            }
    }
}
