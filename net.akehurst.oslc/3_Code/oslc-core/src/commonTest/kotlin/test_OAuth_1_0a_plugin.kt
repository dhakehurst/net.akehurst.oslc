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
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.Url
import net.akehurst.oslc.core.util.waitForCallbackUsingKtorCIOEngineAndOpenUrl
import kotlin.test.Test

class test_OAuth_1_0a_plugin {

    @Test
    fun t() {
        HttpClient(CIO) {
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(OAuth_1_0a) {
                consumerKey("")
                consumerSecret("")
                oauthRequestTokenUrl("")
                userAuthorizeCallbackUrl("")
                oauthAccessTokenUrl("")
                realm("")
                userAuthorize { client, token ->
                    waitForCallbackUsingKtorCIOEngineAndOpenUrl(Url(""),token)
                }
            }
        }

    }

}