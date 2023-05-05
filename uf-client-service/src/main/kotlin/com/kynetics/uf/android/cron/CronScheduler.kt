/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.cron

import android.util.Log
import com.cronutils.model.time.ExecutionTime
import com.kynetics.uf.android.api.UFServiceConfigurationV2
import kotlinx.coroutines.*
import java.time.ZonedDateTime
import java.util.*

object CronScheduler {
    private val TAG:String = CronScheduler::class.java.simpleName

    private var scheduledJobs: MutableMap<String, ScheduledJob> = mutableMapOf()

    private class ScheduledJob(
        val job:Job,
        val timeWindows: UFServiceConfigurationV2.TimeWindows,
        val action: () -> Unit)

    sealed interface Status{
        data class Error(val details:List<String>):Status
        data class Scheduled(val seconds:Long):Status
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    fun reschedule(tag:String):Status?{
        Log.i(TAG, "Rescheduling job[$tag]")
        return scheduledJobs[tag]?.run {
            removeScheduledJob(tag)
            schedule(tag, timeWindows, action)
        }
    }

    fun schedule(tag:String,
                 timeWindows: UFServiceConfigurationV2.TimeWindows,
                 action:()->Unit):Status{
        with(ExecutionTime.forCron(HaraCronParser.parse(timeWindows.cronExpression))){
            val now: ZonedDateTime = ZonedDateTime.now()
            val lastExecution = lastExecution(now)
            val nextExecution = timeToNextExecution(now)
            scheduledJobs[tag]?.job?.cancel()
            return when{
                isInTimeWindows(now, lastExecution, timeWindows) ->{
                    action()
                    Status.Scheduled(0)
                }

                nextExecution.isPresent -> {
                    runCatching{
                        val delay = nextExecution.get().toMillis()
                        scheduledJobs[tag] = scope.launch{
                            try{
                                delay(delay)
                                action()
                            } finally {
                                Log.i(TAG, "Scheduled job[$tag] stopped")
                            }
                        }.run { ScheduledJob(this, timeWindows, action) }
                        Log.i(TAG, "Job[$tag] scheduled will start in ${nextExecution.get().seconds} seconds")
                        Status.Scheduled(nextExecution.get().seconds)
                    }.onFailure { exception ->
                        Log.w(TAG, "Error on scheduling next job[$tag]", exception)
                        return Status.Error(listOf("Error on scheduling next job", exception.message?:""))
                    }.getOrThrow()
                }

                else -> {
                    Log.w(TAG, "Next execution not exist")
                    Status.Error(listOf("Next update windows not exist", "Check the cron expression"))
                }
            }
        }
    }

    fun removeScheduledJob(tag:String) = scheduledJobs.remove(tag)?.job?.cancel()

    private fun isInTimeWindows(now:ZonedDateTime,
                                lastExecution: Optional<ZonedDateTime>,
                                timeWindows: UFServiceConfigurationV2.TimeWindows):Boolean{
        return lastExecution.isPresent &&
                now.isAfter(lastExecution.get()) &&
                now.isBefore(lastExecution.get().plusSeconds(timeWindows.duration))
    }
}