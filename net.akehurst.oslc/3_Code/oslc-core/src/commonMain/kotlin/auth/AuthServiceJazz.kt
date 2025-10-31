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

package net.akehurst.oslc.core.auth

import io.ktor.client.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import net.akehurst.kotlin.json.Json

/**
 * AuthService interactions with the Jazz Authorization Server (JAS) for OAuth 2.0.
 *
 * NOTE: This assumes the Jazz ELM environment is configured with JSA SSO/JAS, enabling
 * the use of Bearer Tokens (OAuth 2.0) instead of OAuth 1.0a signing.
 */
class AuthServiceJazz(
    private val rawClient: HttpClient,
    val jasHost: String,
    private val clientId: String,
    private val clientSecret: String
) {

    // 🚨 Jazz ELM Endpoints (these are typically served by the Jazz Authorization Server/JAS)
    private val tokenEndpoint = "$jasHost/token"

    @Serializable
    private data class TokenResponse(
        val access_token: String?,
        val token_type: String?,
        val expires_in: Int?,
        val refresh_token: String? = null,
        val scope: String?
    )

    /**
     * Executes the Token Refresh Grant flow.
     */
    suspend fun refreshToken(refreshToken: String): BearerTokens? {
        println("   > AUTH: Requesting new token via Refresh Token grant...")

        // This is a real-world pattern for OAuth 2.0 Refresh: POST request with form data
        val response = rawClient.post(tokenEndpoint) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "grant_type" to "refresh_token",
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "refresh_token" to refreshToken
                ).formUrlEncode()
            )
        }

        return if (response.status.isSuccess()) {
            val resp = decodeJson(response.bodyAsText())
            if (resp.access_token == null) {
                null
            } else {
                BearerTokens(
                    accessToken = resp.access_token,
                    refreshToken = resp.refresh_token ?: refreshToken // Often reuses the refresh token
                )
            }
        } else {
            println("   > AUTH: Refresh failed with status: ${response.status}")
            null
        }
    }

    /**
     * Executes the initial token acquisition using the Client Credentials Grant.
     * This is suitable for server-to-server or background applications that don't need a user interface.
     * * @param scope The required resource scopes (e.g., "openid profile general").
     */
    suspend fun obtainInitialToken(scope: String): BearerTokens? {
        println("   > AUTH: Requesting initial token via Client Credentials grant...")

        val response = rawClient.post(tokenEndpoint) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "grant_type" to "client_credentials",
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "scope" to scope
                ).formUrlEncode()
            )
        }

        return if (response.status.isSuccess()) {
            val resp = decodeJson(response.bodyAsText())
            if (resp.access_token == null) {
                null
            } else {
                BearerTokens(
                    accessToken = resp.access_token,
                    refreshToken = resp.refresh_token ?: "" // Client Credentials rarely returns a refresh token
                )
            }
        } else {
            println("   > AUTH: Initial token acquisition failed with status: ${response.status}")
            println("   > AUTH: Error Body: ${response.bodyAsText()}")
            null
        }
    }

    /**
     * Helper function to correctly URL-encode a list of form parameters.
     */
    private fun List<Pair<String, String>>.formUrlEncode(): String =
        this.joinToString("&") { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        }

    private fun decodeJson(jsonString: String): TokenResponse {
        val json = Json.process(jsonString)
        return TokenResponse(
            access_token = json.root.asObject().property["access_token"]?.asString()?.value,
            token_type = json.root.asObject().property["token_type"]?.asString()?.value,
            expires_in = json.root.asObject().property["expires_in"]?.asNumber()?.toInt(),
            refresh_token = json.root.asObject().property["refresh_token"]?.asString()?.value,
            scope = json.root.asObject().property["scope"]?.asString()?.value,
        )
    }
}
