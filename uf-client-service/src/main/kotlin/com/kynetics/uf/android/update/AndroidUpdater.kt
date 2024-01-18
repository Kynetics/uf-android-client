/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.update

import android.content.Context
import org.eclipse.hara.ddiclient.api.Updater

abstract class AndroidUpdater(protected val context: Context) : Updater {
    protected val currentUpdateState: CurrentUpdateState = CurrentUpdateState(context)

    final override fun apply(modules: Set<Updater.SwModuleWithPath>, messenger: Updater.Messenger): Updater.UpdateResult {
        currentUpdateState.startUpdate()
        return applyUpdate(modules, messenger)
    }

    abstract fun applyUpdate(modules: Set<Updater.SwModuleWithPath>, messenger: Updater.Messenger): Updater.UpdateResult

    override fun updateIsCancellable(): Boolean {
        return !currentUpdateState.isUpdateStart()
    }
}
