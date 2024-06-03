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

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.File
import java.security.KeyStore

object EncryptedSharedPreferences {

    private const val SHARED_PREFERENCE_FILE_NAME = "UF_SECURE_SHARED_FILE"
    private const val DEFAULT_MASTER_KEY_ALIAS = "_androidx_security_master_key_"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private val TAG = EncryptedSharedPreferences::class.java.simpleName

    private val masterKeyAlias: String
        get() = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    fun get(context: Context): SharedPreferences {
        return openKeystoreWithDefaultMasterKey(context)
    }

    private fun openKeystoreWithDefaultMasterKey(context: Context): SharedPreferences {
        return getSharedPreferencesOrNull(context) ?: run {

            Log.d(TAG, "Cannot retrieve encrypted preferences. Deleting and recreating.")
            resetEncryptedSharedPreferences(context)
            
            //Creating the shared preferences again
            getSharedPreferencesOrNull(context)
        } ?: throw IllegalStateException("Cannot create encrypted shared preferences")
    }

    private fun getSharedPreferencesOrNull(context: Context): SharedPreferences? {
        return try {
            EncryptedSharedPreferences
                .create(
                    SHARED_PREFERENCE_FILE_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to create encrypted shared preferences", e)
            null
        }
    }

    private fun resetEncryptedSharedPreferences(context: Context) {
        deleteMasterKey()
        clearEncryptedSharedPreferences(context)
        deleteEncryptedSharedPreferencesFile(context)
    }

    private fun deleteMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.deleteEntry(DEFAULT_MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete MasterKey", e)
        }
    }

    private fun clearEncryptedSharedPreferences(context: Context) {
        runCatching {
            context.getSharedPreferences(SHARED_PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply()
        }.onFailure {
            Log.w(TAG, "Failed to clear the encrypted shared preferences", it)
        }
    }

    private fun deleteEncryptedSharedPreferencesFile(context: Context) {
        runCatching {
            val deleted = File(context.applicationInfo.dataDir,
                "shared_prefs/$SHARED_PREFERENCE_FILE_NAME.xml").delete()
            if (!deleted) {
                Log.w(TAG, "Failed to delete the encrypted shared preferences file")
            }
        }
    }
}