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

package net.akehurst.oslc.core.util

import io.ktor.http.Parameters
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.toMap
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.kotlincrypto.macs.hmac.sha1.HmacSHA1
import kotlin.io.encoding.Base64

expect fun openUrl(url: String)

fun generateHmacSha1Signature(data: String, key: String): String {
    val mac = HmacSHA1(key.toByteArray(Charsets.UTF_8))
    val rawHmac = mac.doFinal(data.toByteArray(Charsets.UTF_8))
    return Base64.encode(rawHmac)
}

//@OptIn(ExperimentalUuidApi::class)
//fun generateNonce(): String = kotlin.uuid.Uuid.random().toString().replace("-", "")

suspend fun waitForCallbackUsingKtorCIOEngineAndOpenUrl(oauthUserAuthorizationUrl: String, request_token: String) = coroutineScope {
    val callback = async { waitForCallbackUsingKtorCIOEngine(9000, "/callback") }
    openUrl(oauthUserAuthorizationUrl + "?oauth_token=$request_token")
    val params = callback.await()
    params["oauth_verifier"]!![0]
}

/**
 * Sets up an io.ktor.server.cio.CIO embeddedServer listening on the given port and path.
 * Will stop the server and return params when they are received.
 */
suspend fun waitForCallbackUsingKtorCIOEngine(port: Int, path: String) = coroutineScope {
    val receivedParams = CompletableDeferred<Parameters>()
    val server = embeddedServer(io.ktor.server.cio.CIO, port = port, host = "127.0.0.1") {
        routing {
            get(path) {
                val params = call.request.queryParameters
                call.respondText("Success! You can close this window now.")
                receivedParams.complete(params)
            }
        }
    }
    val serverJob = launch { server.start(wait = false) }
    val params = receivedParams.await()
    server.stop(100, 100)
    serverJob.cancel()
    return@coroutineScope params.toMap()
}