/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.update.application

import android.content.Context
import android.os.Build
import android.util.Log
import com.kynetics.uf.android.api.UFServiceInfo
import com.kynetics.uf.android.update.AndroidUpdater
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.uf.android.update.application.ApkAnalyzer.getPackageFromApk
import com.kynetics.uf.android.update.application.ApkAnalyzer.getVersionFromApk
import org.eclipse.hara.ddiclient.api.Updater

class ApkUpdater(context: Context) : AndroidUpdater(context) {

    companion object {
        val TAG: String = ApkUpdater::class.java.simpleName
        const val TIMEOUT_LIMIT = 1800L
    }

    override fun requiredSoftwareModulesAndPriority(swModules: Set<Updater.SwModule>): Updater.SwModsApplication {

        return Updater.SwModsApplication(1,
            swModules
                .filter { it.type == "bApp" }
                .map { Updater.SwModsApplication.SwModule(
                    it.type,
                    it.name,
                    it.version,
                    it.artifacts
                        .filter { a -> a.filename.endsWith(".apk", true) }
                        .map { a -> a.hashes }
                        .toSet())
                }.toSet())
    }

    override fun applyUpdate(
        modules: Set<Updater.SwModuleWithPath>,
        messenger: Updater.Messenger
    ): Updater.UpdateResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val errorMessage = "Installation of apk is not supported from device with android system " +
                "com.kynetics.uf.android.api lower than ${Build.VERSION_CODES.LOLLIPOP} " +
                "(current is ${Build.VERSION.SDK_INT})"
            messenger.sendMessageToServer(errorMessage)
            Log.w(TAG, errorMessage)
            return Updater.UpdateResult(
                success = false,
                details = listOf(errorMessage)
            )
        }

        val currentUpdateState = CurrentUpdateState(context)

        modules.flatMap { it.artifacts }
            .filter { !currentUpdateState.isPackageInstallationTerminated(
                getPackageFromApk(context, it.path), getVersionFromApk(context, it.path))
            }.sortedWith { artifact1, artifact2 ->
                val pk1 = getPackageFromApk(context, artifact1.path) ?: ""
                val pk2 = getPackageFromApk(context, artifact2.path) ?: ""
                when {
                    pk1 == UFServiceInfo.SERVICE_PACKAGE_NAME -> -1
                    pk2 == UFServiceInfo.SERVICE_PACKAGE_NAME -> 1
                    else -> artifact1.filename.compareTo(artifact2.filename)
                }
            }.forEach { a ->
                Log.d(TAG, "install artifact ${a.filename} (${ getPackageFromApk(context, a.path) }) from file ${a.path}")
                try {
                    ApkInstaller.install(a, currentUpdateState, messenger, context)
                } catch (t: Throwable) { // new client replace with IOException | IllegalArgumentException e
                    val error = "${a.filename} installation fails with error ${t.message}"
                    currentUpdateState.addErrorToReport(error)
                    currentUpdateState.packageInstallationTerminated(
                        getPackageFromApk(context, a.path),
                        getVersionFromApk(context, a.path))
                    Log.d(TAG, "Failed to install ${a.filename}")
                    Log.d(TAG, t.message, t)
                }
            }
        val details = currentUpdateState.distributionReportError + currentUpdateState.distributionReportSuccess
        return Updater.UpdateResult(success = currentUpdateState.distributionReportError.isEmpty(),
            details = details.toList())
    }
}
