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

package net.akehurst.oslc.api.v2_0.common

import io.ktor.http.Url
import net.akehurst.oslc.api.OslcClient
import net.akehurst.oslc.api.v2_0.shapes.ServiceProvider
import kotlin.time.Instant

/**
 * The object resource can be identified with either a URI or a blank node.
 */
interface AnyResource<T : Any> {
    val value: T?
    suspend fun fetch(client: OslcClient): T?
}

interface Resource<T : Any> : AnyResource<T> {
    val url: Url
}

interface LocalResource<T : Any> : AnyResource<T> {}

interface XMLLiteral {
    val value: Any
}

// https://archive.open-services.net/bin/view/Main/OSLCCoreSpecAppendixA.html


interface DublinCoreProperties {
    val contributor: AnyResource<Any>?
    val creator: AnyResource<Any>?
    val created: Instant?
    val description: String?
    val identifier: String?
    val modified: Instant?
    val relation: AnyResource<Any>?
    val subject: String?
    val title: String?
}

interface RdfProperties {
    val type: Resource<Any>?
}

interface RdfsProperties {
    val member: Resource<Any>?
}

interface OslcProperties {
    val serviceProvider: Resource<ServiceProvider>?
    val instanceShape: Resource<ResourceShape>?
    val shortTitle: String?
    val discussedBy: Resource<Discussion>?
    val modifiedBy: Resource<Any>?
}

interface Person {
    /** foaf:name */
    val name: String?

    /** foaf:givenName */
    val givenName: String?

    /** foaf:familyName */
    val familyName: String?
}

interface ResourceShape {
    /** dcterms:title — zero-or-one */
    val title: XMLLiteral?

    /** oslc:describes — zero-or-many; URIs of resource types */
    val describes: List<Resource<Url>>?

    /** oslc:property — zero-or-many inlined Property resources */
    val property: List<Resource<Property>>?
}

enum class OccursKind(val iri: String) {
    ZeroOrOne("http://open-services.net/ns/core#Zero-or-one"),
    ZeroOrMany("http://open-services.net/ns/core#Zero-or-many"),
    OneOrMany("http://open-services.net/ns/core#One-or-many"),
    ExactlyOne("http://open-services.net/ns/core#Exactly-one"),
}

interface Property {
    /** dcterms:description — zero-or-one */
    val description: String?

    /** dcterms:title — zero-or-one */
    val title: String?

    /** oslc:allowedValues — zero-or-one reference to AllowedValues resource */
    val allowedValues: Resource<AllowedValues>?

    /** oslc:allowedValue — zero-or-many inlined allowed values (same value-type as property) */
    val allowedValue: List<Any>?

    /** oslc:defaultValue — zero-or-one (same as property value-type) */
    val defaultValue: Any?

    /** oslc:hidden */
    val hidden: Boolean?

    /** oslc:isMemberProperty */
    val isMemberProperty: Boolean?

    /** oslc:name — exactly-one */
    val name: String

    /** oslc:maxSize */
    val maxSize: Int?

    /** oslc:occurs — exactly-one (URI from core vocabulary) */
    val occurs: OccursKind

    /** oslc:propertyDefinition — exactly-one URI of property being described */
    val propertyDefinition: Resource<Url>

    /** oslc:range — zero-or-many URIs (resource types) */
    val range: List<Resource<Url>>?

    /** oslc:readOnly (writable if not set) */
    val readOnly: Boolean?

    /** oslc:representation */
    val representation: Resource<Url>?

    /** oslc:valueType — zero-or-many URIs (literal/resource types) */
    val valueType: List<Resource<Url>>?

    /** oslc:valueShape — zero-or-one ResourceShape reference for resource value-types */
    val valueShape: Resource<ResourceShape>?
}

interface AllowedValues {
    /** oslc:allowedValue — one-or-many values (same value-type as property) */
    val allowedValue: List<Any>
}

interface Comment {
    /** dcterms:identifier — exactly-one */
    val identifier: String

    /** dcterms:creator — exactly-one Resource or LocalResource */
    val creator: AnyResource<Any>

    /** dcterms:title — zero-or-one */
    val title: String?

    /** dcterms:created — exactly-one */
    val created: Instant

    /** dcterms:description — exactly-one */
    val description: String

    /** oslc:partOfDiscussion — exactly-one reference to Discussion */
    val partOfDiscussion: Resource<Discussion>

    /** oslc:inReplyTo — zero-or-one reference to another Comment */
    val inReplyTo: Resource<Comment>?
}

interface Discussion {
    /** oslc:discussionAbout — exactly-one reference to associated resource (any type) */
    val discussionAbout: Resource<Any>

    /** oslc:comment — zero-or-many Comment resources (reference or inline) */
    val comment: List<AnyResource<Comment>>?
}