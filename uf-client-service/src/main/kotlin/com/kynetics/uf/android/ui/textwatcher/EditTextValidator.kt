/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.ui.textwatcher

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

abstract class EditTextValidator(private val editText: EditText) : TextWatcher {

    abstract fun validate(text: String):Result<Unit>

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    final override fun afterTextChanged(s: Editable?) {
        validate(s?.toString() ?: "")
            .onFailure { error ->
                editText.error = error.message
            }
    }
}