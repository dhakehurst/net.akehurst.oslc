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
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.client.plugins.auth.providers.BearerTokens

/**
 * Global mutex to prevent multiple simultaneous token refresh requests.
 */
private val refreshMutex = Mutex()

interface AuthService {
    suspend fun obtainInitialToken(code: String): BearerTokens?
    suspend fun refreshToken(refreshToken: String): BearerTokens?
}

/**
 * Creates and configures the Ktor HttpClient with OAuth 2.0 Bearer Authentication.
 *
 * @param tokenManager The manager responsible for persisting and providing tokens.
 * @param authService The service responsible for executing the token refresh request.
 */
fun createOAuth_2_0_AuthenticatedClient(
    tokenManager: AuthTokenManager,
    authService: AuthService
): HttpClient {
    return HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.INFO
        }

        install(Auth) {
            bearer {
                // 1. Load Tokens: Ktor calls this before every request to get the current token.
                loadTokens {
                    tokenManager.loadTokens()
                }

                // 2. Token Refresh: Ktor calls this when a 401 Unauthorized is received.
                refreshTokens {
                    // Use a mutex to ensure only one thread attempts to refresh the token.
                    return@refreshTokens refreshMutex.withLock {
                        val currentTokens = tokenManager.loadTokens()
                        val refreshToken = currentTokens?.refreshToken.takeIf { it?.isNotBlank() == true }

                        if (refreshToken == null) {
                            // Cannot refresh, prompt for re-login (e.g., throw exception or return null)
                            println("No refresh token available. Failed to refresh.")
                            return@withLock null
                        }

                        println("Attempting token refresh...")

                        // Execute the refresh logic
                        val newTokens = authService.refreshToken(refreshToken)

                        if (newTokens != null) {
                            // Save the new tokens and return them to Ktor
                            tokenManager.saveTokens(newTokens)
                            newTokens
                        } else {
                            // Refresh failed
                            tokenManager.clearTokens()
                            null
                        }
                    }
                }

                // Optional: Define which hosts should receive the Bearer header
                sendWithoutRequest { request ->
                    // Only send Bearer token to our protected API host
                    request.url.host == "api.oauth2provider.com"
                }
            }
        }
    }
}
