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

package net.akehurst.oslc.by.rdf.v2_0

import net.akehurst.oslc.rdf.api.RdfResource
import net.akehurst.oslc.rdf.api.RdfStructure
import net.akehurst.oslc.api.v2_0.common.*
import net.akehurst.oslc.api.v2_0.shapes.*
import net.akehurst.oslc.rdf.asm.RdfResourceDefault

data class ResourceByRdf(
    val rdf:RdfResource
) : Resource

data class ServiceProviderCatalogRdf(
    val rdf: RdfStructure
) : ServiceProviderCatalog {
    override val title: String? by lazy { rdf.getPropertyFirstAsStringOrNull("dcterms:title") }
    override val description: String? by lazy { rdf.getPropertyFirstAsStringOrNull("dcterms:description") }
    override val domain: List<Resource> get() = TODO("not implemented")
    override val publisher: LocalResource? get() = TODO("not implemented")
    override val serviceProvider: List<ServiceProvider> by lazy {
        rdf.getPropertyFirstAsListOrNull("oslc:serviceProvider")?.map {
            ServiceProviderRdf(it as RdfStructure)
        }
            ?: rdf.getPropertyFirstAsListOrNull("oslc_disc:entry")?.map {
                ServiceProviderRdf(it as RdfStructure)
            }
            ?: emptyList()
    }
    override val serviceProviderCatalog: List<ServiceProviderCatalog> get() = TODO("not implemented")
    override val oauthConfiguration: List<OAuthConfiguration> get() = TODO("not implemented")

    override fun asString(): String = """
        ServiceProviderCatalog:
            title: $title
            description: $description
            serviceProviders: [
                ${serviceProvider.joinToString("\n                ")}
            ]
    """.trimIndent()

    override fun toString(): String = "ServiceProviderCatalog(${rdf.identity})"
}

data class ServiceProviderRdf(
    val rdf: RdfStructure
) : ServiceProvider {
    override val title: String? by lazy { rdf.getPropertyFirstAsString("dcterms:title") }
    override val description: String? by lazy { rdf.getPropertyFirstAsString("dcterms:description") }
    override val details: Resource? by lazy{ ResourceByRdf(RdfResourceDefault(rdf.getPropertyFirstAsRdfStructure("oslc_disc:details").identity)) }
    override val oauthConfiguration: AnyResource get() = TODO("not implemented")
    override val prefixDefinition: AnyResource get() = TODO("not implemented")
    override val publisher: AnyResource get() = TODO("not implemented")
    override val service: AnyResource get() = TODO("not implemented")

    override fun asString(): String ="""
        ServiceProvider:
            title: $title
            description: $description
            details: $details
    """.trimIndent()

    override fun toString(): String = "ServiceProvider(${details})"
}