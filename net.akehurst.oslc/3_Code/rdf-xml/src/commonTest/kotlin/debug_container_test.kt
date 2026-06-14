package net.akehurst.oslc.rdf.xml

import kotlin.test.Test
import kotlin.test.assertNotNull

class DebugContainerTest {
    @Test
    fun debug_container() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:ex="http://example.org/ns#">
            
              <rdf:Description rdf:about="http://example.org/project/p1">
                <ex:members>
                  <rdf:Bag>
                    <rdf:li rdf:resource="http://example.org/person/alice"/>
                    <rdf:li rdf:resource="http://example.org/person/bob"/>
                    <rdf:li>Charlie</rdf:li>
                  </rdf:Bag>
                </ex:members>
              </rdf:Description>
            
            </rdf:RDF> 
            """.trimIndent()

        val rdf = Xml2Rdf_v1_1.convert(xml)
        assertNotNull(rdf)
        println("\n=== Container Test ===")
        println("Triples count: ${rdf.triples.size}")
        println(rdf.asString())
    }
}

