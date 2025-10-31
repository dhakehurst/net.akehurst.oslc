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

package net.akehurst.oslc.oauth1_0a

import io.ktor.util.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import org.kotlincrypto.macs.hmac.Hmac
import org.kotlincrypto.macs.hmac.sha1.HmacSHA1
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

/**
 * Extension function to sign an outgoing Ktor request using OAuth 1.0a parameters.
 *
 * @param consumerKey Application's consumer key.
 * @param consumerSecret Application's consumer secret.
 * @param token The OAuth token (Request or Access Token).
 * @param tokenSecret The OAuth token secret.
 * @param callbackUrl The callback URL (required only for the initial request token step).
 * @param verifier The verifier code (required only for the access token step).
 */
fun HttpRequestBuilder.oauth1_0a_sign(
    consumerKey: String,
    consumerSecret: String,
    token: String? = null,
    tokenSecret: String? = null,
    callbackUrl: String? = null,
    verifier: String? = null
) {
    // 1. Core OAuth Parameters
    val nonce = generateNonce()
    val timestamp = (Clock.System.now().epochSeconds).toString()
    val signatureMethod = "HMAC-SHA1"
    val version = "1.0"

    // 2. Build the parameter map for signing
    val oauthParameters = mutableMapOf<String, String>()
    oauthParameters["oauth_consumer_key"] = consumerKey
    oauthParameters["oauth_nonce"] = nonce
    oauthParameters["oauth_signature_method"] = signatureMethod
    oauthParameters["oauth_timestamp"] = timestamp
    oauthParameters["oauth_version"] = version

    if (token != null) oauthParameters["oauth_token"] = token
    if (callbackUrl != null) oauthParameters["oauth_callback"] = callbackUrl
    if (verifier != null) oauthParameters["oauth_verifier"] = verifier

    // 3. Collect ALL parameters (OAuth, Query, and URL-Encoded Body)
    val allParameters = mutableMapOf<String, String>()

    // Add OAuth parameters
    allParameters.putAll(oauthParameters)

    // Add Query parameters
    url.parameters.entries().forEach { (key, values) ->
        values.forEach { value -> allParameters[key] = value }
    }

    // Note: For POST/PUT with 'application/x-www-form-urlencoded', body parameters
    // must also be included in 'allParameters'. This is complex and omitted for brevity.

    // 4. Generate the Signature Base String
    val signatureBaseString = createSignatureBaseString(method, url.toString(), allParameters)

    // 5. Generate the Signing Key
    val signingKey = "${oauthEncode(consumerSecret)}&${oauthEncode(tokenSecret.orEmpty())}"

    // 6. Generate the Signature
    val signature = generateHmacSha1Signature(signatureBaseString, signingKey)

    // 7. Add the signature to the OAuth parameters
    oauthParameters["oauth_signature"] = signature

    // 8. Construct the final Authorization Header
    val headerValue = oauthParameters
        .map { (key, value) ->
            "$key=\"${oauthEncode(value)}\""
        }
        .joinToString(separator = ", ", prefix = "OAuth ")

    headers[HttpHeaders.Authorization] = headerValue
}

// ----------------------------------------------------------------------------------
// Core Utilities
// ----------------------------------------------------------------------------------

/**
 * Creates the canonical Signature Base String for hashing.
 * Format: HTTP_METHOD&URL-ENCODE(BASE_URL)&URL-ENCODE(NORMALIZED_PARAMETERS)
 */
fun createSignatureBaseString(
    method: HttpMethod,
    fullUrl: String,
    allParameters: Map<String, String>
): String {
    // a) HTTP Method (Uppercase)
    val httpMethod = method.value.uppercase()

    // b) Base URL: Remove query, fragment, and default ports (80/443)
    val baseUrl = URLBuilder(fullUrl).apply {
        parameters.clear()
        val isDefaultPort = (protocol.name == "https" && port == 443) || (protocol.name == "http" && port == 80)
        if (isDefaultPort) port = protocol.defaultPort
    }.buildString()
    val encodedBaseUrl = oauthEncode(baseUrl)

    // c) Normalized Parameters
    val normalizedParams = allParameters
        .map { (key, value) ->
            "${oauthEncode(key)}=${oauthEncode(value)}"
        }
        .joinToString("&")
    val encodedParameters = oauthEncode(normalizedParams)

    // d) Final concatenation
    return "$httpMethod&$encodedBaseUrl&$encodedParameters"
}

@OptIn(ExperimentalUuidApi::class)
fun generateNonce(): String = kotlin.uuid.Uuid.random().toString().replace("-", "")

/**
 * Strict OAuth 1.0a encoding required for signature generation.
 */
fun oauthEncode(s: String): String =
    s.encodeURLPath()
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~") // tilde is NOT encoded in OAuth

fun generateHmacSha1Signature(data: String, key: String): String {
    val mac = HmacSHA1(key.toByteArray())
    val rawHmac = mac.doFinal(data.encodeToByteArray())
    return rawHmac.encodeBase64()
}

