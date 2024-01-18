/*
 * Copyright © 2017-2024  Kynetics  LLC
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.cron

import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.android.communication.messenger.MessengerHandler

fun CronScheduler.Status.notifyScheduleStatus(){
    when(this){
        is CronScheduler.Status.Scheduled ->
            MessengerHandler.notifyMessage(UFServiceMessageV1.State.WaitingUpdateWindow(this.seconds))
        is CronScheduler.Status.Error ->{
            MessengerHandler.notifyMessage(UFServiceMessageV1.State.WaitingUpdateWindow(-1))
            MessengerHandler.notifyMessage(UFServiceMessageV1.Event.Error(this.details))
        }
    }
}