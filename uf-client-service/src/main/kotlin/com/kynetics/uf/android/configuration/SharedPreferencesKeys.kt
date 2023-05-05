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

import android.content.Context
import com.kynetics.uf.android.R

class SharedPreferencesKeys private constructor(context: Context){
    val sharedPreferencesServerUrlKey:String = context.getString(R.string.shared_preferences_server_url_key)
    val sharedPreferencesApiModeKey:String = context.getString(R.string.shared_preferences_api_mode_key)
    val sharedPreferencesTenantKey:String = context.getString(R.string.shared_preferences_tenant_key)
    val sharedPreferencesControllerIdKey:String = context.getString(R.string.shared_preferences_controller_id_key)
    val sharedPreferencesServiceEnableKey:String = context.getString(R.string.shared_preferences_is_enable_key)
    val sharedPreferencesGatewayToken:String = context.getString(R.string.shared_preferences_gateway_token_key)
    val sharedPreferencesTargetToken:String = context.getString(R.string.shared_preferences_target_token_key)
    val sharedPreferencesTargetTokenReceivedFromServer:String = context.getString(R.string.shared_preferences_target_token_received_from_server_key)
    val sharedPreferencesIsUpdateFactoryServerType:String = context.getString(R.string.shared_preferences_is_update_factory_server_type_key)
    val sharedPreferencesCronExpression:String = context.getString(R.string.shared_preferences_time_windows_cron_expression_key)
    val sharedPreferencesTimeWindowsDuration:String = context.getString(R.string.shared_preferences_time_windows_duration_key)
    val sharedPreferencesTargetAttributesFromConfiguration:String = context.getString(R.string.shared_preferences_args_key)
    val sharedPreferencesAddTargetAttributes:String = context.getString(R.string.shared_preferences_add_target_attributes_key)

    companion object{
        private var instance: SharedPreferencesKeys? = null

        fun getInstance(context: Context): SharedPreferencesKeys {
            if(instance == null){
                instance = SharedPreferencesKeys(context)
            }
          return instance!!
        }
    }
}