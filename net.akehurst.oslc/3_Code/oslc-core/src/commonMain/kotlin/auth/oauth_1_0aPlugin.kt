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

import io.ktor.client.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import net.akehurst.kotlinx.logging.api.logger
import net.akehurst.oslc.core.util.generateHmacSha1Signature
import net.akehurst.oslc.core.util.waitForCallbackUsingKtorCIOEngineAndOpenUrl
import kotlin.collections.plus
import kotlin.time.Clock

enum class ConsumerRequestParameterMethod {
    /** (Recommended) In the HTTP Authorization header as defined in OAuth HTTP Authorization Scheme [https://oauth.net/core/1.0a/#auth_header]. */
    HttpAuthorizationHeader,

    /** As the HTTP POST request body with a content-type of application/x-www-form-urlencoded. */
    HttpPostRequestBody,

    /** Added to the URLs in the query part (as defined by [RFC3986] section 3). */
    UrlQueryPart
}

private val logger = logger("oauth_1_0a")

suspend fun HttpClient.oauth_1_0a_dance(
    consumerKey: String,
    consumerSecret: String,
    userAuthorizeCallbackUrl: Url,
    authorizeUrl: Url,
    requestTokenUrl: Url,
    accessTokenUrl: Url,
    requestTokenParameterMethod: ConsumerRequestParameterMethod = ConsumerRequestParameterMethod.HttpAuthorizationHeader,
    additionalRequestTokenParameters: Map<String, String> = emptyMap(),
    accessTokenParameterMethod: ConsumerRequestParameterMethod = ConsumerRequestParameterMethod.HttpAuthorizationHeader,
    additionalAccessTokenParameters: Map<String, String> = emptyMap(),
    realm: String?,
    /** Must respond with the verification_code */
    userAuthorize: suspend (authorizeUrl:Url, oauth_token: String) -> String,
): Pair<String, String> {

    // 1. The Consumer obtains an unauthorized Request Token.
    val (request_token, request_token_secret) = let {
        val oauthRequestParameters = mutableMapOf(
            "oauth_callback" to userAuthorizeCallbackUrl.toString(),
            "oauth_consumer_key" to consumerKey,
            "oauth_nonce" to generateNonce(),
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to Clock.System.now().epochSeconds.toString(),
            "oauth_version" to "1.0",
            ) + (realm?.let { mapOf("realm" to it) } ?: emptyMap()) + additionalRequestTokenParameters
        val oauthRequestParametersSorted = oauthRequestParameters.entries.sortedBy { it.key }.associate { it.key to it.value }
        val signatureBaseString = createSignatureBaseString(HttpMethod.Post, requestTokenUrl.toString(), oauthRequestParametersSorted)
        val signingKey = "${consumerSecret.encodeOAuth()}&"
        val signature = generateHmacSha1Signature(signatureBaseString, signingKey)

        val (unauthorizedResponse, errors) = let {
            val finalParameters = oauthRequestParameters + Pair("oauth_signature", signature)
            logger.logTrace { "Obtain an unauthorized Request Token: POST $requestTokenUrl (parameters via $requestTokenParameterMethod) parameters: $finalParameters" }
            val requestTokenResponse = this.post(requestTokenUrl) {
                addParameters(requestTokenParameterMethod, finalParameters)
            }
            if (requestTokenResponse.status.isSuccess()) {
                Pair(requestTokenResponse.bodyAsText(), null)
            } else {
                Pair(null, requestTokenResponse.headers.toMap())
            }
        }
        // parse response: oauth_token=ab3cd9j4ks73hf7g&oauth_token_secret=xyz4992k83j47x0b
        unauthorizedResponse?.let { raw ->
            raw.split("&").map { it.substringBefore("=") to it.substringAfter("=") }.let {
                it[0].second to it[1].second
            }
        } ?: error("Unable to obtain a Request Token. $errors")
    }
    logger.logTrace { "Obtained request_token=$request_token  request_token_secret=**********" }

    // 2. The Consumer directs the Resource Owner to the Authorization page.
    // the mechnaism to do this depends on the application it is executed in
    // typically need to open a webbrowser and get the verification code from the callback
    val oauth_verifier = userAuthorize.invoke(authorizeUrl, request_token)

    //3. The Consumer exchanges the Request Token for an Access Token.
    return let {
        val oauthAccessParameters = mutableMapOf(
            "oauth_consumer_key" to consumerKey,
            "oauth_nonce" to generateNonce(),
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to Clock.System.now().epochSeconds.toString(),
            "oauth_token" to request_token.decodeURLPart(),
            "oauth_verifier" to oauth_verifier.decodeURLPart(),
            "oauth_version" to "1.0",
        ) + (realm?.let { mapOf("realm" to it) } ?: emptyMap()) + additionalAccessTokenParameters
        val oauthAccessParametersSorted = oauthAccessParameters.entries.sortedBy { it.key }.associate { it.key to it.value }
        val signatureBaseString = createSignatureBaseString(HttpMethod.Post, accessTokenUrl.toString(), oauthAccessParametersSorted)
        val signingKey = "${consumerSecret.encodeOAuth()}&${request_token_secret.decodeURLPart().encodeOAuth()}"
        val signature = generateHmacSha1Signature(signatureBaseString, signingKey)
        val (authorizedResponse, errors) = let {
            val finalParameters = oauthAccessParameters + Pair("oauth_signature", signature)
            logger.logTrace { "Exchange the Request Token for an Access Token: POST $accessTokenUrl (parameters via $accessTokenParameterMethod) parameters: $finalParameters" }
            val accessTokenResponse = this.post(accessTokenUrl) {
                addParameters(accessTokenParameterMethod, finalParameters)
            }
            if (accessTokenResponse.status.isSuccess()) {
                Pair(accessTokenResponse.bodyAsText(), null)
            } else {
                Pair(null, accessTokenResponse.headers.toMap())
            }
        }
        val accessPair = authorizedResponse?.let { raw ->
            raw.split("&").map { it.substringBefore("=") to it.substringAfter("=") }.let {
                it[0].second to it[1].second
            }
        } ?: error("Unable to obtain an Access Token. $errors")
        logger.logTrace { "Obtained access_token=${accessPair.first}  access_token_secret=**********" }
        accessPair
    }
}

fun HttpRequestBuilder.addParameters(
    parameterMethod: ConsumerRequestParameterMethod,
    parameters: Map<String, String>
) {
    when (parameterMethod) {
        ConsumerRequestParameterMethod.HttpAuthorizationHeader -> {
            val params = parameters.entries.joinToString(", ") { "${it.key}=\"${it.value.encodeOAuth()}\"" }
            headers.append(HttpHeaders.Authorization, "OAuth $params")
        }

        ConsumerRequestParameterMethod.HttpPostRequestBody -> {
            TODO()
        }

        ConsumerRequestParameterMethod.UrlQueryPart -> {
            parameters.forEach { (k, v) -> parameter(k, v) }
        }
    }
}

fun HttpRequestBuilder.oauth1_0a_sign(
    protectedResourceUrl: String,
    consumerKey: String,
    consumerSecret: String,
    accessToken: String,
    accessTokenSecret: String,
    realm: String?,
    parameterMethod: ConsumerRequestParameterMethod = ConsumerRequestParameterMethod.HttpAuthorizationHeader,
    additionalProtectedParameters: List<Pair<String, String>> = emptyList(),
) {
    val oauthParameters = mutableMapOf(
        "oauth_consumer_key" to consumerKey,
        "oauth_nonce" to generateNonce(),
        "oauth_signature_method" to "HMAC-SHA1",
        "oauth_timestamp" to Clock.System.now().epochSeconds.toString(),
        "oauth_token" to accessToken,
        "oauth_version" to "1.0",
    ) + (realm?.let { mapOf("realm" to it) } ?: emptyMap())
    val allParameters = oauthParameters + additionalProtectedParameters
    val signatureBaseString = createSignatureBaseString(this.method, protectedResourceUrl, allParameters)
    val signingKey = "${consumerSecret.encodeOAuth()}&${accessTokenSecret.encodeOAuth()}"
    val signature = generateHmacSha1Signature(signatureBaseString, signingKey)
    val finalParameters = oauthParameters + Pair("oauth_signature", signature)
    addParameters(parameterMethod, finalParameters)
    logger.logInformation { "Request Signed (OAuth 1.0a): $method $protectedResourceUrl" }
    logger.logTrace { "Request Signed (OAuth 1.0a):    (parameters via $parameterMethod) parameters: $finalParameters" }
}

class OAuth_1_0a_Config {
    internal var _consumerKey: String? = null
    internal var _consumerSecret: String? = null
    internal var _authorizeUrl: Url? = null
    internal var _oauthRequestTokenUrl: Url? = null
    internal var _userAuthorizeCallbackUrl: Url? = null
    internal var _oauthAccessTokenUrl: Url? = null
    internal var _realm: String? = null

    internal var _requestTokenParameterMethod: ConsumerRequestParameterMethod = ConsumerRequestParameterMethod.HttpAuthorizationHeader
    internal var _additionalRequestTokenParameters: Map<String, String> = emptyMap()
    internal var _accessTokenParameterMethod: ConsumerRequestParameterMethod = ConsumerRequestParameterMethod.HttpAuthorizationHeader
    internal var _additionalAccessTokenParameters: Map<String, String> = emptyMap()
    internal var _signProtectedParameterMethod: ConsumerRequestParameterMethod = ConsumerRequestParameterMethod.HttpAuthorizationHeader
    internal var _userAuthorize: (suspend (authorizeUrl:Url, oauth_token: String) -> String)? = { authorizeUrl, oauth_token ->
        waitForOAuth_1_0a_CallbackUsingKtorCIOEngineAndOpenUrl(
            authorizationUrl = authorizeUrl,
            oauth_token = oauth_token
        )
    }

    // cached values
    internal var _accessToken: String? = null
    internal var _accessTokenSecret: String? = null

    fun consumerKey(value: String) {
        _consumerKey = value
    }

    fun consumerSecret(value: String) {
        _consumerSecret = value
    }

    fun userAuthorizeCallbackUrl(value: Url) {
        _userAuthorizeCallbackUrl = value
    }

    fun authorizeUrl(value: Url) {
        _authorizeUrl = value
    }

    fun requestTokenUrl(value: Url) {
        _oauthRequestTokenUrl = value
    }

    fun accessTokenUrl(value: Url) {
        _oauthAccessTokenUrl = value
    }

    fun realm(value: String?) {
        _realm = value
    }

    fun requestTokenParameterMethod(value: ConsumerRequestParameterMethod) {
        _requestTokenParameterMethod = value
    }

    fun additionalRequestTokenParameters(value: Map<String, String>) {
        _additionalRequestTokenParameters = value
    }

    fun accessTokenParameterMethod(value: ConsumerRequestParameterMethod) {
        _accessTokenParameterMethod = value
    }

    fun additionalAccessTokenParameters(value: Map<String, String>) {
        _additionalAccessTokenParameters = value
    }

    fun userAuthorize(value: suspend (authorizeUrl:Url, oauth_token: String) -> String) {
        _userAuthorize = value
    }

    fun signProtectedParameterMethod(value: ConsumerRequestParameterMethod) {
        _signProtectedParameterMethod = value
    }
}

suspend fun waitForOAuth_1_0a_CallbackUsingKtorCIOEngineAndOpenUrl(
    callbackHost: String = "127.0.0.1",
    callbackPort: Int = 9000,
    callbackPath: String = "/callback",
    authorizationUrl: Url,
    oauth_token:String
):String {
    val result = waitForCallbackUsingKtorCIOEngineAndOpenUrl(
        callbackHost, callbackPort, callbackPath,
        authorizationUrl
    ) {
        append("oauth_token", oauth_token)
    }
    return result["oauth_verifier"]!![0]
}

val OAuth_1_0a: ClientPlugin<OAuth_1_0a_Config> = createClientPlugin("OAuth_1_0a", ::OAuth_1_0a_Config) {
    val consumerKey = this.pluginConfig._consumerKey ?: error("consumerKey not set")
    val consumerSecret = this.pluginConfig._consumerSecret ?: error("consumerSecret not set")
    val realm = this.pluginConfig._realm
    val signProtectedParameterMethod = pluginConfig._signProtectedParameterMethod
    val config = this.pluginConfig
    val clientEngine = this.client.engine
    onRequest { request, _ ->
        // if accessToken and secret are null then must do the oauth 1.0a dance
        val (accessToken, accessTokenSecret) = let {
            if (null == config._accessToken || null == config._accessTokenSecret) {
                val oauthRequestTokenUrl = config._oauthRequestTokenUrl ?: error("oauthRequestTokenUrl not set")
                val userAuthorizeCallbackUrl = config._userAuthorizeCallbackUrl ?: error("userAuthorizeCallbackUrl not set")
                val authorizeUrl = config._authorizeUrl ?: error("authorizeUrl not set")
                val accessTokenUrl = config._oauthAccessTokenUrl ?: error("oauthAccessTokenUrl not set")
                val requestTokenParameterMethod = config._requestTokenParameterMethod
                val additionalRequestTokenParameters = config._additionalRequestTokenParameters
                val accessTokenParameterMethod = config._accessTokenParameterMethod
                val additionalAccessTokenParameters = config._additionalAccessTokenParameters
                val userAuthorize = config._userAuthorize ?: error("userAuthorize not set")
                val (access_token, access_secret) = HttpClient(clientEngine).oauth_1_0a_dance(
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    userAuthorizeCallbackUrl = userAuthorizeCallbackUrl,
                    authorizeUrl = authorizeUrl,
                    requestTokenUrl = oauthRequestTokenUrl,
                    accessTokenUrl = accessTokenUrl,
                    requestTokenParameterMethod = requestTokenParameterMethod,
                    additionalRequestTokenParameters = additionalRequestTokenParameters,
                    accessTokenParameterMethod = accessTokenParameterMethod,
                    additionalAccessTokenParameters = additionalAccessTokenParameters,
                    realm = realm,
                    userAuthorize = userAuthorize,
                )
                logger.logTrace { "Received: access_token=$access_token  access_secret=**********" }
                config._accessToken = access_token.decodeURLPart()
                config._accessTokenSecret = access_secret.decodeURLPart()
                config._accessToken!! to config._accessTokenSecret!!
            } else {
                Pair(config._accessToken!!, config._accessTokenSecret!!)
            }
        }
        logger.logTrace { "Using for signing: access_token=$accessToken  access_secret=**********" }
        val additionalProtectedParameters = request.url.parameters.entries().flatMap { (k, l) -> l.map { k to it } }
        request.oauth1_0a_sign(
            protectedResourceUrl = request.url.buildString(),
            consumerKey = consumerKey,
            consumerSecret = consumerSecret,
            accessToken = accessToken,
            accessTokenSecret = accessTokenSecret,
            realm = realm,
            parameterMethod = signProtectedParameterMethod,
            additionalProtectedParameters = additionalProtectedParameters
        )
    }
}

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
    val encodedBaseUrl = baseUrl.encodeURLParameter()

    // c) Normalized Parameters
    val normalizedParams = allParameters
        .map { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        }
        .joinToString("&")
    val encodedParameters = normalizedParams.encodeOAuth()

    // d) Final concatenation
    return "$httpMethod&$encodedBaseUrl&$encodedParameters"
}
