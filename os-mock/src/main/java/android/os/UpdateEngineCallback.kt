/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package android.os

abstract class UpdateEngineCallback {
    /**
     * Invoked when anything changes. The value of `status` will
     * be one of the values from [UpdateEngine.UpdateStatusConstants],
     * and `percent` will be valid [TODO: in which cases?].
     */
    abstract fun onStatusUpdate(status: Int, percent: Float)

    /**
     * Invoked when the payload has been applied, whether successfully or
     * unsuccessfully. The value of `errorCode` will be one of the
     * values from [UpdateEngine.ErrorCodeConstants].
     */
    abstract fun onPayloadApplicationComplete(error: Int)
}