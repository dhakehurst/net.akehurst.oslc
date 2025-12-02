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

import net.akehurst.oslc.api.v2_0.shapes.ServiceProvider
import kotlin.time.Instant

/**
 * The object resource can be identified with either a URI or a blank node.
 */
interface AnyResource<T:Any> {
    fun  fetch(): T
}
interface Resource<T:Any> : AnyResource<T> {

}
interface LocalResource<T:Any> : AnyResource<T> {}

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
    val modifiedBy : Resource<Any>?
}

interface Person {
//TODO:
}

interface ResourceShape {

}

interface Property {

}

interface AllowedValues {

}

interface Comment {

}

interface Discussion {

}