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

package net.akehurst.oslc.rdf.asm

import net.akehurst.oslc.rdf.api.*


data class RdfGraphDefault(
    override val identity: String
) : RdfGraph {
    override val triples = mutableListOf<RdfTriple>()
    override val nodes get() = triples.flatMap { listOf(it.subject, it.object_) }.toSet()

    override operator fun plus(other: RdfGraph): RdfGraph = RdfGraphDefault(this.identity).also {
        it.triples.addAll(this.triples)
        it.triples.addAll(other.triples)
    }

    override fun findTripleWithSubject(subject: RdfSubject) = triples.filter { it.subject == subject }
    override fun findTripleWithPredicate(predicate: RdfPredicate) = triples.filter { it.predicate == predicate }
    override fun findTripleWithObject(object_: RdfObject) = triples.filter { it.object_ == object_ }

    override fun findSubject(predicate: RdfPredicate, value: RdfObject): Set<RdfSubject> {
        return triples.filter {
            it.predicate == predicate && it.object_ == value
        }.map { it.subject }.toSet()
    }

    override fun asModel(): RdfModel = RdfModelDefault(this)

    override fun asString(): String {
        val sb = StringBuilder()
        sb.appendLine("Rdf Graph $identity")
        triples.forEach {
            sb.appendLine("  ${it.asString()}")
        }
        return sb.toString()
    }
}

data class RdfTripleDefault(
    override val graph: RdfGraph,
    override val subject: RdfSubject,
    override val predicate: RdfPredicate,
    override val object_: RdfObject
) : RdfTriple {
    override fun asString(): String {
        return "${subject.asString()} ${predicate.asString()} ${object_.asString()} ."
    }
}

data class RdfPredicateDefault(
    override val iri: String
) : RdfPredicate {
    override fun asString(): String {
        return iri
    }
}

data class RdfBlankNodeDefault(
    override val label: String
) : RdfBlankNode {
    companion object {
        var counter = 0
    }

    constructor() : this($$"$blank$${counter++}")

    override fun asString(): String {
        return "_:$label"
    }
}

data class RdfLiteralDefault(
    override val type: String,
    override val value: String,
    override val tag: String?
) : RdfLiteral {

    override fun asString(): String = when (type) {
        "STRING" -> "'$value'${tag ?: ""}"
        else -> "$value${tag ?: ""}"
    }
}

data class RdfResourceDefault(
    override val iri: String
) : RdfResource {
    override fun asString(): String {
        return iri
    }
}

data class RdfCollectionDefault(
    override val elements: List<RdfNode>
) : RdfCollection {
    override fun asString(): String {
        return elements.joinToString(prefix = "( ", separator = " ", postfix = " )") { it.asString() }
    }
}

fun RdfNode.convert(graph: RdfGraph): Any = when (this) {
    is RdfResource -> RdfStructureDefault(graph, this)
    is RdfLiteral -> when (this.type) {
        "INTEGER" -> this.value.toInt()
        "DECIMAL" -> this.value.toDouble()
        "DOUBLE" -> this.value.toDouble()
        "BOOLEAN" -> this.value.toBoolean()
        "STRING" -> this.value
        else -> error("should not happen")
    }

    is RdfBlankNode -> RdfStructureDefault(graph, this)
    is RdfCollection -> this.elements.map { element -> element.convert(graph) }
    else -> error("should not happen")
}

data class RdfModelDefault(
    override val graph: RdfGraph
) : RdfModel {

    override fun findStructureWithIdentity(value: String): RdfStructure? {
        return graph.triples.firstNotNullOfOrNull {
            val subject = it.subject
            when (subject) {
                is RdfResource -> subject.takeIf { it.iri == value }?.let { it.convert(graph) as RdfStructure }
                else -> null
            }
        }
    }

    override fun findStructuresWithPropertyValue(property: String, value: Any): Set<RdfStructure> {
        return graph.triples.filter {
            it.predicate.iri == property && it.object_.convert(graph) == value
        }.map { RdfStructureDefault(graph, it.subject) }.toSet()
    }
}

data class RdfStructureDefault(
    val graph: RdfGraph,
    override val subject: RdfSubject
) : RdfStructure {

    override val identity: String
        get() = when (subject) {
            is RdfResource -> subject.iri
            is RdfBlankNode -> subject.label
            else -> error("should not happen")
        }

    override val propertyValue: Map<String, List<Any>>
        get() {
            val triples = graph.findTripleWithSubject(subject)
            val groups = triples.groupBy { it.predicate.iri }
            return groups.entries.associate { (k, v) ->
                val name = k
                val list = v.map {
                    val obj = it.object_
                    obj.convert(graph)
                }
                name to list
            }
        }

    override fun getPropertyAllAsString(propertyName:String): List<String> = propertyValue[propertyName] as? List<String> ?: emptyList()
    override fun getPropertyFirstAsStringOrNull(propertyName:String): String? = getPropertyAllAsString(propertyName).firstOrNull()
    override fun getPropertyFirstAsString(propertyName:String): String = getPropertyAllAsString(propertyName).first()

    override fun getPropertyAllAsInteger(propertyName:String): List<Int> = propertyValue[propertyName] as? List<Int> ?: emptyList()
    override fun getPropertyFirstAsIntegerOrNull(propertyName:String): Int? = getPropertyAllAsInteger(propertyName).firstOrNull()
    override fun getPropertyFirstAsInteger(propertyName:String): Int = getPropertyAllAsInteger(propertyName).first()

    override fun getPropertyAllAsDouble(propertyName:String): List<Double> = propertyValue[propertyName] as? List<Double> ?: emptyList()
    override fun getPropertyFirstAsDoubleOrNull(propertyName:String): Double? = getPropertyAllAsDouble(propertyName).firstOrNull()
    override fun getPropertyFirstAsDouble(propertyName:String): Double = getPropertyAllAsDouble(propertyName).first()

    override fun getPropertyAllAsBoolean(propertyName:String): List<Boolean> = propertyValue[propertyName] as? List<Boolean> ?: emptyList()
    override fun getPropertyFirstAsBooleanOrNull(propertyName:String): Boolean? = getPropertyAllAsBoolean(propertyName).firstOrNull()
    override fun getPropertyFirstAsBoolean(propertyName:String): Boolean = getPropertyAllAsBoolean(propertyName).first()

    override fun getPropertyAllAsRdfStructure(propertyName:String): List<RdfStructure> = propertyValue[propertyName] as? List<RdfStructure> ?: emptyList()
    override fun getPropertyFirstAsRdfStructureOrNull(propertyName:String): RdfStructure? = getPropertyAllAsRdfStructure(propertyName).firstOrNull()
    override fun getPropertyFirstAsRdfStructure(propertyName:String): RdfStructure = getPropertyAllAsRdfStructure(propertyName).first()

    override fun getPropertyAllAsList(propertyName:String): List<List<Any>> = propertyValue[propertyName] as? List<List<Any>> ?: emptyList()
    override fun getPropertyFirstAsListOrNull(propertyName:String): List<Any>? = getPropertyAllAsList(propertyName).firstOrNull()
    override fun getPropertyFirstAsList(propertyName:String): List<Any> = getPropertyAllAsList(propertyName).first()

    override fun asString(): String {
        val sb = StringBuilder()
        sb.appendLine("RdfStructure ${subject.asString()}")
        propertyValue.forEach { (k, list) ->
            list.forEach { v ->
                val value = when (v) {
                    is String -> "'$v'"
                    else -> v.toString()
                }
                sb.appendLine("  $k == $v")
            }
        }
        return sb.toString()
    }
}