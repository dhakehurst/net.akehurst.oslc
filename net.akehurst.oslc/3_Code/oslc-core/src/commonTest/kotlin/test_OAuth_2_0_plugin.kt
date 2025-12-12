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
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.Url
import io.ktor.http.parameters
import net.akehurst.oslc.core.util.waitForCallbackUsingKtorCIOEngineAndOpenUrl
import kotlin.test.Test


class test_OAuth_2_0_plugin {

    @Test
    fun t() {
        HttpClient(CIO) {
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(Auth) {
                oauth2 {
                    authorizeUrl(Url(""))
                    redirectUrl(Url(""))
                    tokenUrl(Url(""))
                    clientId("")
                    clientSecret("")
                    scopes("","")
                    loadTokens {
                        //get tokens from secure storage
                        null
                    }
                    saveTokens { tokens ->
                        // store tokens in secure storage
                    }

                    //optional overide
                    userAuthorize { clientId, redirectUrl, scopes, authorizeUrl ->
                        waitForOAuth_2_0_CallbackUsingKtorCIOEngineAndOpenUrl(
                            clientId = clientId,
                            callbackHost = redirectUrl.host,
                            callbackPort = redirectUrl.port,
                            callbackPath = redirectUrl.encodedPath,
                            scopes = scopes,
                            oauthUserAuthorizationUrl = authorizeUrl
                        )
                    }
                }
            }
        }

    }
}