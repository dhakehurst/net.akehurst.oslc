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

package net.akehurst.oslc.api.v3_0.shapes

import net.akehurst.oslc.api.v3_0.vocab.*


/**
 * Service Provider Catalog
 */
interface ServiceProviderCatalog {
    val title: String?
    val description: String?
    val domain: Resource?
    val serviceProviderCatalog: AnyResource?
    val oauthConfiguration: AnyResource?
    val publisher: AnyResource?
    val serviceProvider: AnyResource?

    val entry: List<ServiceProvider>
}

interface ServiceProvider {
    val title: String?
    val description: String?
    val details: Resource?
    val service: AnyResource
    val publisher: AnyResource
    val prefixDefinition: AnyResource
    val oauthConfiguration: AnyResource
}