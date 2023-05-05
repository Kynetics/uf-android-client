/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.clientexample.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.kynetics.uf.clientexample.activity.MainActivity

class MyAlertDialogFragment : androidx.fragment.app.DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogType = requireArguments().getString(ARG_DIALOG_TYPE)
        val titleResource = resources.getIdentifier(String.format("%s_%s", dialogType?.lowercase(), "title"),
            "string", requireActivity().packageName)
        val contentResource = resources.getIdentifier(String.format("%s_%s", dialogType?.lowercase(), "content"),
            "string", requireActivity().packageName)

        return AlertDialog.Builder(requireActivity())
            // .setIcon(R.drawable.alert_dialog_icon)
            .setTitle(titleResource)
            .setMessage(contentResource)
            .setPositiveButton(android.R.string.ok
            ) { _, _ -> (activity as MainActivity).sendPermissionResponse(true) }
            .setNegativeButton(android.R.string.cancel
            ) { _, _ -> (activity as MainActivity).sendPermissionResponse(false) }
            .create()
    }

    companion object {
        private val ARG_DIALOG_TYPE = "DIALOG_TYPE"
        fun newInstance(dialogType: String): MyAlertDialogFragment {
            val frag = MyAlertDialogFragment()
            val args = Bundle()
            args.putString(ARG_DIALOG_TYPE, dialogType)
            frag.arguments = args
            frag.isCancelable = false
            return frag
        }
    }
}
