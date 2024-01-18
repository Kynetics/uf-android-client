/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.update.application

import android.app.PendingIntent
import android.content.*
import android.util.Log
import com.kynetics.uf.android.BuildConfig

object PackageInstallerBRHandler {
    const val ACTION_INSTALL_COMPLETE = "com.kynetics.action.INSTALL_COMPLETED"

    private var currentReceiver:BroadcastReceiver? = null

    private val intentFilter = IntentFilter(ACTION_INSTALL_COMPLETE)
    private val intent = Intent(ACTION_INSTALL_COMPLETE)
        .apply {
            setPackage(BuildConfig.APPLICATION_ID)
        }

    fun registerReceiver(context: Context, receiver: PackageInstallerBroadcastReceiver){
        unregisterReceiver(context)
        currentReceiver = receiver
        context.registerReceiver(currentReceiver, intentFilter, "", null)
    }

    fun createIntentSender(context: Context, requestCode:Int): IntentSender {
        val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        return pendingIntent.intentSender
    }

    private fun unregisterReceiver(context: Context){
        if(currentReceiver != null){
            try {
                context.unregisterReceiver(currentReceiver)
            } catch (e:Exception){
                Log.i(TAG, "Can't unregister receiver", e)
            }
        }
    }

    private val TAG = PackageInstallerBRHandler::class.java.simpleName
}