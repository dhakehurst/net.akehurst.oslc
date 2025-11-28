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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_RdfGraphDefault {

    @Test
    fun emptyGraph() {
        val g = RdfGraphDefault("empty")
        // graph should start empty
        assertEquals(0, g.triples.size)

        // queries on empty graph should return empty collections
        val s = RdfResourceDefault("http://example.org/x")
        val p = RdfPredicateDefault("http://example.org/p")
        val o = RdfLiteralDefault("STRING", "v", null)
        assertEquals(emptyList<RdfTriple>(), g.findTripleWithSubject(s))
        assertEquals(emptyList<RdfTriple>(), g.findTripleWithPredicate(p))
        assertEquals(emptyList<RdfTriple>(), g.findTripleWithObject(o))
        assertEquals(emptySet<RdfSubject>(), g.findSubject(p, o))

        // asModel should return a model that finds no structures
        val model = g.asModel()
        assertNotNull(model)
        assertEquals(null, model.findStructureWithIdentity(s.iri))
        assertTrue(model.findStructuresWithPropertyValue(p.iri, "v").isEmpty())

        // asString should include the graph identity header
        val asStr = g.asString()
        assertTrue(asStr.contains("Rdf Graph empty"))
    }

    @Test
    fun basicGraphOperations() {
        val g = RdfGraphDefault("g1")
        val s = RdfResourceDefault("http://example.org/s1")
        val p = RdfPredicateDefault("http://example.org/p1")
        val lit = RdfLiteralDefault("STRING", "hello", null)
        val t = RdfTripleDefault(g, s, p, lit)
        g.triples.add(t)

        assertEquals(1, g.triples.size)
        assertEquals(listOf(t), g.findTripleWithSubject(s))
        assertEquals(listOf(t), g.findTripleWithPredicate(p))
        assertEquals(listOf(t), g.findTripleWithObject(lit))

        val subjects = g.findSubject(p, lit)
        assertEquals(setOf<RdfSubject>(s), subjects)

        val nodes = g.nodes
        assertTrue(nodes.contains(s))
        assertTrue(nodes.contains(lit))

        val s2 = RdfResourceDefault("http://example.org/s2")
        val p2 = RdfPredicateDefault("http://example.org/p2")
        val lit2 = RdfLiteralDefault("STRING", "world", null)
        val g2 = RdfGraphDefault("g2")
        g2.triples.add(RdfTripleDefault(g2, s2, p2, lit2))

        val combined = g + g2
        assertEquals(2, combined.triples.size)
        assertEquals("g1", combined.identity)

        val asStr = g.asString()
        assertTrue(asStr.contains("Rdf Graph g1"))
        assertTrue(asStr.contains("hello"))
    }

    @Test
    fun modelAndStructureConversions() {
        val g = RdfGraphDefault("g-model")
        val s = RdfResourceDefault("http://example.org/subject")
        val pStr = RdfPredicateDefault("http://example.org/name")
        val pInt = RdfPredicateDefault("http://example.org/age")

        g.triples.add(RdfTripleDefault(g, s, pStr, RdfLiteralDefault("STRING", "Alice", null)))
        g.triples.add(RdfTripleDefault(g, s, pInt, RdfLiteralDefault("INTEGER", "42", null)))
        g.triples.add(RdfTripleDefault(g, s, RdfPredicateDefault("http://example.org/active"), RdfLiteralDefault("BOOLEAN", "true", null)))

        val model = g.asModel()
        val struct = model.findStructureWithIdentity("http://example.org/subject")
        assertNotNull(struct)

        val byName = model.findStructuresWithPropertyValue(pStr.iri, "Alice")
        assertTrue(byName.isNotEmpty())

        val byAge = model.findStructuresWithPropertyValue(pInt.iri, 42)
        assertTrue(byAge.isNotEmpty())

        val sStruct = struct
        assertEquals(listOf("Alice"), sStruct.getPropertyAllAsString(pStr.iri))
        assertEquals("Alice", sStruct.getPropertyFirstAsString(pStr.iri))
        assertEquals(listOf(42), sStruct.getPropertyAllAsInteger(pInt.iri))
        assertEquals(42, sStruct.getPropertyFirstAsInteger(pInt.iri))

        val active = sStruct.getPropertyFirstAsBooleanOrNull("http://example.org/active")
        assertTrue(active == true)
    }

}