/*
 * Copyright © 2017-2022  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.ddiclient

import okhttp3.Interceptor
import okhttp3.Response
import org.eclipse.hara.ddi.api.DdiRestConstants.Companion.CONFIG_DATA_ACTION
import org.eclipse.hara.ddi.security.Authentication
import java.io.IOException

/**
 * @author Daniele Sergio
 */
class UpdateFactoryAuthenticationRequestInterceptor(
    private val authentications: MutableSet<Authentication>,
    private val targetTokenFoundListener: TargetTokenFoundListener =
                object : TargetTokenFoundListener {}
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
        val isConfigDataRequest = originalRequest.url.toString().endsWith(CONFIG_DATA_ACTION)
        var targetTokenAuth: Authentication? = null
        builder.removeHeader("Authorization")
        authentications.filter { authentication ->
            authentication.token.isNotBlank()
        }.forEach { authentication ->
            builder.addHeader(authentication.header, authentication.headerValue)
            if (authentication.type === Authentication.AuthenticationType.TARGET_TOKEN_AUTHENTICATION) {
                targetTokenAuth = authentication
            }
        }
        if (isConfigDataRequest || targetTokenAuth == null) {
            builder.header(TARGET_TOKEN_REQUEST_HEADER_NAME, true.toString())
        }
        val response = chain.proceed(builder.build())
        val targetToken = response.header(TARGET_TOKEN_HEADER_NAME)
        if (targetToken != null && targetToken.isNotBlank()) {
            authentications.add(Authentication.newInstance(Authentication.AuthenticationType.TARGET_TOKEN_AUTHENTICATION, targetToken))
            targetTokenFoundListener.onFound(targetToken)
            authentications.remove(targetTokenAuth)
        }
        return response
    }

    companion object{
        private const val TARGET_TOKEN_HEADER_NAME = "UF-target-token"
        private const val TARGET_TOKEN_REQUEST_HEADER_NAME = "Expect-UF-target-token"
    }
}
