/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.kynetics.uf.android.content

import android.content.SharedPreferences
import java.io.Serializable

interface SharedPreferencesWithObject:SharedPreferences {
    fun <T : Serializable?> getObject(objKey: String?): T?

    fun <T : Serializable?> getObject(objKey: String?, defaultObj: T?): T?

    fun <T> putAndCommitObject(key: String?, obj: T)
}