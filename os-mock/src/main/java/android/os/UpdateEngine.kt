/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
@file:Suppress("UNUSED_PARAMETER", "unused", "KDocUnresolvedReference")

package android.os

class UpdateEngine {
    /**
     * Error codes from update engine upon finishing a call to
     * [applyPayload]. Values will be passed via the callback function
     * [UpdateEngineCallback.onPayloadApplicationComplete]. Values must
     * agree with the ones in `system/update_engine/common/error_code.h`.
     */
    object ErrorCodeConstants {
        /**
         * Error code: a request finished successfully.
         */
        const val SUCCESS = 0

        /**
         * Error code: a request failed due to a generic error.
         */
        const val ERROR = 1

        /**
         * Error code: an update failed to apply due to filesystem copier
         * error.
         */
        const val FILESYSTEM_COPIER_ERROR = 4

        /**
         * Error code: an update failed to apply due to an error in running
         * post-install hooks.
         */
        const val POST_INSTALL_RUNNER_ERROR = 5

        /**
         * Error code: an update failed to apply due to a mismatching payload.
         *
         *
         * For example, the given payload uses a feature that's not
         * supported by the current update engine.
         */
        const val PAYLOAD_MISMATCHED_TYPE_ERROR = 6

        /**
         * Error code: an update failed to apply due to an error in opening
         * devices.
         */
        const val INSTALL_DEVICE_OPEN_ERROR = 7

        /**
         * Error code: an update failed to apply due to an error in opening
         * kernel device.
         */
        const val KERNEL_DEVICE_OPEN_ERROR = 8

        /**
         * Error code: an update failed to apply due to an error in fetching
         * the payload.
         *
         *
         * For example, this could be a result of bad network connection
         * when streaming an update.
         */
        const val DOWNLOAD_TRANSFER_ERROR = 9

        /**
         * Error code: an update failed to apply due to a mismatch in payload
         * hash.
         *
         *
         * Update engine does validity checks for the given payload and its
         * metadata.
         */
        const val PAYLOAD_HASH_MISMATCH_ERROR = 10

        /**
         * Error code: an update failed to apply due to a mismatch in payload
         * size.
         */
        const val PAYLOAD_SIZE_MISMATCH_ERROR = 11

        /**
         * Error code: an update failed to apply due to failing to verify
         * payload signatures.
         */
        const val DOWNLOAD_PAYLOAD_VERIFICATION_ERROR = 12

        /**
         * Error code: an update failed to apply due to a downgrade in payload
         * timestamp.
         *
         *
         * The timestamp of a build is encoded into the payload, which will
         * be enforced during install to prevent downgrading a device.
         */
        const val PAYLOAD_TIMESTAMP_ERROR = 51

        /**
         * Error code: an update has been applied successfully but the new slot
         * hasn't been set to active.
         *
         *
         * It indicates a successful finish of calling [.applyPayload] with
         * `SWITCH_SLOT_ON_REBOOT=0`. See [.applyPayload].
         */
        const val UPDATED_BUT_NOT_ACTIVE = 52

        /**
         * Error code: there is not enough space on the device to apply the update. User should
         * be prompted to free up space and re-try the update.
         *
         *
         * See [UpdateEngine.allocateSpace].
         */
        const val NOT_ENOUGH_SPACE = 60

        /**
         * Error code: the device is corrupted and no further updates may be applied.
         *
         *
         * See [UpdateEngine.cleanupAppliedPayload].
         */
        const val DEVICE_CORRUPTED = 61
    }
    /**
     * Status codes for update engine. Values must agree with the ones in
     * `system/update_engine/client_library/include/update_engine/update_status.h`.
     */
    object UpdateStatusConstants {
        /**
         * Update status code: update engine is in idle state.
         */
        const val IDLE = 0

        /**
         * Update status code: update engine is checking for update.
         */
        const val CHECKING_FOR_UPDATE = 1

        /**
         * Update status code: an update is available.
         */
        const val UPDATE_AVAILABLE = 2

        /**
         * Update status code: update engine is downloading an update.
         */
        const val DOWNLOADING = 3

        /**
         * Update status code: update engine is verifying an update.
         */
        const val VERIFYING = 4

        /**
         * Update status code: update engine is finalizing an update.
         */
        const val FINALIZING = 5

        /**
         * Update status code: an update has been applied and is pending for
         * reboot.
         */
        const val UPDATED_NEED_REBOOT = 6

        /**
         * Update status code: update engine is reporting an error event.
         */
        const val REPORTING_ERROR_EVENT = 7

        /**
         * Update status code: update engine is attempting to rollback an
         * update.
         */
        const val ATTEMPTING_ROLLBACK = 8

        /**
         * Update status code: update engine is in disabled state.
         */
        const val DISABLED = 9
    }

    fun bind(callback: UpdateEngineCallback):Boolean{
        TODO("")
    }

    fun applyPayload(url:String, offset:Long, size:Long, headerKeyValuePairs:Array<String>){
        TODO("")
    }

    fun unbind():Boolean{
        TODO("")
    }
}