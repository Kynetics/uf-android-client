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
import android.os.Build
import android.os.PowerManager
import android.os.UpdateEngine
import android.os.UpdateEngineCallback
import android.util.Log
import androidx.annotation.RequiresApi
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.uf.android.util.zip.getEntryOffset
import org.eclipse.hara.ddiclient.api.Updater
import java.util.concurrent.*
import java.util.zip.ZipFile
import kotlin.math.min
import kotlin.streams.toList

@RequiresApi(Build.VERSION_CODES.O)
internal object ABOtaInstaller : OtaInstaller {

    val TAG: String = ABOtaInstaller::class.java.simpleName
    private const val PROPERTY_FILE = "payload_properties.txt"
    private const val PAYLOAD_FILE = "payload.bin"
    private val UPDATE_STATUS = mapOf(
        UpdateEngine.UpdateStatusConstants.IDLE to "Android Update Engine: Idle",
        UpdateEngine.UpdateStatusConstants.CHECKING_FOR_UPDATE to "Android Update Engine: Checking for update",
        UpdateEngine.UpdateStatusConstants.UPDATE_AVAILABLE to "Android Update Engine: Update available",
        UpdateEngine.UpdateStatusConstants.DOWNLOADING to "Android Update Engine: Copying file",
        UpdateEngine.UpdateStatusConstants.VERIFYING to "Android Update Engine: Verifying",
        UpdateEngine.UpdateStatusConstants.FINALIZING to "Android Update Engine: Finalizing",
        UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT to "Android Update Engine: Rebooting",
        UpdateEngine.UpdateStatusConstants.REPORTING_ERROR_EVENT to "Android Update Engine: Reporting error event",
        UpdateEngine.UpdateStatusConstants.ATTEMPTING_ROLLBACK to "Android Update Engine: Attempting rollback",
        UpdateEngine.UpdateStatusConstants.DISABLED to "Android Update Engine: Disable")

    @Suppress("MagicNumber")
    private val errorCodeToDescription = mapOf(
        0 to "Success",
        1 to "Error",
        2 to "Omaha request error",
        3 to "Omaha response handler error",
        4 to "Filesystem copier error",
        5 to "Postinstall runner error",
        6 to "Payload mismatched type",
        7 to "Install device open error",
        8 to "Kernel device open error",
        9 to "Download transfer error",
        10 to "Payload hash mismatch error",
        11 to "Payload size mismatch error",
        12 to "Download payload verification error",
        13 to "Download new partition info error",
        14 to "Download write error",
        15 to "New rootfs verification error",
        16 to "New kernel verification error",
        17 to "Signed delta payload expected error",
        18 to "Download payload pub key verification error",
        19 to "Postinstall booted from firmware b",
        20 to "Download state initialization error",
        21 to "Download invalid metadata magic string",
        22 to "Download signature missing in manifest",
        23 to "Download manifest parse error",
        24 to "Download metadata signature error",
        25 to "Download metadata signature verification error",
        26 to "Download metadata signature mismatch",
        27 to "Download operation hash verification error",
        28 to "Download operation execution error",
        29 to "Download operation hash mismatch",
        30 to "Omaha request empty response error",
        31 to "Omaha request xmlparse error",
        32 to "Download invalid metadata size",
        33 to "Download invalid metadata signature",
        34 to "Omaha response invalid",
        35 to "Omaha update ignored per policy",
        36 to "Omaha update deferred per policy",
        37 to "Omaha error in httpresponse",
        38 to "Download operation hash missing error",
        39 to "Download metadata signature missing error",
        40 to "Omaha update deferred for backoff",
        41 to "Postinstall powerwash error",
        42 to "Update canceled by channel change",
        43 to "Postinstall firmware ronot updatable",
        44 to "Unsupported major payload version",
        45 to "Unsupported minor payload version",
        46 to "Omaha request xmlhas entity decl",
        47 to "Filesystem verifier error",
        48 to "User canceled",
        49 to "Non critical update in oobe",
        50 to "Omaha update ignored over cellular",
        51 to "Payload timestamp error",
        52 to "Updated but not active")

    private const val HOURS_TIMEOUT_FOR_UPDATE = 3L

    private val updateEngine: UpdateEngine = UpdateEngine()

    private fun isOtaMalformed(artifact: Updater.SwModuleWithPath.Artifact): Boolean {
        val zipFile = ZipFile(artifact.path)
        val payloadEntry = zipFile.getEntry(PAYLOAD_FILE)
        val propEntry = zipFile.getEntry(PROPERTY_FILE)
        return payloadEntry == null || propEntry == null
    }

    override fun install(
        artifact: Updater.SwModuleWithPath.Artifact,
        currentUpdateState: CurrentUpdateState,
        messenger: Updater.Messenger,
        context: Context
    ): CurrentUpdateState.InstallationResult {
        val updateStatus = CompletableFuture<Int>()

        val abInstallationPending = currentUpdateState.isABInstallationPending(artifact)
        val isDeviceFailedToRebootOrUFServiceCrashed =
            currentUpdateState.isDeviceFailedToRebootOrUFServiceCrashed()
        val deviceRebootedWhileUpdating = currentUpdateState.isDeviceRebootedAfterUpdateStarted(context)
        return when {
            abInstallationPending && !deviceRebootedWhileUpdating &&
                    isDeviceFailedToRebootOrUFServiceCrashed -> {
                updateEngine.bind(
                    MyUpdateEngineCallback(
                        context,
                        messenger,
                        updateStatus, currentUpdateState
                    )
                )
                installationResult(updateStatus, messenger, artifact)
            }
            abInstallationPending && !isDeviceFailedToRebootOrUFServiceCrashed -> {
                val result = currentUpdateState.lastABInstallationResult()
                val message = "Installation result of Ota named ${artifact.filename} is " +
                        if (result is CurrentUpdateState.InstallationResult.Success) "success" else "failure"
                messenger.sendMessageToServer(message + result.details)
                Log.i(TAG, message)
                result
            }
            isOtaMalformed(artifact) -> {
                Log.d(TAG, "Malformed AB ota")
                CurrentUpdateState.InstallationResult.Error(
                    listOf(
                        "Malformed ota for AB update.",
                        "An AB ota update must contain a payload file named $PAYLOAD_FILE and a property file named " +
                                PROPERTY_FILE
                    )
                )
            }
            else -> {
                currentUpdateState.saveSlotName()
                val zipFile = ZipFile(artifact.path)

                val prop = zipFile.getInputStream(zipFile.getEntry(PROPERTY_FILE))
                    .bufferedReader().lines().toList().toTypedArray()

                Log.d(TAG, prop.joinToString())

                updateEngine.bind(
                    MyUpdateEngineCallback(
                        context,
                        messenger,
                        updateStatus, currentUpdateState
                    )
                )
                currentUpdateState.storeDeviceBootCount(context)
                currentUpdateState.addPendingABInstallation(artifact)
                currentUpdateState.addUFServiceVersionThatStartedTheUpdate()
                messenger.sendMessageToServer(
                    "Applying A/B ota update (${artifact.filename})...")
                val zipPath = "file://${artifact.path}"
                Log.d(TAG, zipPath)
                updateEngine.applyPayload(zipPath, zipFile.getEntryOffset(PAYLOAD_FILE), 0,
                    prop)
                return installationResult(
                    updateStatus,
                    messenger,
                    artifact
                )
            }
        }
    }

    private fun installationResult(
        updateStatus: CompletableFuture<Int>,
        messenger: Updater.Messenger,
        artifact: Updater.SwModuleWithPath.Artifact
    ): CurrentUpdateState.InstallationResult {
        return try {
            @Suppress("MagicNumber")
            val result: Int = updateStatus.get(HOURS_TIMEOUT_FOR_UPDATE, TimeUnit.HOURS)
            updateEngine.unbind()
            val messages = listOf("result: $result", errorCodeToDescription[result] ?: "")
            Log.d(TAG, "result: ${messages.joinToString(" ")}")
            when (result) {

                UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT -> {
                    Log.w(TAG, "Reboot fail, waiting for a manual reboot")
                    CountDownLatch(1).await()
                    CurrentUpdateState.InstallationResult.Error(
                        listOf(
                            "Update is successfully applied but system failed to reboot",
                            "Installation status unknown"
                        )
                    )
                }

                UpdateEngine.ErrorCodeConstants.UPDATED_BUT_NOT_ACTIVE -> {
                    messenger.sendMessageToServer(
                        "Update is successfully applied but system failed to reboot")
                    CurrentUpdateState.InstallationResult.Success()
                }

                UpdateEngine.ErrorCodeConstants.SUCCESS -> CurrentUpdateState.InstallationResult.Success()

                else -> CurrentUpdateState.InstallationResult.Error(
                    messages
                )
            }
        } catch (e: Throwable) {
            when (e) {
                is TimeoutException -> {
                    val messages = listOf(
                        "Time to update exceeds the timeout",
                        "Package manager timeout expired, package installation status unknown"
                    )
                    CurrentUpdateState.InstallationResult.Error(
                        messages
                    )
                }

                else -> {
                    Log.w(TAG, "Exception on apply AB update (${artifact.filename})", e)
                    CurrentUpdateState.InstallationResult.Error(
                        listOf("error: ${e.message}")
                    )
                }
            }
        }
    }

    private class MyUpdateEngineCallback(
        private val context: Context,
        private val messenger: Updater.Messenger,
        private val updateStatus: CompletableFuture<Int>,
        private val currentUpdateState: CurrentUpdateState
    ) : UpdateEngineCallback() {

        companion object {
            private const val MAX_MESSAGES_PER_PHASE = 10
            private const val UPDATE_ENGINE_STATUS_CLEAN_UP = 11
        }

        private var previousState = Int.MAX_VALUE
        private val queue = ArrayBlockingQueue<Double>(MAX_MESSAGES_PER_PHASE, true)

        override fun onStatusUpdate(status: Int, percent: Float) { // i==status  v==percent
            Log.d(TAG, "status:$status")
            Log.d(TAG, "percent:$percent")
            val currentPhaseProgress = min(percent.toDouble(), 100.0)
            val newPhase = previousState != status
            if (newPhase) {
                previousState = status
                if (status == UPDATE_ENGINE_STATUS_CLEAN_UP) {
                    Log.d(TAG, "Android Update Engine: Clean up previous update")
                    return
                }
                messenger.sendMessageToServer(UPDATE_STATUS.getValue(status))
                queue.clear()
                queue.addAll(
                    (1 until MAX_MESSAGES_PER_PHASE).map { it.toDouble() / MAX_MESSAGES_PER_PHASE })
            }

            val limit = queue.peek() ?: 1.0
            if (currentPhaseProgress > limit || currentPhaseProgress == 1.0 || newPhase) {
                MessengerHandler.notifyMessage(UFServiceMessageV1.Event.UpdateProgress(
                    phaseName = UPDATE_STATUS.getValue(status),
                    percentage = currentPhaseProgress
                ))

                while (currentPhaseProgress >= (queue.peek() ?: 1.0) && queue.isNotEmpty()) {
                    queue.poll()
                }
            }

            if (status == UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
                currentUpdateState.setDeviceRebootedOnABInstallation()
                pm!!.reboot(null)
                Log.w(TAG, "Reboot fail")
                messenger.sendMessageToServer(
                    "Update is successfully applied but system failed to reboot",
                    "Waiting manual reboot")
                updateStatus.complete(UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT)
            }
        }

        override fun onPayloadApplicationComplete(error: Int) {
            Log.d(
                TAG,
                "onPayloadApplicationComplete: $error"
            )
            updateStatus.complete(error)
        }
    }
}
