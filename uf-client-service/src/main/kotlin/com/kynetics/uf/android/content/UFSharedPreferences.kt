/*
 * Copyright © 2017-2022  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.content

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kynetics.uf.android.R
import java.io.Serializable

class UFSharedPreferences private constructor(
        private val sharedPreferencesWithObject: SharedPreferencesWithObject,
        private val secureSharedPreferences: SharedPreferences,
        private val secureKeys: Array<String>): SharedPreferences by sharedPreferencesWithObject {

    private val errorOnMovingSharedPreference:Boolean

    companion object{
        private val TAG = UFSharedPreferences::class.java.simpleName

        fun get(context: Context, name:String?, mode:Int):UFSharedPreferences = UFSharedPreferences(
                SharedPreferencesWithObject(context.getSharedPreferences(name, mode)),
                EncryptedSharedPreferences.get(context),
                arrayOf(context.getString(R.string.shared_preferences_gateway_token_key),
                        context.getString(R.string.shared_preferences_target_token_key),
                        context.getString(R.string.shared_preferences_target_token_received_from_server_key)))
    }

    init {
        runCatching {
            Log.d(TAG, "Moving $secureKeys to encrypted sharedPreferences")
            moveSharedPreferences(sharedPreferencesWithObject, secureSharedPreferences) { entry -> secureKeys.contains(entry.key) }
            Log.d(TAG, "$secureKeys successfully moved to encrypted sharedPreferences")
            Log.d(TAG, "Moving other keys to plain sharedPreferences")
            moveSharedPreferences(secureSharedPreferences, sharedPreferencesWithObject) { entry -> !secureKeys.contains(entry.key) }
            Log.d(TAG, "Keys successfully moved to plain sharedPreferences")
        }.onFailure {
            Log.w(TAG, "Error on moving shared preferences", it.cause)
        }.run {
            errorOnMovingSharedPreference = isFailure
        }
    }

    override fun contains(key: String?): Boolean = selectSP(key).contains(key)

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = selectSP(key).getBoolean(key, defValue)

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if(listener != null){
            secureSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            sharedPreferencesWithObject.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun getInt(key: String?, defValue: Int): Int  = selectSP(key).getInt(key, defValue)

    override fun getAll(): MutableMap<String, *>{
        return listOf(*secureSharedPreferences.all.entries.toTypedArray(),
                *sharedPreferencesWithObject.all.entries.toTypedArray())
                .map { entry -> entry.toPair()  }
                .toMap()
                .toMutableMap()
    }

    override fun edit(): SharedPreferences.Editor = UFEditor(sharedPreferencesWithObject.edit(), secureSharedPreferences.edit(), secureKeys)

    override fun getLong(key: String?, defValue: Long): Long = selectSP(key).getLong(key, defValue)

    override fun getFloat(key: String?, defValue: Float): Float = selectSP(key).getFloat(key, defValue)

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = selectSP(key).getStringSet(key, defValues)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if(listener != null){
            secureSharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            sharedPreferencesWithObject.registerOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun getString(key: String?, defValue: String?): String? {
        return selectSP(key).getString(key, defValue)
    }

    fun <T : Serializable?> getObject(objKey: String?): T? =
            sharedPreferencesWithObject.getObject(objKey)

    fun <T : Serializable?> getObject(objKey: String?, defaultObj: T?): T? =
            sharedPreferencesWithObject.getObject(objKey, defaultObj)

    fun <T> putAndCommitObject(key: String?, obj: T) =
            sharedPreferencesWithObject.putAndCommitObject(key, obj)

    @Suppress("UNCHECKED_CAST")
    private fun moveSharedPreferences(sp1:SharedPreferences, sp2:SharedPreferences, moveTo:(Map.Entry<String, Any?>) -> Boolean){
        sp2.edit().apply{
            val sp1Editor = sp1.edit()
            sp1.all.filter(moveTo).forEach{ entry ->
                Log.d(TAG, "Moving key ${entry.key}")
                var remove = true
                when(entry.value){
                    is String -> putString(entry.key, entry.value.toString())
                    is Int -> putInt(entry.key, entry.value.toString().toInt())
                    is Float -> putFloat(entry.key, entry.value.toString().toFloat())
                    is Long-> putLong(entry.key, entry.value.toString().toLong())
                    is Boolean -> putBoolean(entry.key, entry.value.toString().toBoolean())
                    is MutableSet<*> ->  putStringSet(entry.key, entry.value as MutableSet<String>)
                    else -> {
                        Log.w(TAG, "Can't move entry with key ${entry.key}")
                        remove = false
                    }
                }
                if(remove) {
                    sp1Editor.remove(entry.key)
                }
            }
            apply()
            sp1Editor.apply()
        }
    }


    private fun selectSP(key:String?): SharedPreferences{
        return when{
            errorOnMovingSharedPreference -> sharedPreferencesWithObject
            key in secureKeys -> secureSharedPreferences
            else -> sharedPreferencesWithObject
        }
    }

    private class UFEditor(private val editor:SharedPreferences.Editor,
                           private val secureEditor: SharedPreferences.Editor,
                           private val secureKeys: Array<String>
    ):SharedPreferences.Editor{


        override fun clear(): SharedPreferences.Editor = apply {
            secureEditor.clear()
            editor.clear()
        }


        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = selectEditor(key).putLong(key, value)


        override fun putInt(key: String?, value: Int): SharedPreferences.Editor  = selectEditor(key).putInt(key, value)

        override fun remove(key: String?): SharedPreferences.Editor = selectEditor(key).remove(key)

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = selectEditor(key).putBoolean(key, value)

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor  = selectEditor(key).putStringSet(key, values)

        override fun commit(): Boolean  = secureEditor.commit() && editor.commit()

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor  = selectEditor(key).putFloat(key, value)

        override fun apply() {
            secureEditor.apply()
            editor.apply()
        }

        override fun putString(key: String?, value: String?): SharedPreferences.Editor  = selectEditor(key).putString(key, value)

        private fun selectEditor(key:String?): SharedPreferences.Editor{
            return if(key in secureKeys){
                secureEditor
            } else {
                editor
            }
        }
    }
}

