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

package net.akehurst.oslc.rdf.api

interface RdfGraph {
    val identity: String
    val triples: List<RdfTriple>
    val nodes: Set<RdfNode>

    operator fun plus(other: RdfGraph) : RdfGraph

    fun findTripleWithSubject(subject: RdfSubject) : List<RdfTriple>
    fun findTripleWithPredicate(predicate: RdfPredicate) : List<RdfTriple>
    fun findTripleWithObject(object_: RdfObject) : List<RdfTriple>

    fun findSubject(predicate: RdfPredicate, value: RdfObject) : Set<RdfSubject>

    fun asModel():RdfModel
    fun asString(): String
}

interface RdfTriple {
    val graph: RdfGraph
    val subject: RdfSubject
    val predicate: RdfPredicate
    val object_: RdfObject

    fun asString(): String
}

interface RdfNode {
    fun asString(): String
}

interface RdfSubject : RdfNode

interface RdfPredicate {
    val iri: String
    fun asString(): String
}

interface RdfObject : RdfNode {
    val asLiteral : RdfLiteral?
    val asResource : RdfResource?
    val asBlankNode : RdfBlankNode?
    val asCollection : RdfCollection?
}

interface RdfBlankNode : RdfSubject, RdfObject {
    val label: String
}

interface RdfLiteral : RdfObject {
    val type: String
    val value: String
    val tag: String?
}

interface RdfResource : RdfObject, RdfSubject {
    val iri: String
}

interface RdfCollection : RdfObject, RdfSubject {
    val elements: List<RdfNode>
}


interface RdfModel {
    val graph : RdfGraph

    fun findStructureWithIdentity(value:String) : RdfStructure?
    fun findStructuresWithPropertyValue(property:String, value: Any) : Set<RdfStructure>
}

interface RdfStructure {
    val subject: RdfSubject
    val identity: String
    val propertyValue : Map<String, List<Any>>

    fun getPropertyAllAsString(propertyName:String): List<String>
    fun getPropertyFirstAsStringOrNull(propertyName:String): String?
    fun getPropertyFirstAsString(propertyName:String): String

    fun getPropertyAllAsInteger(propertyName:String): List<Int>
    fun getPropertyFirstAsIntegerOrNull(propertyName:String): Int?
    fun getPropertyFirstAsInteger(propertyName:String): Int

    fun getPropertyAllAsDouble(propertyName:String): List<Double>
    fun getPropertyFirstAsDoubleOrNull(propertyName:String): Double?
    fun getPropertyFirstAsDouble(propertyName:String): Double

    fun getPropertyAllAsBoolean(propertyName:String): List<Boolean>
    fun getPropertyFirstAsBooleanOrNull(propertyName:String): Boolean?
    fun getPropertyFirstAsBoolean(propertyName:String): Boolean

    fun getPropertyAllAsRdfStructure(propertyName:String): List<RdfStructure>
    fun getPropertyFirstAsRdfStructureOrNull(propertyName:String): RdfStructure?
    fun getPropertyFirstAsRdfStructure(propertyName:String): RdfStructure

    fun getPropertyAllAsList(propertyName:String): List<Any>
    fun getPropertyFirstAsListOrNull(propertyName:String): List<Any>?
    fun getPropertyFirstAsList(propertyName:String): List<Any>

    fun asString(): String
}