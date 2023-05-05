/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.kynetics.uf.android.content

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.util.Base64
import android.util.Base64InputStream
import android.util.Base64OutputStream
import android.util.Log
import java.io.*

/**
 * @author Daniele Sergio
 */
class SharedPreferencesWithObjectImpl(private val sharedPreferences: SharedPreferences) : SharedPreferences by sharedPreferences,
    SharedPreferencesWithObject {

    override fun <T : Serializable?> getObject(objKey: String?): T? {
        return getObject<T>(objKey, null)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Serializable?> getObject(objKey: String?, defaultObj: T?): T? {
        val bytes = sharedPreferences.getString(objKey, "")!!.toByteArray()
        if (bytes.isEmpty()) {
            return defaultObj
        }
        try {
            val byteArray = ByteArrayInputStream(bytes)
            val base64InputStream = Base64InputStream(byteArray, Base64.DEFAULT)
            val `in` = ObjectInputStream(base64InputStream)
            return `in`.readObject() as T
        } catch (ex: IOException) {
            Log.e(TAG, ex.message, ex)
        } catch (ex: ClassNotFoundException) {
            Log.e(TAG, ex.message, ex)
        }
        return defaultObj
    }

    @SuppressLint("ApplySharedPref")
    override fun <T> putAndCommitObject(key: String?, obj: T) {
        val arrayOutputStream = ByteArrayOutputStream()
        val ed = sharedPreferences.edit()
        val objectOutput: ObjectOutputStream
        try {
            objectOutput = ObjectOutputStream(arrayOutputStream)
            objectOutput.writeObject(obj)
            val data = arrayOutputStream.toByteArray()
            objectOutput.close()
            arrayOutputStream.close()
            val out = ByteArrayOutputStream()
            val b64 = Base64OutputStream(out, Base64.DEFAULT)
            b64.write(data)
            b64.close()
            out.close()
            ed.putString(key, String(out.toByteArray()))
            ed.commit()
        } catch (ex: IOException) {
            Log.e(TAG, ex.message, ex)
        }
    }

    companion object {
        private val TAG = SharedPreferencesWithObjectImpl::class.java.simpleName
    }

}