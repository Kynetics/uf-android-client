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

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Constructor


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class UFSharedPreferencesTest() {

    data class SharedPreferenceEntry<out T>(val key:String,val value:T){

        fun retrieveFrom(sh: SharedPreferences):T{
            return when(value){
                is String -> sh.getString(key, "")
                is Float -> sh.getFloat(key, 0F)
                is Long -> sh.getLong(key, 0L)
                is Boolean -> sh.getBoolean(key, false)
                is Set<*> -> sh.getStringSet(key, emptySet())
                else -> throw IllegalArgumentException("")
            } as T
        }

        fun putTo(sh: SharedPreferences){
            val editor = sh.edit()
            when(value){
                is String -> editor.putString(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Set<*> -> editor.putStringSet(key, value as Set<String>)
                else -> throw IllegalArgumentException("")
            }
            editor.commit()
        }
    }

    companion object{
        private val secureSP1 = SharedPreferenceEntry("secureKey1","secure value 1")
        private val secureSP2 = SharedPreferenceEntry("secureKey2", 1L)
        private val secureEntries = arrayOf(secureSP1,secureSP2)
        private val secureKeys = secureEntries.map { it.key }.toTypedArray()

        private val key1 = SharedPreferenceEntry("key1", "value 1")
        private val key2 = SharedPreferenceEntry("key2", 2L)
        private val entries = arrayOf(key1,key2)
        private val keys = entries.map { it.key }.toTypedArray()

    }


    init {
        val intersect = keys.intersect(secureKeys.toList())
        if(intersect.isNotEmpty() ){
            throw IllegalArgumentException("${intersect.joinToString (", ")} are in secureKeys")
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val spPlain = SharedPreferencesWithObjectImpl(context.getSharedPreferences("file1",Context.MODE_PRIVATE))
    private val spSecure = EncryptedSharedPreferences.get(context);

    @Test
    fun testSecureKeysAreStoredInEncryptedSharedPreferences() {
        // Context of the app under test.

        val ufSharedPreferences = buildSharedPreferences(spPlain, spSecure, secureKeys)

        secureSP1.putTo(ufSharedPreferences)

        Assert.assertEquals(secureSP1.value, secureSP1.retrieveFrom(ufSharedPreferences))
        Assert.assertEquals(secureSP1.value, secureSP1.retrieveFrom(spSecure))
        Assert.assertFalse(spPlain.contains(secureSP1.key))

    }

    @Test
    fun testUnSecureKeysAreStoredInSharedPreferences() {
        // Context of the app under test.

        val ufSharedPreferences = buildSharedPreferences(spPlain, spSecure, secureKeys)

        key1.putTo(ufSharedPreferences)

        Assert.assertEquals(key1.value, key1.retrieveFrom(ufSharedPreferences))
        Assert.assertEquals(key1.value, key1.retrieveFrom(spPlain))
        Assert.assertFalse(spSecure.contains(key1.key))
    }

    @Test
    fun testMoveSpEntriesDuringInitialization(){
        buildSharedPreferences(spPlain, spSecure, secureKeys)
        secureEntries.forEach {
            entry -> entry.putTo(spPlain)
        }

        entries.forEach {
            entry -> entry.putTo(spSecure)
        }

        entries.forEach {
            Assert.assertEquals(it.value, it.retrieveFrom(spSecure))
            Assert.assertFalse(spPlain.contains(it.key))
        }

        secureEntries.forEach {
            Assert.assertEquals(it.value, it.retrieveFrom(spPlain))
            Assert.assertFalse(spSecure.contains(it.key))
        }

        buildSharedPreferences(spPlain, spSecure, secureKeys)

        secureEntries.forEach {
            Assert.assertEquals(it.value, it.retrieveFrom(spSecure))
            Assert.assertFalse(spPlain.contains(it.key))
        }

        entries.forEach {
            Assert.assertEquals(it.value, it.retrieveFrom(spPlain))
            Assert.assertFalse(spSecure.contains(it.key))
        }

    }

    @Before
    fun initializePreferences(){
        spPlain.edit().clear().commit()
        spSecure.edit().clear().commit()
    }

    private fun buildSharedPreferences(
        sharedPreferencesWithObject: SharedPreferencesWithObjectImpl,
        secureSharedPreferences: SharedPreferences,
        secureKeys: Array<String>):UFSharedPreferences{

        val constructor:Constructor<UFSharedPreferences> = UFSharedPreferences::class.java.declaredConstructors.first() as Constructor<UFSharedPreferences>
        constructor.isAccessible = true
        return constructor.newInstance(sharedPreferencesWithObject, secureSharedPreferences, secureKeys)
    }
}
