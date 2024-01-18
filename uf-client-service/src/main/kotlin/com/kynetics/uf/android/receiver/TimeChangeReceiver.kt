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
import com.kynetics.uf.android.client.RestartableClientService.Companion.CRON_TAG
import com.kynetics.uf.android.cron.CronScheduler
import com.kynetics.uf.android.cron.notifyScheduleStatus

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (Intent.ACTION_TIME_CHANGED == action || Intent.ACTION_TIMEZONE_CHANGED == action) {
            Log.i(TAG, "Time or Time zone is changed")

            CronScheduler.reschedule(CRON_TAG)?.run {
                Log.i(TAG, "Rescheduled job with result: $this")
                notifyScheduleStatus()
            }
        }
    }

    companion object {
        private val TAG = TimeChangeReceiver::class.java.simpleName
    }
}
