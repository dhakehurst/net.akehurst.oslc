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

package net.akehurst.oslc.api

import io.ktor.http.ContentType
import io.ktor.http.Url
import net.akehurst.oslc.rdf.api.RdfGraph
import net.akehurst.oslc.rdf.api.RdfStructure

object RdfContentType {
    val APPLICATION_RDF_XML = ContentType("application", "rdf+xml")
    val TEXT_TURTLE = ContentType("text", "turtle")
    val APPLICATION_JSON_LD = ContentType("application", "ld+json")
    val APPLICATION_N_TRIPLES = ContentType("application", "n-triples")
}

interface OslcClient {
    val baseUrl: Url

    val issues: List<String>

    suspend fun rootServicesGraph(): RdfGraph?
    suspend fun rootServicesStructure(): RdfStructure?

    suspend fun fetchRdfGraphFromUrl(url: Url): RdfGraph?

    fun authoriseBasic(username: String, password: String, realm: String?)
    fun authoriseOauth_1_0a(
        consumerKey: String,
        consumerSecret: String,
        oauthRequestTokenUrl: Url,
        oauthUserAuthorizationUrl: Url,
        userAuthorizeCallbackUrl: Url,
        oauthAccessTokenUrl: Url,
        realm: String?,
    )
}

