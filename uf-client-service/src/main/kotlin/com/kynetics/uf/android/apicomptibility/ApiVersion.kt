/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.kynetics.uf.android.apicomptibility

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * @author Daniele Sergio
 */
enum class ApiVersion {
    PRE_O, POST_O {
        @RequiresApi(api = Build.VERSION_CODES.O)
        override fun startService(context: Context, intent: Intent?) {
            context.startForegroundService(intent)
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        override fun configureChannel(channelId: String?, channelName: String?, notificationManager: NotificationManager) {
            val channel = NotificationChannel(channelId, channelName,
                    NotificationManager.IMPORTANCE_DEFAULT)
            channel.setShowBadge(false)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }
    };

    open fun startService(context: Context, intent: Intent?) {
        context.startService(intent)
    }

    open fun configureChannel(channelId: String?, channelName: String?, notificationManager: NotificationManager) {}

    companion object {
        fun fromVersionCode(): ApiVersion {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) POST_O else PRE_O
        }
    }
}