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
import androidx.annotation.RequiresApi
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.uf.android.update.Installer
import com.kynetics.uf.android.update.application.ApkAnalyzer.getPackageFromApk
import com.kynetics.uf.android.update.application.ApkAnalyzer.getVersionFromApk
import com.kynetics.uf.android.update.application.ApkAnalyzer.verifySharedUserId
import org.eclipse.hara.ddiclient.api.Updater
import java.io.File
import java.util.concurrent.CountDownLatch

object ApkInstaller : Installer<Unit> {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun install(
        artifact: Updater.SwModuleWithPath.Artifact,
        currentUpdateState: CurrentUpdateState,
        messenger: Updater.Messenger,
        context: Context
    ) {

        val apk = File(artifact.path)

        when {
            !apk.exists() -> {
                val errorMessage = "Apk not found"
                Log.w(ApkUpdater.TAG, errorMessage)
                currentUpdateState.addErrorToRepor(errorMessage)
            }

            getPackageFromApk(context, apk.absolutePath) == null -> {
                val errorMessage = "Apk file is invalid"
                Log.w(ApkUpdater.TAG, errorMessage)
                currentUpdateState.addErrorToRepor(errorMessage)
            }

            !verifySharedUserId(context, apk.absolutePath) -> {
                val errorMessage = "Application was already installed with a different sharedUserId"
                Log.w(ApkUpdater.TAG, errorMessage)
                currentUpdateState.addErrorToRepor(errorMessage)
            }

            else -> {
                val countDownLatch = CountDownLatch(1)
                val packageName = getPackageFromApk(context, apk.absolutePath)?:""
                val packageVersion = getVersionFromApk(context, apk.absolutePath)
                val installerSession = InstallerSession.newInstance(
                    context,
                    countDownLatch,
                    packageName,
                    packageVersion,
                    artifact,
                    messenger,
                    currentUpdateState
                )

                installerSession.writeSession(apk, packageName)
                installerSession.commitSession()

                val timeout = UpdateConfirmationTimeoutProvider.FixedTimeProvider.ofSeconds(
                    ApkUpdater.TIMEOUT_LIMIT
                ).getTimeout(null)

                if (!countDownLatch.await(timeout.value, timeout.timeUnit)) {
                    currentUpdateState.addSuccessMessageToRepor("Time to update exceeds the timeout",
                        "Package manager timeout expired, package installation status unknown")
                    Log.w(ApkUpdater.TAG, "Time to update exceeds the timeout")
                }
            }
        }
    }
}
