/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.update.system

import android.content.Context
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.uf.android.update.Installer
import org.eclipse.hara.ddiclient.api.Updater

interface OtaInstaller : Installer<CurrentUpdateState.InstallationResult> {
    fun isFeedbackReliable(context: Context): Boolean = true
    fun onComplete(context: Context, messenger: Updater.Messenger, result: CurrentUpdateState.InstallationResult): Unit = Unit
}
