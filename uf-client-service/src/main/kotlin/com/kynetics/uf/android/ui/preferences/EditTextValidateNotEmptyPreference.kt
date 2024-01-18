/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.ui.preferences

import android.content.Context
import android.util.AttributeSet

class EditTextValidateNotEmptyPreference(
    context: Context,
    attrs: AttributeSet
) : EditTextValidatePreference(context, attrs) {

    override fun validateText(text: String): Result<Unit> {
        return if (text.trim { it <= ' ' } == "") {
            Result.failure(IllegalArgumentException("Filed can't be empty"))
        } else {
            Result.success(Unit)
        }
    }
}