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

package net.akehurst.oslc.core

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.akehurst.oslc.api.OslcClient
import net.akehurst.oslc.api.RdfContentType
import net.akehurst.oslc.core.auth.oauth_1_0a.OAuth_1_0a
import net.akehurst.oslc.core.util.waitForCallbackUsingKtorCIOEngineAndOpenUrl
import net.akehurst.oslc.rdf.api.RdfGraph
import net.akehurst.oslc.rdf.api.RdfStructure
import net.akehurst.oslc.rdf.turtle.TurtleLanguage
import net.akehurst.oslc.rdf.xml.Xml2Rdf_v1_1

fun oslcClient(
    httpClient: HttpClient = HttpClient(CIO),
    baseUrl: Url,
    rootservices: String = "/rootservices",
    requestBuilder: HttpRequestBuilder.() -> Unit = {}
): OslcClient = OslcClientDefault(httpClient, baseUrl, rootservices, requestBuilder)

internal class OslcClientDefault(
    initialHttpClient: HttpClient,
    override val baseUrl: Url,
    val rootservices: String,
    val requestBuilder: HttpRequestBuilder.() -> Unit
) : OslcClient {

    override val issues = mutableListOf<String>()

    var httpClient: HttpClient = initialHttpClient

    override suspend fun rootServicesGraph(): RdfGraph? {
        val rsUrl = Url(baseUrl.toString() + rootservices)
        return fetchRdfGraphFromUrl(rsUrl)
    }

    override suspend fun rootServicesStructure(): RdfStructure? {
        val rdf = rootServicesGraph()
        val mdl = rdf?.asModel()
        val res = mdl?.findStructureWithIdentity(baseUrl.toString() + rootservices)
        return res
    }

    override suspend fun fetchRdfGraphFromUrl(url: Url): RdfGraph? {
        val response = httpClient.get(url) {
            requestBuilder.invoke(this)
        }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val ct = response.contentType()
                when {
                    null == ct//assume rdf+xml if nothing specified
                            || ct.match(RdfContentType.APPLICATION_RDF_XML)
                            || ct.match("application/x-oslc-disc-service-provider-catalog+xml")
                        -> {
                        val body = response.bodyAsText()
                        Xml2Rdf_v1_1.convert(body)
                    }

                    ct.match(RdfContentType.TEXT_TURTLE) -> {
                        val body = response.bodyAsText()
                        TurtleLanguage.processor.process(body).let {
                            if (issues.isEmpty()) {
                                it.asm!!
                            } else {
                                it.allIssues.forEach { iss ->
                                    issues.add(iss.toString())
                                }
                                null
                            }
                        }
                    }

                    else -> {
                        issues.add("Error $url returns an unsupported ContentType: $ct")
                        null
                    }
                }
            }

            else -> {
                issues.add("Error performing GET on $url, response.status: ${response.status}")
                null
            }
        }
    }

    override fun authoriseBasic(username: String, password: String, realm:String?) {
        this.httpClient = httpClient.config {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = username, password = password)
                    }
                    sendWithoutRequest { true }
                    realm?.let { this.realm = it }
                }
            }
        }
    }

    override fun authoriseOauth_1_0a(
        consumerKey: String,
        consumerSecret: String,
        oauthRequestTokenUrl: Url,
        oauthUserAuthorizationUrl: Url,
        userAuthorizeCallbackUrl: Url,
        oauthAccessTokenUrl: Url,
        realm: String?,
    ) {
        this.httpClient = httpClient.config {
            install(OAuth_1_0a) {
                consumerKey(consumerKey)
                consumerSecret(consumerSecret)
                oauthRequestTokenUrl(oauthRequestTokenUrl.toString())
                userAuthorizeCallbackUrl(userAuthorizeCallbackUrl.toString())
                oauthAccessTokenUrl(oauthAccessTokenUrl.toString())
                userAuthorize { client, token ->
                    waitForCallbackUsingKtorCIOEngineAndOpenUrl(oauthUserAuthorizationUrl,token,userAuthorizeCallbackUrl.host,userAuthorizeCallbackUrl.port, userAuthorizeCallbackUrl.fullPath)
                }
                realm(realm)
            }
        }
    }
}
