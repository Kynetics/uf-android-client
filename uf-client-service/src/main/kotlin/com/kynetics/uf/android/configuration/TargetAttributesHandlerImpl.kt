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

import android.os.Build
import com.kynetics.uf.android.BuildConfig
import com.kynetics.uf.android.api.UFServiceConfigurationV2
import com.kynetics.uf.android.content.SharedPreferencesWithObject
import com.kynetics.uf.android.md5.toMD5
import com.kynetics.uf.android.update.system.SystemUpdateType
import org.eclipse.hara.ddiclient.api.ConfigDataProvider
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TargetAttributesHandlerImpl(
    private val sharedPreferences: SharedPreferencesWithObject,
    private val keys: SharedPreferencesKeys,
    private val timeWindows: UFServiceConfigurationV2.TimeWindows,
): TargetAttributesHandler {
    private val systemUpdateType: SystemUpdateType = SystemUpdateType.getSystemUpdateType()

    override fun saveConfigurationTargetAttributes(targetAttributes: Map<String, String>) =
        saveMap(keys.sharedPreferencesTargetAttributesFromConfiguration, targetAttributes)


    override fun saveAddTargetAttributes(targetAttributes: Map<String, String>, merge:Boolean){
        if(merge){
            getAddTargetAttributes().apply {
                putAll(targetAttributes)
            }
        } else {
            targetAttributes
        }.let {
            saveMap(keys.sharedPreferencesAddTargetAttributes, it)
        }
    }

    override fun getConfigurationTargetAttributes(): MutableMap<String, String> =
        getMap (keys.sharedPreferencesTargetAttributesFromConfiguration)
            .toMutableMap()

    override fun newConfigDataProvider(): ConfigDataProvider {
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

    private fun saveMap(key:String, map:Map<String,String>) =
        sharedPreferences.putAndCommitObject(key, map)

    private fun getMap(key:String):MutableMap<String,String>{
        val map: MutableMap<String, String>? = sharedPreferences.getObject(key)
        return map ?: mutableMapOf()
    }
    private fun mergeThirdPartyAppTargetAttributes():MutableMap<String,String>{
        return getConfigurationTargetAttributes()
            .apply {
                putAll(getAddTargetAttributes())
            }.filterNot { (key,_) -> key.startsWith("UF_", true)}
            .toMutableMap()
    }

    private fun getAddTargetAttributes():MutableMap<String, String> =
        getMap (keys.sharedPreferencesAddTargetAttributes)

    private fun decorateTargetAttribute(): Map<String, String> {
        val targetAttributes = mergeThirdPartyAppTargetAttributes()
        targetAttributes[CLIENT_TYPE_TARGET_TOKEN_KEY] = "Android"
        targetAttributes[CLIENT_VERSION_TARGET_ATTRIBUTE_KEY] = BuildConfig.VERSION_NAME
        targetAttributes[CLIENT_VERSION_CODE_ATTRIBUTE_KEY] = BuildConfig.VERSION_CODE.toString()
        val buildDate = Date(Build.TIME)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.UK)
        targetAttributes[ANDROID_BUILD_DATE_TARGET_ATTRIBUTE_KEY] = dateFormat.format(buildDate)
        targetAttributes[ANDROID_BUILD_TYPE_TARGET_ATTRIBUTE_KEY] = Build.TYPE
        targetAttributes[ANDROID_FINGERPRINT_TARGET_ATTRIBUTE_KEY] = Build.FINGERPRINT
        targetAttributes[ANDROID_KEYS_TARGET_ATTRIBUTE_KEY] = Build.TAGS
        targetAttributes[ANDROID_VERSION_TARGET_ATTRIBUTE_KEY] = Build.VERSION.RELEASE
        targetAttributes[ANDROID_API_LEVEL_TARGET_ATTRIBUTE_KEY] = Build.VERSION.SDK_INT.toString()
        targetAttributes[DEVICE_NAME_TARGET_ATTRIBUTE_KEY] = Build.DEVICE
        targetAttributes[SYSTEM_UPDATE_TYPE] = systemUpdateType.name
        targetAttributes[DEVICE_TIME_ZONE_TARGET_ATTRIBUTE_KEY] = TimeZone.getDefault().id
        targetAttributes[DEVICE_UTC_TARGET_ATTRIBUTE_KEY] = "${OffsetDateTime.now().offset}"
        targetAttributes[TIME_WINDOWS_CRON_EXPRESSION_ATTRIBUTE_KEY] = timeWindows.cronExpression
        targetAttributes[TIME_WINDOWS_DURATION_ATTRIBUTE_KEY] = timeWindows.duration.toDuration(
            DurationUnit.SECONDS
        ).toString()
        return targetAttributes
    }

    private fun Map<String, String>.toMD5(): String {
        return entries
            .sortedBy { it.key }
            .joinToString("-") { "${it.key}_${it.value}" }
            .toMD5()
    }

    companion object {
        private const val LAST_TARGET_ATTRIBUTES_MD5_SENT_KEY = "LAST_TARGET_ATTRIBUTES_MD5_SET_KEY"
        private const val CLIENT_VERSION_TARGET_ATTRIBUTE_KEY = "client_version"
        private const val CLIENT_VERSION_CODE_ATTRIBUTE_KEY = "client_version_code"
        private const val ANDROID_BUILD_DATE_TARGET_ATTRIBUTE_KEY = "android_build_date"
        private const val ANDROID_BUILD_TYPE_TARGET_ATTRIBUTE_KEY = "android_build_type"
        private const val ANDROID_FINGERPRINT_TARGET_ATTRIBUTE_KEY = "android_fingerprint"
        private const val ANDROID_KEYS_TARGET_ATTRIBUTE_KEY = "android_keys"
        private const val ANDROID_VERSION_TARGET_ATTRIBUTE_KEY = "android_version"
        private const val ANDROID_API_LEVEL_TARGET_ATTRIBUTE_KEY = "android_api_level"
        private const val DEVICE_NAME_TARGET_ATTRIBUTE_KEY = "device_name"
        private const val SYSTEM_UPDATE_TYPE = "system_update_type"
        private const val DEVICE_UTC_TARGET_ATTRIBUTE_KEY = "utc_offset"
        private const val DEVICE_TIME_ZONE_TARGET_ATTRIBUTE_KEY = "time_zone"
        private const val TIME_WINDOWS_CRON_EXPRESSION_ATTRIBUTE_KEY = "time_windows_cron_expression"
        private const val TIME_WINDOWS_DURATION_ATTRIBUTE_KEY = "time_windows_duration"
        private const val CLIENT_TYPE_TARGET_TOKEN_KEY = "client"
    }
}