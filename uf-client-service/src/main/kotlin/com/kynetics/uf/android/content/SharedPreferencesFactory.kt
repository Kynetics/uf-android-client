/*
 * Copyright © 2017-2023  Kynetics  LLC
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.content

import android.content.Context
import com.kynetics.uf.android.R

object SharedPreferencesFactory {

    fun get(context: Context, name:String?, mode:Int):SharedPreferencesWithObject{
        return UFSharedPreferences(
            SharedPreferencesWithObjectImpl(context.getSharedPreferences(name, mode)),
            EncryptedSharedPreferences.get(context),
            arrayOf(context.getString(R.string.shared_preferences_gateway_token_key),
                context.getString(R.string.shared_preferences_target_token_key),
                context.getString(R.string.shared_preferences_target_token_received_from_server_key)))

    }
}