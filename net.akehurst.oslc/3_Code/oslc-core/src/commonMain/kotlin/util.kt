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
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
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

suspend fun waitForCallbackUsingKtorCIOEngineAndOpenUrl(
    callbackHost: String = "127.0.0.1",
    callbackPort: Int = 9000,
    callbackPath: String = "/callback",
    oauthUserAuthorizationUrl: Url,
    parameterBuilder: ParametersBuilder.() -> Unit = {}
) = coroutineScope {
    val callback = async { waitForCallback(io.ktor.server.cio.CIO, callbackHost, callbackPort, callbackPath) }
    val urlWithParams = URLBuilder(oauthUserAuthorizationUrl).apply {
        parameters.parameterBuilder()
    }.buildString()
    openUrl(urlWithParams)
    val params = callback.await()
    params
}

/**
 * Sets up an io.ktor.server.cio.CIO embeddedServer listening on the given port and path.
 * Will stop the server and return params when they are received.
 */
suspend fun <TEngine : ApplicationEngine, TConfiguration : ApplicationEngine.Configuration> waitForCallback(
    factory: ApplicationEngineFactory<TEngine, TConfiguration>,
    host: String,
    port: Int,
    path: String
) = coroutineScope {
    val receivedParams = CompletableDeferred<Parameters>()
    val server = embeddedServer(factory, port, host) {
        routing {
            get(path) {
                val params = call.request.queryParameters
                call.respondText("Authorization successful! You can close this window now.")
                receivedParams.complete(params)
            }
        }
    }
    val serverJob = launch { server.startSuspend(wait = false) }
    val params = receivedParams.await()
    server.stop(100, 100)
    serverJob.cancel()
    return@coroutineScope params.toMap()
}