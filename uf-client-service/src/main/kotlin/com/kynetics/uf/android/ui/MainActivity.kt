/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.kynetics.uf.android.R
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.content.SharedPreferencesWithObject
import com.kynetics.uf.android.content.SharedPreferencesFactory
import com.kynetics.uf.android.ui.fragment.AuthorizationDialogFragment
import com.kynetics.uf.android.ui.fragment.AuthorizationDialogFragment.OnAuthorization
import com.kynetics.uf.android.ui.fragment.UFPreferenceFragment

/**
 * @author Daniele Sergio
 */
class MainActivity : AppCompatActivity(), OnAuthorization {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.update_factory_label)
        val intent = intent
        when (val type = intent.getIntExtra(INTENT_TYPE_EXTRA_VARIABLE, 0)) {
            INTENT_TYPE_EXTRA_VALUE_SETTINGS -> {
                setTheme(R.style.AppTheme)
                setContentView(R.layout.activity_main)
                val actionBar = supportActionBar
                actionBar!!.setDisplayHomeAsUpEnabled(true)
                changePage(UFPreferenceFragment.newInstance())
            }
            INTENT_TYPE_EXTRA_VALUE_DOWNLOAD, INTENT_TYPE_EXTRA_VALUE_REBOOT -> {
                setTheme(R.style.AppTransparentTheme)
                showAuthorizationDialog(type)
            }
            else -> throw IllegalArgumentException("")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAuthorizationDialog(type: Int) {
        val newFragment = AuthorizationDialogFragment.newInstance(
                getString(
                        if (type == INTENT_TYPE_EXTRA_VALUE_DOWNLOAD)
                            R.string.update_download_title
                        else
                            R.string.update_started_title
                ),
                getString(
                        if (type == INTENT_TYPE_EXTRA_VALUE_DOWNLOAD)
                            R.string.update_download_content
                        else
                            R.string.update_started_content
                ),
                getString(android.R.string.ok),
                getString(R.string.close)
        )
        newFragment.show(supportFragmentManager, "authorization")
    }

    private fun changePage(fragment: androidx.fragment.app.Fragment) {
        val tx = supportFragmentManager.beginTransaction()
        tx.replace(R.id.frame_layout, fragment)
        tx.commit()
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    override fun onAuthorizationGrant() {
        UpdateFactoryService.ufServiceCommand!!.authorizationGranted()
        finishActivity()
    }

    override fun onAuthorizationDenied() {
        UpdateFactoryService.ufServiceCommand!!.authorizationDenied()
        finishActivity()
    }

    private fun finishActivity() {
        Handler(Looper.myLooper()!!).postDelayed({ finish() }, 500)
    }

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferencesWithObject {
        return SharedPreferencesFactory.get(applicationContext, name, mode)
    }

    companion object {
        const val INTENT_TYPE_EXTRA_VARIABLE = "EXTRA_TYPE"
        const val INTENT_TYPE_EXTRA_VALUE_SETTINGS = 0
        const val INTENT_TYPE_EXTRA_VALUE_DOWNLOAD = 1
        const val INTENT_TYPE_EXTRA_VALUE_REBOOT = 2
    }
}
