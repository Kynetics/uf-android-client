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

import android.content.Context
import android.content.SharedPreferences
import com.kynetics.uf.android.api.UFServiceConfigurationV2
import com.kynetics.uf.android.configuration.VariableEvaluation.Companion.parseStringWithVariable
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * @author Daniele Sergio
 */
class ConfigurationFileLoader(private val sh: SharedPreferences, private val configurationFilePath: String, private val context: Context) {
    private val map: MutableMap<String, String> = HashMap()
    val newFileConfiguration: UFServiceConfigurationV2?
        get() {
            if (!configurationFileFound() || isServiceConfiguredWithApi || !isNewConfigurationFile) {
                return null
            }

            return  UFServiceConfigurationV2(
                isEnable = getBooleanConfiguration(ENABLE_CONFIGURATION_KEY),
                isApiMode = apiModeConfiguration,
                isUpdateFactoryServe = getBooleanConfiguration(IS_UPDATE_FACTORY_SERVER_KEY),
                gatewayToken = map.getOrDefault(GATEWAY_TOKEN_CONFIGURATION_KEY, ""),
                targetToken = map.getOrDefault(TARGET_TOKEN_CONFIGURATION_KEY, ""),
                tenant = map.getOrDefault(TENANT_CONFIGURATION_KEY, ""),
                controllerId = parseStringWithVariable(map.getOrDefault(CONTROLLER_ID_CONFIGURATION_KEY, ""), context),
                url = map.getOrDefault(URL_CONFIGURATION_KEY,"")
            ).run {
                if(isValid){
                    this
                } else {
                    null
                }
            }
        }

    private val apiModeConfiguration: Boolean
        get() {
            val value = map[API_MODE_CONFIGURATION_KEY]
            return value != null && value.equals("TRUE", ignoreCase = true)
        }

    private fun getBooleanConfiguration(mapKey: String): Boolean {
        val value = map[mapKey]
        return value == null || !value.equals("FALSE", ignoreCase = true)
    }

    private val isNewConfigurationFile: Boolean
        get() {
            try {
                BufferedReader(FileReader(configurationFilePath)).use { br ->
                    val md5digest = digest
                    br.lineSequence().forEach{ line ->
                        md5digest!!.update(line.toByteArray())
                        val keyValue = line.split(SPLIT_CHAR.toRegex()).toTypedArray()
                        if (keyValue.size == 2) {
                            map[keyValue[0].trim { it <= ' ' }] = keyValue[1].trim { it <= ' ' }
                        }
                    }
                    val bigInt = BigInteger(1, md5digest!!.digest())
                    val md5NewConfigurationFile = bigInt.toString(16)
                    val md5LastConfigurationFileLoaded = sh.getString(SHARED_PREFERENCES_LAST_CONFIGURATION_FILE_KEY, "")
                    sh.edit().putString(SHARED_PREFERENCES_LAST_CONFIGURATION_FILE_KEY, md5NewConfigurationFile).apply()
                    return md5NewConfigurationFile != md5LastConfigurationFileLoaded
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return false
        }

    private val isServiceConfiguredWithApi: Boolean
        get() {
            val keys = SharedPreferencesKeys.getInstance(context)
            return sh.getBoolean(keys.sharedPreferencesConfigurationLoadedFromApi, false)
        }

    private fun configurationFileFound(): Boolean {
        val file = File(configurationFilePath)
        if (!file.exists()) {
            map.clear()
            return false
        }
        return true
    }

    private val digest: MessageDigest?
        get() {
            try {
                return MessageDigest.getInstance("MD5")
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
            return null
        }

    companion object {
        private const val SHARED_PREFERENCES_LAST_CONFIGURATION_FILE_KEY = "LAST_CONFIGURATION_FILE_KEY"
        private const val SPLIT_CHAR = "="
        private const val TENANT_CONFIGURATION_KEY = "tenant"
        private const val URL_CONFIGURATION_KEY = "url"
        private const val CONTROLLER_ID_CONFIGURATION_KEY = "controllerId"
        private const val GATEWAY_TOKEN_CONFIGURATION_KEY = "gatewayToken"
        private const val TARGET_TOKEN_CONFIGURATION_KEY = "targetToken"
        private const val API_MODE_CONFIGURATION_KEY = "apiMode"
        private const val ENABLE_CONFIGURATION_KEY = "enable"
        private const val IS_UPDATE_FACTORY_SERVER_KEY = "updateFactoryServer"
    }

}