/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kynetics.uf.android.BuildConfig
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.update.CurrentUpdateState

class StartServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) {
            return
        }
        val action = intent.action
        val ufServiceIsUpdated = Intent.ACTION_MY_PACKAGE_REPLACED == action
        if (ufServiceIsUpdated) {
            Log.d(TAG, "Uf service is updated")
            CurrentUpdateState(context).packageInstallationTerminated(
                BuildConfig.APPLICATION_ID,
                BuildConfig.VERSION_CODE.toLong()
            )
        }
        if (ufServiceIsUpdated || Intent.ACTION_BOOT_COMPLETED == action) {
            UpdateFactoryService.startService(context)
        }
    }

    companion object {
        private val TAG = StartServiceReceiver::class.java.simpleName
    }
}
