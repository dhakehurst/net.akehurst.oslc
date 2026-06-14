package net.akehurst.oslc.rdf.xml

import net.akehurst.oslc.rdf.turtle.TurtleLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DebugTest {
    @Test
    fun debug_simple() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:ex="http://example.org/ns#">
              <rdf:Description rdf:about="http://example.org/book/123">
                <ex:title>The Hitchhiker's Guide to the Galaxy</ex:title>
              </rdf:Description>
            </rdf:RDF>            
            """.trimIndent()

        val rdf = Xml2Rdf_v1_1.convert(xml)
        assertNotNull(rdf)
        println("\n=== Actual Output ===")
        println(rdf.asString())
        println("=== Triples Count: ${rdf.triples.size} ===")
        rdf.triples.forEach {
            println("  S: ${it.subject.asString()}")
            println("  P: ${it.predicate.asString()}")
            println("  O: ${it.object_.asString()}")
            println()
        }

        val expectedTurtle = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix ex: <http://example.org/ns#> .
            
            <http://example.org/book/123>
                ex:title "The Hitchhiker's Guide to the Galaxy" .
            """.trimIndent()

        val expected = TurtleLanguage.processor.process(expectedTurtle).asm!!
        println("=== Expected Output ===")
        println(expected.asString())
        println("=== Triples Count: ${expected.triples.size} ===")
        expected.triples.forEach {
            println("  S: ${it.subject.asString()}")
            println("  P: ${it.predicate.asString()}")
            println("  O: ${it.object_.asString()}")
            println()
        }

        assertEquals(expected.asString(), rdf.asString())
    }

    @Test
    fun debug_blank_nodes() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:dc="http://purl.org/dc/elements/1.1/"
                     xmlns:ex="http://example.org/ns#">
            
              <rdf:Description rdf:about="http://example.org/document/x001">
                <dc:title>A Complex RDF Document</dc:title>
                <dc:creator>
                  <rdf:Description>
                    <ex:firstName>Cyril</ex:firstName>
                    <ex:lastName>Tester</ex:lastName>
                  </rdf:Description>
                </dc:creator>
              </rdf:Description>
            
            </rdf:RDF>      
            """.trimIndent()

        val rdf = Xml2Rdf_v1_1.convert(xml)
        assertNotNull(rdf)
        println("\n=== Actual (blank_nodes) ===")
        println(rdf.asString())

        val expectedTurtle = """
        @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix dc: <http://purl.org/dc/elements/1.1/> .
        @prefix ex: <http://example.org/ns#> .
        
        <http://example.org/document/x001>
            dc:title "A Complex RDF Document" ;
            dc:creator [
                ex:firstName "Cyril" ;
                ex:lastName "Tester"
            ] .
            """.trimIndent()

        val expected = TurtleLanguage.processor.process(expectedTurtle).asm!!
        println("=== Expected (blank_nodes) ===")
        println(expected.asString())
    }

    @Test
    fun debug_typed_literals() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                     xmlns:ex="http://example.org/ns#">
            
              <rdf:Description rdf:about="http://example.org/product/p456">
                <ex:price rdf:datatype="&xsd;integer">499</ex:price>
              </rdf:Description>
            
            </rdf:RDF>  
            """.trimIndent()

        val rdf = Xml2Rdf_v1_1.convert(xml)
        assertNotNull(rdf)
        println("\n=== Actual (typed_literals) ===")
        println(rdf.asString())

        val expectedTurtle = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            @prefix ex: <http://example.org/ns#> .
            
            <http://example.org/product/p456>
                ex:price "499"^^xsd:integer .
            """.trimIndent()

        val expected = TurtleLanguage.processor.process(expectedTurtle).asm!!
        println("=== Expected (typed_literals) ===")
        println(expected.asString())
    }
}

