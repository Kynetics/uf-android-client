/*
 * Copyright © 2017-2022  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.configuration

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.io.File
import java.net.URI

/**
 * @author Daniele Sergio
 */

@Suppress("unused")
enum class VariableEvaluation {

    FILE {
        override fun evaluate(uri: URI, context: Context): String? {
            return kotlin.runCatching {
                File(uri.path).bufferedReader().readLine().trim()
            }.getOrNull()
        }
    },

    PROPERTY {
        override fun evaluate(uri: URI, context: Context): String? {
            return PROPERTIES[uri.schemeSpecificPart]?.invoke(context)
        }
    };

    protected abstract fun evaluate(uri: URI, context: Context): String?

    companion object {
        private const val PROPERTY_ANDROID_ID_KEY = "ANDROID_ID"

        @Suppress("RegExpRedundantEscape")
        fun parseStringWithVariable(path: String, context: Context): String {
            val regex = "\\$\\{[^$\\{\\}]*\\}".toRegex()
            return regex.replace(path) {
                variableEvaluation(
                    it.value.substringAfter("{")
                            .substringBefore("}"), context)
            }
        }

        private fun variableEvaluation(variable: String, context: Context): String {
            return try {
                val uri = URI.create(variable)
                valueOf(uri.scheme.uppercase())
                        .evaluate(uri, context)
                        ?: (DEFAULT(context))
            } catch (e: IllegalArgumentException) {
                DEFAULT(context)
            }
        }

        @SuppressLint("HardwareIds")
        private val DEFAULT: (Context) -> String = { c -> Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) }

        protected val PROPERTIES: Map<String, (Context) -> String > =
                mapOf(PROPERTY_ANDROID_ID_KEY to DEFAULT)
    }
}
