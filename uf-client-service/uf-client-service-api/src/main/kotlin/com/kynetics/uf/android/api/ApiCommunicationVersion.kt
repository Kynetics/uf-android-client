/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.api

/**
 * Enum class that represents all supported api communication versions.
 *
 * @property versionCode the ApiCommunicationVersion version code number
 * @property versionName the ApiCommunicationVersion version name
 */
@Suppress("MemberVisibilityCanBePrivate")
enum class ApiCommunicationVersion(
    /**
     * the ApiCommunicationVersion version code number
     */
    val versionCode: Int,
    /**
     * the ApiCommunicationVersion version name
     */
    val versionName: String) {

    /**
     * Api version 0.1
     */
    V0_1(0, "0.1"),

    /**
     * Api version 1.0
     */
    V1(1, "1.0"),

    /**
     * Api version 1.1
     */
    V1_1(2, "1.1.0"),

    /**
     * Api version 1.2
     */
    V1_2(3, "1.2.0");


    companion object {

        /**
         * Utility method to obtain an [ApiCommunicationVersion] from its version code
         * @return the ApiCommunicationVersion object matching the given [versionCode].
         * @throws [NoSuchElementException] if no such element is found.
         */
        @JvmStatic
        fun fromVersionCode(
            /**
             * ApiCommunicationVersion version code
             */
            versionCode: Int): ApiCommunicationVersion {
            return values()
                .first {
                    it.versionCode == versionCode
                }
        }

        /**
         * Return the latest version of ApiCommunicationVersion
         */
        internal fun latest(): ApiCommunicationVersion{
            return values().maxByOrNull { it.versionCode }!!
        }
    }
}
