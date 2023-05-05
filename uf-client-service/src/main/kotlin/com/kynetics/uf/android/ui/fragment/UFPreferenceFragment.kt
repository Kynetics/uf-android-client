/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.ui.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import com.cronutils.descriptor.CronDescriptor
import com.kynetics.uf.android.R
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.api.UFServiceConfigurationV2
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.cron.HaraCronParser
import com.kynetics.uf.android.formatter.toPwdFormat


/**
 * A simple [PreferenceFragmentCompat] subclass.
 */
class UFPreferenceFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var startingSharedPreferences: Map<String, Any?> = mutableMapOf()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = getString(R.string.shared_preferences_file)
        setPreferencesFromResource(R.xml.pref_general, rootKey)
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        startingSharedPreferences = preferenceScreen.sharedPreferences.all


        preferenceManager.sharedPreferences.all.forEach { (key, _) ->
            if(key.contains("token", true)){
                val pref = findPreference<Preference>(key)
                if(pref is EditTextPreference){
                    pref.setOnBindEditTextListener {
                        with(it){
                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            transformationMethod = PasswordTransformationMethod.getInstance()
                            selectAll()
                            filters = arrayOf(InputFilter.LengthFilter(99))
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        UpdateFactoryService.startService(requireContext())
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = preferenceManager.sharedPreferences

        for (i in 0 until preferenceScreen.preferenceCount) {
            val preference = preferenceScreen.getPreference(i)
            if (preference is PreferenceGroup) {
                for (j in 0 until preference.preferenceCount) {
                    val singlePref = preference.getPreference(j)
                    updatePreference(singlePref, singlePref.key, sharedPrefs)
                }
            } else {
                updatePreference(preference, preference.key, sharedPrefs)
            }
        }
        enableDisableActivePreference(sharedPrefs)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (!isAdded) {
            return
        }
        val sharedPrefs = preferenceManager.sharedPreferences
        val preference:Preference? = findPreference(key)
        updatePreference(preference, key, sharedPrefs)
        enableDisableActivePreference(sharedPrefs)

        if (key == getString(R.string.shared_preferences_is_enable_key) && !(preference as SwitchPreferenceCompat).isChecked) {
            AlertDialog.Builder(requireContext())
                    .setTitle(resources.getString(R.string.stop_service_dialog_title))
                    .setCancelable(false)
                    .setMessage(
                            resources.getString(R.string.stop_service_dialog_message))
                    .setPositiveButton(
                            resources.getString(android.R.string.ok)
                    ) { _, _ -> }
                    .setNegativeButton(
                            resources.getString(android.R.string.cancel)
                    ) { _, _ -> preference.isChecked = true }.show()
        }
    }

    private fun enableDisableActivePreference(shp: SharedPreferences) {
        val activePreference:Preference? = findPreference(getString(R.string.shared_preferences_is_enable_key))
        activePreference?.isEnabled =
                !(shp.getString(getString(R.string.shared_preferences_server_url_key), "")!!.isEmpty() ||
                shp.getString(getString(R.string.shared_preferences_tenant_key), "")!!.isEmpty() ||
                shp.getString(getString(R.string.shared_preferences_controller_id_key), "")!!.isEmpty())
    }

    private fun updatePreference(preference: Preference?, key: String, sharedPrefs: SharedPreferences) {
        if (preference == null || preference is SwitchPreferenceCompat) {
            return
        }

        if (preference is ListPreference) {
            val listPreference = preference as ListPreference?
            listPreference!!.summary = listPreference.entry
            return
        }

        if (preference is EditTextPreference) {
            preference.setSummaryProvider {
                when {
                    preference.key.contains("token", true) -> preference.text.toPwdFormat()
                    preference.key == getString(R.string.shared_preferences_time_windows_cron_expression_key) ->{
                        val cronExpression = preference.text ?: UFServiceConfigurationV2.TimeWindows.ALWAYS
                        val cronDescription = CronDescriptor.instance().describe(HaraCronParser.parse(cronExpression))
                        "$cronDescription ( $cronExpression )"
                    }
                    else -> preference.text
                }

            }

        }

        if (key == getString(R.string.shared_preferences_current_state_key)) {
            runCatching {
                (MessengerHandler.getlastSharedMessage(ApiCommunicationVersion.V1).messageToSendOnSync as? String)
                    ?.let {
                        preference.summary = UFServiceMessageV1.fromJson(it).name.name
                    }
            }.onFailure { error ->
                Log.w(TAG, "Error setting current state", error)
            }
            return
        }

        if (key == getString(R.string.shared_preferences_system_update_type_key)) {
            preference.summary = sharedPrefs.getString(getString(R.string.shared_preferences_system_update_type_key), "")
            return
        }

        if (key == getString(R.string.shared_preferences_target_token_received_from_server_key)){
            preference.summary = sharedPrefs.getString(key, "").toPwdFormat()
        }
    }

    override fun onPause() {
        super.onPause()
        val sp = preferenceManager.sharedPreferences
        sp.edit().apply()

        if(startingSharedPreferences[getString(R.string.shared_preferences_server_url_key)] != sp.getString(getString(R.string.shared_preferences_server_url_key), null)
            || startingSharedPreferences[getString(R.string.shared_preferences_tenant_key)] != sp.getString(getString(R.string.shared_preferences_tenant_key), null)
            || startingSharedPreferences[getString(R.string.shared_preferences_controller_id_key)] != sp.getString(getString(R.string.shared_preferences_controller_id_key), null)
        ){
            sp.edit().remove(getString(R.string.shared_preferences_target_token_received_from_server_key)).apply()
        }

        val currentSharedPreferences = preferenceScreen.sharedPreferences.all
        if (currentSharedPreferences != startingSharedPreferences) {
            UpdateFactoryService.ufServiceCommand!!.configureService()
            startingSharedPreferences = currentSharedPreferences
        }
    }

    override fun onDetach() {
        super.onDetach()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    companion object {

        private val TAG = UFPreferenceFragment::class.java.simpleName

        fun newInstance(): UFPreferenceFragment {
            return UFPreferenceFragment()
        }
    }
}
