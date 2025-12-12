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

package net.akehurst.oslc.core.auth.oauth_2_0

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.akehurst.kotlinx.logging.api.logger
import net.akehurst.oslc.core.util.openUrl
import net.akehurst.oslc.core.util.waitForCallbackUsingKtorCIOEngineAndOpenUrl
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class OAuth_2_0_Config {
    internal var _clientId: String = ""
    internal var _clientSecret: String = ""
    internal var _authorizeUrl: Url? = null
    internal var _tokenUrl: Url? = null
    internal var _redirectUrl: Url? = null
    internal var _scopes: List<String> = emptyList()
    internal var _state: String? = null
    internal var _userAuthorize: (suspend (String, Url, List<String>, Url) -> String)? = { clientId, redirectUrl, scopes, authorizeUrl ->
        waitForOAuth_2_0_CallbackUsingKtorCIOEngineAndOpenUrl(
            clientId = clientId,
            callbackHost = redirectUrl.host,
            callbackPort = redirectUrl.port,
            callbackPath = redirectUrl.encodedPath,
            scopes = scopes,
            oauthUserAuthorizationUrl = authorizeUrl
        )
    }

    internal var _loadTokens: (suspend () -> BearerTokens?)? = null
    internal var _saveTokens: (suspend (BearerTokens) -> Unit)? = null

    fun clientId(value: String) {
        _clientId = value
    }

    fun clientSecret(value: String) {
        _clientSecret = value
    }

    fun authorizeUrl(value: Url) {
        _authorizeUrl = value
    }

    fun userAuthorize(value: suspend (String, Url, List<String>, Url) -> String) {
        _userAuthorize = value
    }

    fun tokenUrl(value: Url) {
        _tokenUrl = value
    }

    fun redirectUrl(value: Url) {
        _redirectUrl = value
    }

    fun scopes(vararg values: String) {
        _scopes = values.toList()
    }

    fun state(value: String) {
        _state = value
    }

    fun loadTokens(block: suspend () -> BearerTokens?) {
        _loadTokens = block
    }

    fun saveTokens(block: suspend (BearerTokens) -> Unit) {
        _saveTokens = block
    }
}

private val logger = logger("oauth_2_0")
private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String? = null
)

/**
 * Configures OAuth 2.0 authentication using Ktor's Auth plugin with Bearer tokens.
 */
fun AuthConfig.oauth2(config: OAuth_2_0_Config.() -> Unit) {
    val cfg = OAuth_2_0_Config().apply(config)

    bearer {
        loadTokens {
            cfg._loadTokens?.invoke()
        }

        refreshTokens {
            val currentTokens = oldTokens ?: return@refreshTokens null
            val refreshToken = currentTokens.refreshToken
            if (refreshToken.isNullOrEmpty()) {
                // No refresh token, perform full authorization flow
                val newTokens = performAuthorizationCodeFlow(client, cfg)
                cfg._saveTokens?.invoke(newTokens)
                newTokens
            } else {
                val newTokens = performTokenRefresh(client, cfg, refreshToken)
                cfg._saveTokens?.invoke(newTokens)
                newTokens
            }
        }

        sendWithoutRequest { request ->
            // Send bearer token with all requests (can be customized)
            true
        }
    }
}

suspend fun waitForOAuth_2_0_CallbackUsingKtorCIOEngineAndOpenUrl(
    clientId: String,
    scopes: List<String>,
    callbackHost: String = "127.0.0.1",
    callbackPort: Int = 9000,
    callbackPath: String = "/callback",
    oauthUserAuthorizationUrl: Url
): String {
    val redirectUrl = URLBuilder().apply {
        host = callbackHost
        port = callbackPort
        path(callbackPath)
    }.build()
    val result = waitForCallbackUsingKtorCIOEngineAndOpenUrl(
        callbackHost, callbackPort, callbackPath,
        oauthUserAuthorizationUrl
    ) {
        append("client_id", clientId)
        append("redirect_uri", redirectUrl.toString())
        append("response_type", "code")
        append("scope", scopes.joinToString(" "))
        append("access_type", "offline")
        append("prompt", "consent")
    }
    return result["code"]!![0]
}

@OptIn(ExperimentalEncodingApi::class)
private suspend fun performAuthorizationCodeFlow(
    client: HttpClient,
    cfg: OAuth_2_0_Config
): BearerTokens {
    val clientId = cfg._clientId.ifEmpty { error("clientId not set") }
    val clientSecret = cfg._clientSecret.ifEmpty { error("clientSecret not set") }
    val authorizeUrl = cfg._authorizeUrl ?: error("authorizeUrl not set")
    val userAuthorize = cfg._userAuthorize ?: error("userAuthorize function not set")
    val tokenUrl = cfg._tokenUrl ?: error("tokenUrl not set")
    val redirectUrl = cfg._redirectUrl ?: error("redirectUrl not set")
    val scopes = cfg._scopes
    val state = cfg._state

    try {
        val authorizationCode = userAuthorize.invoke(clientId, redirectUrl, scopes, authorizeUrl)
        logger.logTrace { "Received authorization code: $authorizationCode" }
        // Exchange code for tokens
        val tokenResponse = client.post(tokenUrl) {
            parameter("client_id", clientId)
            parameter("client_secret", clientSecret)
            parameter("code", authorizationCode)
            parameter("grant_type", "authorization_code")
            parameter("redirect_uri", redirectUrl.toString())
            parameter("access_type", "offline")
        }
        val tokensStr = when (tokenResponse.status) {
            HttpStatusCode.OK -> tokenResponse.bodyAsText()
            else -> error("Failed to exchange authorization code for tokens: ${tokenResponse.status}")
        }
        val tokens = json.decodeFromString<TokenResponse>(tokensStr)
        logger.logTrace { "Obtained access token (expires_in=${tokens.expiresIn})" }
        return BearerTokens(tokens.accessToken, tokens.refreshToken ?: "")
    } finally {

    }
}

@OptIn(ExperimentalEncodingApi::class)
private suspend fun performTokenRefresh(
    client: HttpClient,
    cfg: OAuth_2_0_Config,
    refreshToken: String
): BearerTokens {
    val clientId = cfg._clientId.ifEmpty { error("clientId not set") }
    val clientSecret = cfg._clientSecret.ifEmpty { error("clientSecret not set") }
    val tokenUrl = cfg._tokenUrl ?: error("tokenUrl not set")

    logger.logTrace { "Refreshing access token" }

    val refreshResponse = client.post(tokenUrl) {
        parameter("client_id", clientId)
        parameter("client_secret", clientSecret)
        parameter("grant_type", "refresh_token")
        parameter("refresh_token", refreshToken)
    }
    val newTokens = when (refreshResponse.status) {
        HttpStatusCode.OK -> refreshResponse.body<TokenResponse>()
        else -> error("Unexpected response: $refreshResponse")
    }

    logger.logTrace { "Refreshed access token (expires_in=${newTokens.expiresIn})" }

    return BearerTokens(newTokens.accessToken, newTokens.refreshToken ?: refreshToken)
}

