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
import android.util.Log

class UFSharedPreferences(
    private val sharedPreferencesWithObject: SharedPreferencesWithObjectImpl,
    private val secureSharedPreferences: SharedPreferences,
    private val secureKeys: Array<String>): SharedPreferencesWithObject by sharedPreferencesWithObject {

    private val errorOnMovingSharedPreference:Boolean

    companion object{
        private val TAG = UFSharedPreferences::class.java.simpleName
    }

    init {
        runCatching {
            moveSharedPreferences(sharedPreferencesWithObject, secureSharedPreferences) { entry -> secureKeys.contains(entry.key) }
            moveSharedPreferences(secureSharedPreferences, sharedPreferencesWithObject) { entry -> !secureKeys.contains(entry.key) }
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

