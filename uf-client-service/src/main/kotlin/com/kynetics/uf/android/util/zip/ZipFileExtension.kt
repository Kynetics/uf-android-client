/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.util.zip

import android.util.Log
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private val TAG = ZipFile::class.java.simpleName

fun ZipFile.getEntryOffset(name:String): Long {
    val zipEntries = entries()
    var offset: Long = 0
    while (zipEntries.hasMoreElements()) {
        val entry = zipEntries.nextElement()
        offset += entry.getHeaderSize()
        if (entry.name == name) {
            return offset
        }
        offset += entry.compressedSize
    }
    Log.e(TAG, "Entry $name not found")
    throw IllegalArgumentException("The given entry was not found")
}

fun ZipEntry.getHeaderSize(): Long {
    // Each entry has an header of (30 + n + m) bytes
    // 'n' is the length of the file name
    // 'm' is the length of the extra field
    val fixedHeaderSize = 30L
    val n = name.length
    val m = extra?.size ?: 0
    return fixedHeaderSize + n + m
}