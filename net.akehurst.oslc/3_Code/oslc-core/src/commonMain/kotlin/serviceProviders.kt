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

package net.akehurst.oslc.v3_0.core.api

/*
OSLC Core Version 3.0. Part 1: Overview. https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/oslc-core.html
OSLC Core Version 3.0. Part 2: Discovery (this document). https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/discovery.html
OSLC Core Version 3.0. Part 3: Resource Preview. https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/resource-preview.html
OSLC Core Version 3.0. Part 4: Delegated Dialogs. https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/dialogs.html
OSLC Core Version 3.0. Part 5: Attachments. https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/attachments.html
OSLC Core Version 3.0. Part 6: Resource Shape. https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/resource-shape.html
OSLC Core Version 3.0. Part 7: Vocabulary. https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/core-vocab.html
OSLC Core Version 3.0. Part 8: Constraints. https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/core-shapes.html
OSLC Core Version 3.0. Machine Readable Vocabulary Terms. https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/core-vocab.ttl
OSLC Core Version 3.0. Machine Readable Constraints. https://docs.oasis-open-projects.org/oslc-op/core/v3.0/os/core-shapes.tt
*/

//TODO: change to value class when they work properly in JS and wasmJS
data class URI(val value: String)

/**
 * An LDPC describing an OSLC server that offers one or more ServiceProvider LDPCs. Servers mAy also organize
 * the ServiceProviders in one or more ServiceProviderCatalog LDPCs to enable OSLC clients to find ServiceProviders offered.
 * The members of these catalogs may include other nested catalogs as well as service providers.
 */
interface ServiceProviderCatalog {
    /**
     * Description of the services provided.
     *
     * dcterms:description
     */
    val description: String?

    /**
     * Describes the software product that provides the implementation.
     *
     * dcterms:publisher
     */
    val publisher:Publisher

    /**
     * Title of this resource.
     *
     * dcterms:title
     */
    val title: String?

    /**
     * Namespace URI of the specification that is implemented by this service.
     * In most cases this namespace URI will be for an OSLC domain,
     * but other URIs may be used.
     *
     *  oslc:domain
     */
    val domain: List<URI>

    /**
     * Defines the three OAuth URIs required for a client to act as an OAuth consumer.
     *
     * oslc:oauthConfiguration
     */
    val oauthConfiguration: List<OAuthConfiguration>

    /**
     * A service provider LDPC offered by this server
     *
     * oslc:serviceProvider
     */
    val serviceProvider: List<ServiceProvider>

    /**
     * Additional service provider catalog LDPCs used to organize services.
     *
     * oslc:serviceProviderCatalog
     */
    val serviceProviderCatalog: List<ServiceProviderCatalog>
}

/**
 * An LDPC whose members are the Service LDPCs offered by an OSLC server.
 */
interface ServiceProvider {
    /**
     * Description of the services provided.
     *
     * dcterms:description [0..1]
     */
    val description: String?

    /**
     * Describes the software product that provides the implementation.
     *
     * dcterms:publisher [0..1]
     */
    val publisher: Publisher?

    /**
     * Title of this resource.
     *
     * dcterms:title [0..1]
     */
    val title: String?

    /**
     * A URL that may be used to retrieve a resource to determine additional
     * details about the service provider such as a web page describing it.
     *
     * oslc:details [0..*]
     */
    val details: List<URI>

    /**
     * Defines the three OAuth URIs required for a client to act as an OAuth consumer.
     *
     * oslc:oauthConfiguration [0..*]
     */
    val oauthConfiguration: List<OAuthConfiguration>

    /**
     * Defines a namespace prefix for use in JSON representations and in forming OSLC Query Syntax strings.
     *
     * oslc:prefixDefinition [0..*]
     */
    val prefixDefinition: List<String>

    /**
     * Describes a service LDPC offered by the service provider.
     *
     * oslc:service [1..*]
     */
    val service: List<Service>
}

/**
 * An LDPC whose properties describe specific services offered by a server, and the URIs to use for those services in
 * the context of that ServiceProvider.
 */
interface Service {
    val domain: URI

    val creationFactory: List<CreationFactory>
    val queryCapability: List<QueryCapability>
    val selectionDialog: List<Dialog>
    val creationDialog: List<Dialog>
}

interface OslcResource {
    val about: URI
    val shortTitle: String
    val shortId: String

    val serviceProvider: ServiceProvider
    val instanceShape: ResourceShape
    val modifiedBy: List<OslcResource>
}



interface AllowedValues

interface CreationFactory {
    val title: String
    val label: String
    val creation: URI
    val resourceType: URI
    val usage: URI

    val resourceShape: List<ResourceShape>
}

interface ResourceShape {
    val title: String
    val describes: URI

    val property: List<Property>
}

interface Property {
    val description: String
    val title: String
    val defaultValue: String
    val hidden: Boolean
    val isMemberProperty: Boolean
    val name: String
    val maxSize: Int
    val occurs: URI
    val propertyDefinition: URI
    val range: URI
    val readOnly: URI
    val representation: URI
    val valueType: URI
    val valueShape: URI

    val allowedValues: AllowedValues?
}

interface QueryCapability {
    val title: String
    val label: String
    val queryBase: URI
    val resourceType: URI
    val usage: URI

    val resourceShape: List<ResourceShape>
}

interface Dialog {
    val title: String
    val label: String
    val dialog: URI
    val hintWidth: Int
    val hintHeight: Int
    val resourceType: URI
    val usage: URI
}

interface Compact {
    val title: String
    val shortTitle: String
    val icon: URI

    val smallPreview: Preview?
    val largePreview: Preview?
}

interface Preview {
    val document: URI
    val hintWidth: Int
    val hintHeight: Int
    val initialHeight: Int
}

interface PrefixDefinition {
    /** oslc:prefix */
    val prefix: String

    /** oslc:prefixBase */
    val prefixBase: URI
}

interface OAuthConfiguration {
    /** oslc:authorizationURI */
    val authorizationURI: URI

    /** oslc:oauthAccessTokenURI  */
    val oauthAccessTokenURI: URI

    /** oslc:oauthRequestTokenURI */
    val oauthRequestTokenURI: URI
}

interface Publisher