/*
 * Copyright © 2017-2022  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.configuration

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kynetics.uf.android.BuildConfig
import com.kynetics.uf.android.R
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.api.UFServiceConfigurationV2
import com.kynetics.uf.android.api.UFServiceConfigurationV2.TimeWindows.Companion.ALWAYS
import com.kynetics.uf.android.api.UFServiceConfigurationV2.TimeWindows.Companion.DEFAULT_WINDOW_SIZE
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.content.UFSharedPreferences
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.uf.android.update.system.SystemUpdateType
import com.kynetics.uf.android.update.application.ApkUpdater
import com.kynetics.uf.android.update.system.OtaUpdater
import com.kynetics.uf.ddiclient.HaraClientFactory
import com.kynetics.uf.ddiclient.TargetTokenFoundListener
import org.eclipse.hara.ddiclient.api.*
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

data class ConfigurationHandler(
    private val context: UpdateFactoryService,
    private val sharedPreferences: UFSharedPreferences
):ConfigurationLoaderFromIntent {

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
        configuration: UFServiceConfigurationV2?
    ) {
        if (configuration == null) {
            return
        }
        sharedPreferences.edit().apply {
            if(isTargetTokenReceivedFromServerOld(configuration)){
                remove(sharedPreferencesTargetTokenReceivedFromServer)
            }
            putString(sharedPreferencesControllerIdKey, configuration.controllerId)
            putString(sharedPreferencesTenantKey, configuration.tenant)
            putString(sharedPreferencesServerUrlKey, configuration.url)
            putString(sharedPreferencesGatewayToken, configuration.gatewayToken)
            putString(sharedPreferencesTargetToken, configuration.targetToken)
            putString(sharedPreferencesCronExpression, configuration.updateWindows.cronExpression)
            putString(sharedPreferencesUpdateWindowSize, "${configuration.updateWindows.windowSize}")
            putBoolean(sharedPreferencesApiModeKey, configuration.isApiMode)
            putBoolean(sharedPreferencesServiceEnableKey, configuration.isEnable)
            putBoolean(sharedPreferencesIsUpdateFactoryServerType, configuration.isUpdateFactoryServe)
            apply()
        }

        sharedPreferences.putAndCommitObject(sharedPreferencesTargetAttributes, configuration.targetAttributes)
    }

    fun getCurrentConfiguration(): UFServiceConfigurationV2 {
        return with(sharedPreferences){
            UFServiceConfigurationV2(
                tenant = getString(sharedPreferencesTenantKey, "")!!,
                controllerId = getString(sharedPreferencesControllerIdKey, "")!!,
                url = getString(sharedPreferencesServerUrlKey, "")!!,
                targetToken = getTargetToken(),
                gatewayToken = getString(sharedPreferencesGatewayToken, "")!!,
                isApiMode = getBoolean(sharedPreferencesApiModeKey, true),
                isEnable = getBoolean(sharedPreferencesServiceEnableKey, false),
                isUpdateFactoryServe = getBoolean(sharedPreferencesIsUpdateFactoryServerType, true),
                targetAttributes = getTargetAttributes(),
                updateWindows = UFServiceConfigurationV2.TimeWindows(getString(sharedPreferencesCronExpression, ALWAYS)!!, getString(sharedPreferencesUpdateWindowSize, "$DEFAULT_WINDOW_SIZE")!!.toLong())
            )
        }
    }

    private fun getTargetToken():String{
        val targetToken = sharedPreferences.getString(sharedPreferencesTargetToken,"")
        return if(targetToken == null || targetToken == ""){
            sharedPreferences.getString(sharedPreferencesTargetTokenReceivedFromServer, "")!!
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

    fun apiModeIsEnabled() = sharedPreferences.getBoolean(sharedPreferencesApiModeKey, false)

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
                MessengerHandler.onConfigurationError(listOf(e.message ?: "Error"))
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

    private fun buildConfigDataProvider(): ConfigDataProvider {
        return object : ConfigDataProvider {
            override fun configData(): Map<String, String> {
                return decorateTargetAttribute()
            }

            override fun isUpdated(): Boolean {
                val md5 = decorateTargetAttribute().toMD5()
                return md5 == sharedPreferences.getString(LAST_TARGET_ATTRIBUTES_MD5_SENT_KEY, "")
            }

            override fun onConfigDataUpdate() {
                val md5 = decorateTargetAttribute().toMD5()
                sharedPreferences.edit().putString(LAST_TARGET_ATTRIBUTES_MD5_SENT_KEY, md5)
                        .apply()
            }
        }
    }

    private fun getTargetTokenListener(): TargetTokenFoundListener {
        return object : TargetTokenFoundListener {
            override fun onFound(targetToken: String){
                Log.d(TAG, "New target token received")
                sharedPreferences.edit()
                        .putString(sharedPreferencesTargetTokenReceivedFromServer, targetToken)
                        .apply()
            }
        }
    }

    private fun getTargetAttributes(): MutableMap<String, String> {
        val targetAttributes: MutableMap<String, String>? = sharedPreferences
                .getObject(sharedPreferencesTargetAttributes)
        return targetAttributes ?: mutableMapOf()
    }

    private fun decorateTargetAttribute(): Map<String, String> {
        val targetAttributes = getTargetAttributes().toMutableMap()
        targetAttributes[CLIENT_TYPE_TARGET_TOKEN_KEY] = "Android"
        targetAttributes[CLIENT_VERSION_TARGET_ATTRIBUTE_KEY] = BuildConfig.VERSION_NAME // TODO: 4/17/18 refactor
        targetAttributes[CLIENT_VERSION_CODE_ATTRIBUTE_KEY] = BuildConfig.VERSION_CODE.toString()
        val buildDate = Date(Build.TIME)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.UK)
        targetAttributes[ANDROID_BUILD_DATE_TARGET_ATTRIBUTE_KEY] = dateFormat.format(buildDate)
        targetAttributes[ANDROID_BUILD_TYPE_TARGET_ATTRIBUTE_KEY] = Build.TYPE
        targetAttributes[ANDROID_FINGERPRINT_TARGET_ATTRIBUTE_KEY] = Build.FINGERPRINT
        targetAttributes[ANDROID_KEYS_TARGET_ATTRIBUTE_KEY] = Build.TAGS
        targetAttributes[ANDROID_VERSION_TARGET_ATTRIBUTE_KEY] = Build.VERSION.RELEASE
        targetAttributes[DEVICE_NAME_TARGET_ATTRIBUTE_KEY] = Build.DEVICE
        targetAttributes[SYSTEM_UPDATE_TYPE] = systemUpdateType!!.name
        return targetAttributes
    }

    private fun Map<String, String>.toMD5(): String {
        val content = entries.sortedBy { it.key }.joinToString("-") { "${it.key}_${it.value}" }
        val bytes = MessageDigest.getInstance("MD5").digest(content.toByteArray())
        return bytes.toMD5()
    }

    private fun ByteArray.toMD5(): String {
        return this.joinToString("") { "%02x".format(it) }
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
                    buildConfigDataProvider(),
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
                    buildConfigDataProvider(),
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

    private var systemUpdateType: SystemUpdateType? = SystemUpdateType.getSystemUpdateType()
    private val configurationFile = ConfigurationFileLoader(sharedPreferences, UF_CONF_FILE, context)

    private val sharedPreferencesServerUrlKey = context.getString(R.string.shared_preferences_server_url_key)
    private val sharedPreferencesApiModeKey = context.getString(R.string.shared_preferences_api_mode_key)
    private val sharedPreferencesTenantKey = context.getString(R.string.shared_preferences_tenant_key)
    private val sharedPreferencesControllerIdKey = context.getString(R.string.shared_preferences_controller_id_key)
    private val sharedPreferencesServiceEnableKey = context.getString(R.string.shared_preferences_is_enable_key)
    private val sharedPreferencesGatewayToken = context.getString(R.string.shared_preferences_gateway_token_key)
    private val sharedPreferencesTargetToken = context.getString(R.string.shared_preferences_target_token_key)
    private val sharedPreferencesTargetTokenReceivedFromServer = context.getString(R.string.shared_preferences_target_token_received_from_server_key)
    private val sharedPreferencesTargetAttributes = context.getString(R.string.shared_preferences_args_key)
    private val sharedPreferencesIsUpdateFactoryServerType = context.getString(R.string.shared_preferences_is_update_factory_server_type_key)
    private val sharedPreferencesCronExpression = context.getString(R.string.shared_preferences_schedule_update_cron_expresison_key)
    private val sharedPreferencesUpdateWindowSize = context.getString(R.string.shared_preferences_schedule_update_windows_size_key)

    private val currentUpdateState: CurrentUpdateState = CurrentUpdateState(context)

    companion object {
        private const val LAST_TARGET_ATTRIBUTES_MD5_SENT_KEY = "LAST_TARGET_ATTRIBUTES_MD5_SET_KEY"
        private const val CLIENT_VERSION_TARGET_ATTRIBUTE_KEY = "client_version"
        private const val CLIENT_VERSION_CODE_ATTRIBUTE_KEY = "client_version_code"
        private const val ANDROID_BUILD_DATE_TARGET_ATTRIBUTE_KEY = "android_build_date"
        private const val ANDROID_BUILD_TYPE_TARGET_ATTRIBUTE_KEY = "android_build_type"
        private const val ANDROID_FINGERPRINT_TARGET_ATTRIBUTE_KEY = "android_fingerprint"
        private const val ANDROID_KEYS_TARGET_ATTRIBUTE_KEY = "android_keys"
        private const val ANDROID_VERSION_TARGET_ATTRIBUTE_KEY = "android_version"
        private const val DEVICE_NAME_TARGET_ATTRIBUTE_KEY = "device_name"
        private const val SYSTEM_UPDATE_TYPE = "system_update_type"
        private const val CLIENT_TYPE_TARGET_TOKEN_KEY = "client"
        @SuppressLint("SdCardPath")
        private const val UF_CONF_FILE = "/sdcard/UpdateFactoryConfiguration/ufConf.conf"
        private val TAG: String = ConfigurationHandler::class.java.simpleName
    }
}
