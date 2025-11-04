/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.oslc.core.auth.oauth_1_0a

import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.util.toMap
import io.ktor.utils.io.KtorDsl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.akehurst.oslc.oauth1_0a.createSignatureBaseString
import net.akehurst.oslc.oauth1_0a.generateHmacSha1Signature
import net.akehurst.oslc.oauth1_0a.generateNonce
import net.akehurst.oslc.oauth1_0a.oauthEncode
import kotlin.time.Clock

enum class ConsumerRequestParameterMethod {
    /** In the HTTP Authorization header as defined in OAuth HTTP Authorization Scheme [https://oauth.net/core/1.0a/#auth_header]. */
    HttpAuthorizationHeader,

    /** As the HTTP POST request body with a content-type of application/x-www-form-urlencoded. */
    HttpPostRequestBody,

    /** Added to the URLs in the query part (as defined by [RFC3986] section 3). */
    UrlQueryPart
}

suspend fun HttpClient.oauth_1_0a(
    clientKey: String,
    clientSecret: String,
    oauthRequestTokenUrl: String,
    oauthUserAuthorizationUrl: String,
    authorizeCallbackUrl: String,
    oauthAccessTokenUrl: String,
    requestParameterMethod: ConsumerRequestParameterMethod = ConsumerRequestParameterMethod.HttpAuthorizationHeader,
    additionalRequestParameters: Map<String, String> = emptyMap(),
    additionalAuthorizationParameters: Map<String, String> = emptyMap(),
    accessParameterMethod: ConsumerRequestParameterMethod,
    additionalAccessParameters: Map<String, String> = emptyMap(),
    /** Must respond with the verification_code */
    userAuthorise: (client: HttpClient, oauth_token: String) -> String,
) {

    // 1. The Consumer obtains an unauthorized Request Token.
    val oauthRequestParameters = mutableMapOf(
        "oauth_consumer_key" to clientKey,
        "oauth_nonce" to generateNonce(),
        "oauth_signature_method" to "HMAC-SHA1",
        "oauth_timestamp" to Clock.System.now().epochSeconds.toString(),
        "oauth_version" to "1.0",
    ) + additionalRequestParameters
    val oauthRequestParametersSorted = oauthRequestParameters.entries.sortedBy { it.key }.associate { it.key to it.value }
    val signatureBaseString = createSignatureBaseString(HttpMethod.Post, oauthRequestTokenUrl, oauthRequestParametersSorted)
    val signingKey = "${oauthEncode(clientSecret)}&"
    val signature = generateHmacSha1Signature(signatureBaseString, signingKey)

    val unauthorizedResponse: String? = when (requestParameterMethod) {
        ConsumerRequestParameterMethod.HttpAuthorizationHeader -> {
            TODO()
        }

        ConsumerRequestParameterMethod.HttpPostRequestBody -> {
            TODO()
        }

        ConsumerRequestParameterMethod.UrlQueryPart -> {
            val finalParameters = oauthRequestParameters + Pair("oauth_signature", signature)
            val requestTokenResponse = this.post(oauthRequestTokenUrl) {
                finalParameters.forEach { (k, v) -> parameter(k, v) }
            }
            if (requestTokenResponse.status.isSuccess()) {
                requestTokenResponse.bodyAsText()
            } else {
                null
            }
        }
    }
    // parse response: oauth_token=ab3cd9j4ks73hf7g&oauth_token_secret=xyz4992k83j47x0b
    val (oauth_token, oauth_token_secret) = unauthorizedResponse?.let { raw ->
        raw.split("&").map { it.substringBefore("=") to it.substringAfter("=") }.let {
            it[0].second to it[1].second
        }
    } ?: Pair(null, null)

    if (null == oauth_token || null == oauth_token_secret) {
        error("Unable to obtain an unauthorized Request Token.")
    } else {

        //2. The User authorizes the Request Token.
        val userAuthResponse = this.get(oauthUserAuthorizationUrl) {
            parameter("oauth_token", oauth_token)
            additionalAuthorizationParameters.forEach { (k, v) -> parameter(k, v) }
        }
        val verificationCode = when (userAuthResponse.status) {
            HttpStatusCode.Found -> userAuthorise.invoke(this, oauth_token)
            HttpStatusCode.OK -> userAuthResponse.bodyAsText()
            else -> error("Unable to Authorize the Request Token.")
        }

        //3. The Consumer exchanges the Request Token for an Access Token.
        val oauthAccessParameters = mutableMapOf(
            "oauth_consumer_key" to clientKey,
            "oauth_nonce" to generateNonce(),
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to Clock.System.now().epochSeconds.toString(),
            "oauth_token" to oauth_token,
            "oauth_verifier" to verificationCode,
            "oauth_version" to "1.0",
        ) + additionalRequestParameters

        val authorizedResponse: String? = when (accessParameterMethod) {
            ConsumerRequestParameterMethod.HttpAuthorizationHeader -> {
                TODO()
            }

            ConsumerRequestParameterMethod.HttpPostRequestBody -> {
                TODO()
            }

            ConsumerRequestParameterMethod.UrlQueryPart -> {
                val finalParameters = oauthAccessParameters + Pair("oauth_signature", signature)
                val accessTokenResponse = this.post(oauthRequestTokenUrl) {
                    finalParameters.forEach { (k, v) -> parameter(k, v) }
                }
                if (accessTokenResponse.status.isSuccess()) {
                    accessTokenResponse.bodyAsText()
                } else {
                    null
                }
            }
        }

    }
}

suspend fun waitForCallbackUsingKtorCIOEngine(port: Int, path: String)= coroutineScope {
    val receivedParams = CompletableDeferred<Parameters>()
    val server = embeddedServer(io.ktor.server.cio.CIO, port = port, host = "127.0.0.1") {
        routing {
            // Define the route that matches the callback URL
            get(path) {
                val params = call.request.queryParameters
                println("callback headers: "+call.request.headers.toMap())
                // 1. Acknowledge the request (e.g., display a success message)
                call.respondText("Success! You can close this window now.")

                // 2. Fulfill the Deferred object with the parameters
                receivedParams.complete(params)
            }
        }
    }
    // Start the server asynchronously
    val serverJob = launch {
        server.start(wait = false)
    }

    val params = receivedParams.await()
    server.stop(100, 100) // Graceful shutdown
    serverJob.cancel()

    return@coroutineScope params
}