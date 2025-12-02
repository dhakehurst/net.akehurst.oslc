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

package net.akehurst.oslc.api.v2_0.shapes

import io.ktor.http.Url
import net.akehurst.oslc.api.v2_0.common.*

// https://archive.open-services.net/bin/view/Main/OslcCoreSpecification.html#Resource_Service_Provider


/**
 * Service Provider Catalog
 */
interface ServiceProviderCatalog {
    val title: String?
    val description: String?
    val publisher: Publisher?
    val domain: List<Resource<Url>>
    val serviceProvider: List<Resource<ServiceProvider>>
    val serviceProviderCatalog: List<Resource<ServiceProviderCatalog>>
    val oauthConfiguration: List<OAuthConfiguration>

    fun asString(): String
}

interface ServiceProvider {
    val title: String?
    val description: String?
    val details : Resource<ServiceProvider>?
    val service: Service
    val publisher: Publisher
    val prefixDefinition: PrefixDefinition
    val oauthConfiguration: OAuthConfiguration

    fun asString(): String
}

interface OAuthConfiguration

interface Publisher

interface Service

interface PrefixDefinition