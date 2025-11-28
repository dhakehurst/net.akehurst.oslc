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

package net.akehurst.oslc.rdf.turtle

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.oslc.rdf.asm.*
import net.akehurst.oslc.rdf.api.*

typealias RdfTripleFunc = (RdfGraph) -> List<RdfTriple>

class TurtleSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<RdfGraph>() {

    companion object {
        var nextAnonNodeId = 0
    }

    private val additionalTriples = mutableListOf<RdfTripleFunc>()

    override fun <T : Any> clear(done: Set<SyntaxAnalyser<T>>) {
        super.clear(done)
        additionalTriples.clear()
    }

    override fun registerHandlers() {
        register(this::unit)
        register(this::directiveOrStatement)
        register(this::statement)
        register(this::directive)
        register(this::simpleTriple)
        register(this::predicateList)
        register(this::predicateObject)
        register(this::objectList)
        register(this::subject)
        register(this::predicate)
        registerFor("object", this::object_)
        register(this::collection)
        register(this::blankNodePropertyList)
        register(this::blankNode)
        register(this::iri)
        register(this::prefixedName)
        register(this::literal)
        register(this::literal)
        register(this::turtleLiteral)
        register(this::rdfLiteral)
        register(this::rdfLiteralTag)
    }

    // unit = directiveOrStatement* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfGraph {
        val tripleFunc = children as List<(RdfGraph) -> List<RdfTriple>>
        return RdfGraphDefault("ParsedUnit").apply {
            tripleFunc.forEach { func ->
                triples.addAll(func(this))
            }
            additionalTriples.forEach { func ->
                triples.addAll(func(this))
            }
        }
    }

    // directiveOrStatement = directive | statement '.' ;
    private fun directiveOrStatement(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfTripleFunc {
        return children[0] as (RdfGraph) -> List<RdfTriple>
    }

    // statement = simpleTriple | predicateList | objectList ;
    private fun statement(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfTripleFunc {
        return children[0] as (RdfGraph) -> List<RdfTriple>
    }

    // directive
    //  = '@prefix' ID? ':' iri '.'
    //  | '@base' iri '.'
    //  | 'PREFIX' ID? ':' iri
    //  | 'BASE' iri
    // ;
    private fun directive(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfTripleFunc {
        val (subject, predicate, obj) = when (nodeInfo.alt.option.value) {
            0 -> {
                val id = children[1] as String?
                val p = (id ?: "") + ":"
                val o = children[3] as RdfResource
                Triple(RdfResourceDefault("@prefix"), RdfPredicateDefault(p), o)
            }

            1 -> {
                val iri = children[1] as RdfResource
                Triple(RdfResourceDefault("@base"), RdfPredicateDefault(iri.iri), RdfBlankNodeDefault())
            }

            2 -> {
                val id = children[1] as String?
                val p = (id ?: "") + ":"
                val o = children[3] as RdfResource
                Triple(RdfResourceDefault("@prefix"), RdfPredicateDefault(p), o)
            }

            3 -> {
                val iri = children[1] as RdfResource
                Triple(RdfResourceDefault("@base"), RdfPredicateDefault(iri.iri), RdfBlankNodeDefault())
            }

            else -> error("should not happen")
        }
        return { graph ->
            listOf(RdfTripleDefault(graph, subject, predicate, obj))
        }
    }

    // simpleTriple = subject predicate object ;
    private fun simpleTriple(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfTripleFunc {
        val subject = children[0] as RdfSubject
        val predicate = children[1] as RdfPredicate
        val obj = children[2] as RdfObject
        return { graph ->
            listOf(RdfTripleDefault(graph, subject, predicate, obj))
        }
    }

    // predicateList = subject [predicateObject / ';']+ ;
    private fun predicateList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfTripleFunc {
        val subject = children[0] as RdfSubject
        val list = children[1] as List<Any>
        val slist = list.toSeparatedList<Any,Pair<RdfPredicate, List<RdfObject>>, String>()
        //TODO: maybe keep the contraction, and only expand in the graph when wanted !
        return { graph ->
            slist.items.flatMap { pair ->
                val pred = pair.first
                pair.second.map { obj ->
                    RdfTripleDefault(graph, subject, pred, obj)
                }
            }
        }
    }

    // predicateObject = predicate [object / ',']+ ;
    private fun predicateObject(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<RdfPredicate, List<RdfObject>> {
        val p = children[0] as RdfPredicate
        val list = children[1] as List<Any>
        val ol = list.toSeparatedList<Any,RdfObject, String>()
        return Pair(p, ol.items)
    }

    // objectList = subject predicate [object / ',']+ ;
    private fun objectList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (RdfGraph) -> List<RdfTriple> {
        val subject = children[0] as RdfSubject
        val predicate = children[1] as RdfPredicate
        val list = children[2] as List<RdfObject>
        val ol = list.toSeparatedList<Any,RdfObject, String>()
        //TODO: maybe keep the contraction, and only expand in the graph when wanted !
        return { graph ->
            ol.items.map { obj ->
                RdfTripleDefault(graph, subject, predicate, obj)
            }
        }
    }

    // subject = iri | blankNode | collection | blankNodePropertyList ;
    private fun subject(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfSubject {
        return when (nodeInfo.alt.option.value) {
            0 -> children[0] as RdfSubject
            1 -> children[0] as RdfSubject
            2 -> children[0] as RdfSubject
            3 -> {
                val list = children[0] as List<Pair<RdfPredicate, List<RdfObject>>>
                return RdfBlankNodeDefault($$"$anon" + (nextAnonNodeId++)).also { subject ->
                    additionalTriples.add({ graph ->
                        list.flatMap { pair ->
                            val pred = pair.first
                            pair.second.map { obj ->
                                RdfTripleDefault(graph, subject, pred, obj)
                            }
                        }
                    })
                }
            }

            else -> error("should not happen")
        }
    }

    // predicate = 'a' | iri ;
    private fun predicate(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfPredicate {
        return when(nodeInfo.alt.option.value) {
            0 -> RdfPredicateDefault("a")
            1 -> RdfPredicateDefault((children[0] as RdfResource).iri)
            else -> error("should not happen")
        }
    }

    // object = iri | blankNode | collection | blankNodePropertyList | literal ;
    private fun object_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfObject {
        return when (nodeInfo.alt.option.value) {
            0 -> children[0] as RdfResource
            1 -> children[0] as RdfBlankNode
            2 -> children[0] as RdfCollection
            3 -> {
                val list = children[0] as List<Pair<RdfPredicate, List<RdfObject>>>
                return RdfBlankNodeDefault($$"$anon" + (nextAnonNodeId++)).also { subject ->
                    additionalTriples.add({ graph ->
                        list.flatMap { pair ->
                            val pred = pair.first
                            pair.second.map { obj ->
                                RdfTripleDefault(graph, subject, pred, obj)
                            }
                        }
                    })
                }
            }
            4 -> children[0] as RdfLiteral
            else -> error("should not happen")
        }
    }

    // collection = '(' object* ')' ;
    private fun collection(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfCollection {
        val els = children[1] as List<RdfObject>
        return RdfCollectionDefault(els)
    }

    //blankNodePropertyList = '[' [predicateObject / ';']+ ']' ;
    private fun blankNodePropertyList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Pair<RdfPredicate, List<RdfObject>>> {
        val list = children[1] as List<Any>
        val slist = list.toSeparatedList<Any,Pair<RdfPredicate, List<RdfObject>>, String>()
        return slist.items
    }

    // blankNode = "_:" ID | '[' ']' ;
    private fun blankNode(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfBlankNode {
        val label = when (nodeInfo.alt.option.value) {
            0 -> children[1] as String
            1 -> $$"$anon" + (nextAnonNodeId++)
            else -> error("should not happen")
        }
        return RdfBlankNodeDefault(label)
    }

    // iri = IRIREF | prefixedName ;
    private fun iri(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfResource= when(nodeInfo.alt.option.value) {
        0 -> {
            val v = children[0] as String
            RdfResourceDefault(v.removePrefix("<").removeSuffix(">"))
        }
        1 -> {
            val v = children[0] as String
            RdfResourceDefault(v)
        }
        else -> error("should not happen")
    }

    // prefixedName = ID? ':' ID? ;
    private fun prefixedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        val v1 = (children[0] as? String) ?: ""
        val v2 = (children[2] as? String) ?: ""
        return "$v1:$v2"
    }

    // literal = rdfLiteral | turtleLiteral ;
    private fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfLiteral {
        return children[0] as RdfLiteral
    }

    // turtleLiteral = INTEGER | DECIMAL | DOUBLE | BOOLEAN | STRING ;
    private fun turtleLiteral(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfLiteral {
        return when(nodeInfo.alt.option.value) {
            0 -> RdfLiteralDefault("INTEGER",children[0] as String, null)
            1 -> RdfLiteralDefault("DECIMAL",children[0] as String, null)
            2-> RdfLiteralDefault("DOUBLE",children[0] as String, null)
            3-> RdfLiteralDefault("BOOLEAN",children[0] as String, null)
            4-> RdfLiteralDefault("STRING",(children[0] as String).removeSurrounding("\""), null)
            else -> error("should not happen")
        }
    }

    //rdfLiteral = STRING rdfLiteralTag? ;
    private fun rdfLiteral(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RdfLiteral {
        val v = children[0] as String
        val tag = children[1] as String?
        return RdfLiteralDefault("STRING", v.removeSurrounding("\""), tag)
    }

    //rdfLiteralTag = LANGTAG | '^^' iri ;
    private fun rdfLiteralTag(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return when(nodeInfo.alt.option.value) {
            0 -> children[0] as String
            1 -> "^^"+(children[1] as RdfResource).iri
            else -> error("should not happen")
        }
    }

}