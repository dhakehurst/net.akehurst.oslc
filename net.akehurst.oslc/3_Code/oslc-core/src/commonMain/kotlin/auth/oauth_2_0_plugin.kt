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
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.akehurst.kotlinx.logging.api.logger
import net.akehurst.oslc.core.util.waitForCallbackUsingKtorCIOEngineAndOpenUrl
import kotlin.random.Random


class OAuth_2_0_Config {
    internal var _clientId: String = ""
    internal var _clientSecret: String = ""
    internal var _authorizeUrl: Url? = null
    internal var _tokenUrl: Url? = null
    internal var _redirectUrl: Url? = null
    internal var _scopes: List<String> = emptyList()
    internal var _authorizationRequestParameters: ParametersBuilder.() -> Unit = {
        // these are required for google
        //append("access_type", "offline")
        //append("prompt", "consent")
    }
    internal var _state: String = generateRandomState()  // Always generate new state for security
    internal var _userAuthorize: (suspend (String, Url, List<String>, Url, String) -> String)? = { clientId, redirectUrl, scopes, authorizeUrl, state ->
        waitForOAuth_2_0_CallbackUsingKtorCIOEngineAndOpenUrl(
            clientId = clientId,
            callbackHost = redirectUrl.host,
            callbackPort = redirectUrl.port,
            callbackPath = redirectUrl.encodedPath,
            scopes = scopes,
            state = state,
            authorizationRequestParameters = _authorizationRequestParameters,
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

    fun userAuthorize(value: suspend (String, Url, List<String>, Url, String) -> String) {
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

    fun authorizationRequestParameters(value: ParametersBuilder.() -> Unit) {
        _authorizationRequestParameters = value
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

/**
 * Generates a cryptographically random state string for CSRF protection.
 */
private fun generateRandomState(length: Int = 32): String {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~"
    return (1..length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}

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
            val loaded = cfg._loadTokens?.invoke()
            if (loaded != null && loaded.accessToken.isNotEmpty()) loaded else null
        }

        refreshTokens {
            val refreshToken = oldTokens?.refreshToken
            if (refreshToken.isNullOrEmpty()) {
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
            // Send bearer token to all requests except the OAuth callback redirect
            request.url.host != cfg._redirectUrl?.host
        }
    }
}

suspend fun waitForOAuth_2_0_CallbackUsingKtorCIOEngineAndOpenUrl(
    clientId: String,
    scopes: List<String>,
    state: String,
    authorizationRequestParameters: ParametersBuilder.() -> Unit = {},
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
        append("state", state)
        authorizationRequestParameters()
    }

    // Validate state to prevent CSRF attacks
    val returnedState = result["state"]?.firstOrNull()
        ?: error("OAuth callback missing state parameter")
    if (returnedState != state) {
        error("OAuth state mismatch: possible CSRF attack detected")
    }

    // Check for authorization errors
    val error = result["error"]?.firstOrNull()
    if (error != null) {
        val errorDescription = result["error_description"]?.firstOrNull() ?: "Unknown error"
        error("OAuth authorization failed: $error - $errorDescription")
    }

    return result["code"]?.firstOrNull()
        ?: error("OAuth callback missing authorization code")
}

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

    val authorizationCode = userAuthorize.invoke(clientId, redirectUrl, scopes, authorizeUrl, state)

    // Exchange code for tokens using form-encoded body (per OAuth 2.0 spec)
    val tokenResponse = client.submitForm(
        url = tokenUrl.toString(),
        formParameters = io.ktor.http.Parameters.build {
            append("client_id", clientId)
            append("client_secret", clientSecret)
            append("code", authorizationCode)
            append("grant_type", "authorization_code")
            append("redirect_uri", redirectUrl.toString())
        }
    )

    val tokensStr = when (tokenResponse.status) {
        HttpStatusCode.OK -> tokenResponse.bodyAsText()
        else -> error("Failed to exchange authorization code for tokens: ${tokenResponse.status}")
    }
    val tokens = json.decodeFromString<TokenResponse>(tokensStr)
    logger.logTrace { "Obtained access token (expires_in=${tokens.expiresIn})" }
    return BearerTokens(tokens.accessToken, tokens.refreshToken ?: "")
}

private suspend fun performTokenRefresh(
    client: HttpClient,
    cfg: OAuth_2_0_Config,
    refreshToken: String
): BearerTokens {
    val clientId = cfg._clientId.ifEmpty { error("clientId not set") }
    val clientSecret = cfg._clientSecret.ifEmpty { error("clientSecret not set") }
    val tokenUrl = cfg._tokenUrl ?: error("tokenUrl not set")

    logger.logTrace { "Refreshing access token" }

    val refreshResponse = client.submitForm(
        url = tokenUrl.toString(),
        formParameters = io.ktor.http.Parameters.build {
            append("client_id", clientId)
            append("client_secret", clientSecret)
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
        }
    )
    val tokensStr = when (refreshResponse.status) {
        HttpStatusCode.OK -> refreshResponse.bodyAsText()
        else -> error("Token refresh failed: ${refreshResponse.status}")
    }
    val newTokens = json.decodeFromString<TokenResponse>(tokensStr)

    logger.logTrace { "Refreshed access token (expires_in=${newTokens.expiresIn})" }

    return BearerTokens(newTokens.accessToken, newTokens.refreshToken ?: refreshToken)
}

