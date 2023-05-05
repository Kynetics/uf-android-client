/*
 * Copyright © 2017-2023  Kynetics  LLC
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.md5

import java.security.MessageDigest

fun String.toMD5(): String {
    return MessageDigest.getInstance("MD5")
        .digest(toByteArray())
        .joinToString("") {
            "%02x".format(it)
        }
}