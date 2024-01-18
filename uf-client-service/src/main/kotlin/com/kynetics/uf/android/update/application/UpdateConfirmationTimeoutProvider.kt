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

import java.io.File
import java.util.concurrent.TimeUnit

interface UpdateConfirmationTimeoutProvider {
    class Timeout(val value: Long, val timeUnit: TimeUnit)

    fun getTimeout(files: List<File?>?): Timeout
    class FixedTimeProvider private constructor(private val timeout: Long) :
        UpdateConfirmationTimeoutProvider {
        override fun getTimeout(files: List<File?>?): Timeout {
            return Timeout(timeout, TimeUnit.SECONDS)
        }

        companion object {
            fun ofSeconds(seconds: Long): UpdateConfirmationTimeoutProvider {
                return FixedTimeProvider(seconds)
            }
        }

    }
}