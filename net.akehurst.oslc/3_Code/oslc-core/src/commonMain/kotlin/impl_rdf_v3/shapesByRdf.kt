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

package net.akehurst.oslc.by.rdf.v3_0.shapes

import net.akehurst.oslc.rdf.api.RdfResource
import net.akehurst.oslc.rdf.api.RdfStructure
import net.akehurst.oslc.api.v3_0.vocab.*
import net.akehurst.oslc.api.v3_0.shapes.ServiceProvider
import net.akehurst.oslc.api.v3_0.shapes.ServiceProviderCatalog

class ServiceProviderCatalogRdf(
    val rdf: RdfStructure
) : ServiceProviderCatalog {
    override val title: String? by lazy { rdf.getPropertyFirstAsStringOrNull("dcterms:title") }
    override val description: String? by lazy { rdf.getPropertyFirstAsStringOrNull("dcterms:description") }
    override val domain: Resource get() = TODO("not implemented")
    override val publisher: AnyResource get() = TODO("not implemented")
    override val serviceProvider: AnyResource get() = TODO("not implemented")
    override val serviceProviderCatalog: AnyResource get() = TODO("not implemented")
    override val oauthConfiguration: AnyResource get() = TODO("not implemented")

    override val entry: List<ServiceProvider> by lazy {
        rdf.getPropertyFirstAsListOrNull("oslc_disc:entry")?.map {
            ServiceProviderRdf(it as RdfStructure)
        } ?: emptyList()
    }
}

class ServiceProviderRdf(
    val rdf: RdfStructure
) : ServiceProvider {
    override val title: String? by lazy { rdf.getPropertyFirstAsString("dcterms:title") }
    override val description: String? by lazy { rdf.getPropertyFirstAsString("dcterms:description") }
    override val details: Resource? get() = TODO("not implemented")
    override val oauthConfiguration: AnyResource get() = TODO("not implemented")
    override val prefixDefinition: AnyResource get() = TODO("not implemented")
    override val publisher: AnyResource get() = TODO("not implemented")
    override val service: AnyResource get() = TODO("not implemented")
}