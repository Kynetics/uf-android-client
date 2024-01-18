/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.kynetics.uf.android.ui.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * Activities that contain this fragment must implement the
 * [AuthorizationDialogFragment.OnAuthorization] interface
 * to handle interaction events.
 * Use the [AuthorizationDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 * @author Daniele Sergio
 */
class AuthorizationDialogFragment : DialogFragment() {
    interface OnAuthorization {
        fun onAuthorizationGrant()
        fun onAuthorizationDenied()
    }

    private var mDescription: String? = null
    private var mTitle: String? = null
    private var mPositiveButton: String? = null
    private var mNegativeButton: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mTitle = arguments?.getString(ARG_TITLE)
            mDescription = arguments?.getString(ARG_DESCRIPTION)
            mPositiveButton = arguments?.getString(ARG_POSITIVE_BUTTON)
            mNegativeButton = arguments?.getString(ARG_NEGATIVE_BUTTON)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireActivity())
                .setTitle(mTitle)
                .setMessage(mDescription)
                .setPositiveButton(mPositiveButton
                ) { _: DialogInterface?, _: Int -> onAuthorizationGrant() }
                .setNegativeButton(mNegativeButton
                ) { _: DialogInterface?, _: Int -> onAuthorizationDenied() }
                .create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        isCancelable = false
        return view
    }

    private fun onAuthorizationGrant() {
        mListener!!.onAuthorizationGrant()
    }

    private fun onAuthorizationDenied() {
        mListener!!.onAuthorizationDenied()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnAuthorization) {
            context
        } else {
            throw RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private var mListener: OnAuthorization? = null

    companion object {
        private const val ARG_DESCRIPTION = "description_arg"
        private const val ARG_TITLE = "title_arg"
        private const val ARG_POSITIVE_BUTTON = "positive_button_arg"
        private const val ARG_NEGATIVE_BUTTON = "negative_button_arg"
        fun newInstance(title: String?, description: String?, positiveButton: String?, negativeButton: String?): AuthorizationDialogFragment {
            val fragment = AuthorizationDialogFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_DESCRIPTION, description)
            args.putString(ARG_POSITIVE_BUTTON, positiveButton)
            args.putString(ARG_NEGATIVE_BUTTON, negativeButton)
            fragment.arguments = args
            return fragment
        }
    }
}