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

package net.akehurst.oslc.rdf.xml

import net.akehurst.oslc.rdf.turtle.TurtleLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_Xml2Rdf_v1_1 {
    private companion object {
        /**
         * Normalizes blank node labels in RDF graph string output.
         * Replaces _:\$blank0, _:\$blank1, etc. with _:b0, _:b1, etc. in order of appearance.
         */
        fun normalizeBlankNodes(graphString: String): String {
            val blankNodePattern = Regex($$"""_:\$blank\d+""")
            val mapping = mutableMapOf<String, String>()
            var counter = 0

            return blankNodePattern.replace(graphString) { matchResult ->
                val original = matchResult.value
                mapping.getOrPut(original) { "_:b${counter++}" }
            }
        }

        fun doTest(xml:String, expectedTurtle:String) {
            val rdf = Xml2Rdf_v1_1.convert(xml)
            assertNotNull(rdf)
            assertTrue(rdf.triples.isNotEmpty())

            val expected = TurtleLanguage.processor.process(expectedTurtle)
                .let {
                    check(it.allIssues.errors.isEmpty()) { it.allIssues.toString() }
                    it.asm!!
                }

            // Normalize blank node labels before comparison
            val normalizedExpected = normalizeBlankNodes(expected.asString())
            val normalizedActual = normalizeBlankNodes(rdf.asString())

            assertEquals(normalizedExpected, normalizedActual)
        }
    }


    @Test
    fun empty() {
        val xml = """
            """.trimIndent()

        val rdf = Xml2Rdf_v1_1.convert(xml)
        assertNotNull(rdf)
        assertTrue(rdf.triples.isEmpty())
    }

    @Test
    fun xml_header_only() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            """.trimIndent()

        val rdf = Xml2Rdf_v1_1.convert(xml)
        assertNotNull(rdf)
        assertTrue(rdf.triples.isEmpty())
    }

    @Test
    fun simple_literal() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:ex="http://example.org/ns#">
              <rdf:Description rdf:about="http://example.org/book/123">
                <ex:title>The Hitchhiker's Guide to the Galaxy</ex:title>
                <ex:author>Douglas Adams</ex:author>
                <ex:year>1979</ex:year>
              </rdf:Description>
            </rdf:RDF>            
            """.trimIndent()

            val expected = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix ex: <http://example.org/ns#> .
            
            <http://example.org/book/123>
                ex:title "The Hitchhiker's Guide to the Galaxy" ;
                ex:author "Douglas Adams" ;
                ex:year "1979" .                
            """.trimIndent()

            doTest(xml, expected)
    }

    @Test
    fun abbreviated_syntax() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:foaf="http://xmlns.com/foaf/0.1/">
            
              <foaf:Person rdf:about="http://example.org/person/alice">
                <foaf:name>Alice</foaf:name>
                <foaf:knows rdf:resource="http://example.org/person/bob"/>
              </foaf:Person>
              
              <foaf:Person rdf:about="http://example.org/person/bob">
                <foaf:name>Bob</foaf:name>
              </foaf:Person>
            
            </rdf:RDF>         
            """.trimIndent()

        val expected = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix foaf: <http://xmlns.com/foaf/0.1/> .
            
            <http://example.org/person/alice>
                a foaf:Person ;
                foaf:name "Alice" ;
                foaf:knows <http://example.org/person/bob> .
            
            <http://example.org/person/bob>
                a foaf:Person ;
                foaf:name "Bob" .    
            """.trimIndent()

        doTest(xml, expected)
    }

    @Test
    fun blank_nodes() {
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

        val expected = """
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

        doTest(xml, expected)
    }

    @Test
    fun typed_literals() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                     xmlns:ex="http://example.org/ns#">
            
              <rdf:Description rdf:about="http://example.org/product/p456">
                <ex:price rdf:datatype="&xsd;integer">499</ex:price>
                
                <ex:description xml:lang="en">A description in English.</ex:description>
                <ex:description xml:lang="fr">Une description en français.</ex:description>
              </rdf:Description>
            
            </rdf:RDF>  
            """.trimIndent()

        val expected = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            @prefix ex: <http://example.org/ns#> .
            
            <http://example.org/product/p456>
                ex:price "499"^^xsd:integer ;
                ex:description "A description in English."@en ;
                ex:description "Une description en français."@fr .                
            """.trimIndent()

        doTest(xml, expected)
    }

    @Test
    fun rdf_container_bag() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:ex="http://example.org/ns#">
            
              <rdf:Description rdf:about="http://example.org/project/p1">
                <ex:members>
                  <rdf:Bag>
                    <rdf:li rdf:resource="http://example.org/person/alice"/>
                    <rdf:li rdf:resource="http://example.org/person/bob"/>
                    <rdf:li>Charlie</rdf:li> </rdf:Bag>
                </ex:members>
              </rdf:Description>
            
            </rdf:RDF> 
            """.trimIndent()

        val expected = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix ex: <http://example.org/ns#> .
            
            <http://example.org/project/p1>
                ex:members [
                    a rdf:Bag ;
                    rdf:li <http://example.org/person/alice> ;
                    rdf:li <http://example.org/person/bob> ;
                    rdf:li "Charlie"
                ] .                
            """.trimIndent()

        doTest(xml, expected)
    }

    @Test
    fun reification_example() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:ex="http://example.org/ns#">
            
              <rdf:Description rdf:about="http://example.org/book/456">
                <ex:title>Novel Title</ex:title>
                
                <rdf:Statement>
                  <rdf:subject rdf:resource="http://example.org/book/456"/>
                  <rdf:predicate rdf:resource="http://example.org/ns#title"/>
                  <rdf:object rdf:parseType="Literal">Novel Title</rdf:object>
                </rdf:Statement>
              </rdf:Description>
            
            </rdf:RDF>
            """.trimIndent()

        val expected = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix ex: <http://example.org/ns#> .
            
            # The original statement
            <http://example.org/book/456>
                ex:title "Novel Title" .
            
            # The reification of the statement (as a new blank node resource)
            [
                a rdf:Statement ;
                rdf:subject <http://example.org/book/456> ;
                rdf:predicate ex:title ;
                rdf:object "Novel Title"
            ] .
            """.trimIndent()

        doTest(xml, expected)
    }

    @Test
    fun xml_literal_content() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:ex="http://example.org/ns#">
            
              <rdf:Description rdf:about="http://example.org/content/c01">
                <ex:htmlContent rdf:parseType="Literal">
                  <![CDATA[
                    <h1>Section Header</h1>
                    <p>This content should be preserved exactly.</p>
                  ]]>
                </ex:htmlContent>
              </rdf:Description>
            
            </rdf:RDF>
            """.trimIndent()

        val expected = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix ex: <http://example.org/ns#> .
            
            <http://example.org/content/c01>
                ex:htmlContent ""${'"'}
                    <h1>Section Header</h1>
                    <p>This content should be preserved exactly.</p>
                  ""${'"'}^^rdf:XMLLiteral .
            """.trimIndent()

        doTest(xml, expected)
    }

    @Test
    fun shorthand_id() {
        val xml = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:ex="http://example.org/ns#">
            
              <rdf:Description rdf:ID="LocalEntity">
                <ex:name>Local Named Resource</ex:name>
                <ex:hasReference rdf:resource="#LocalEntity"/> </rdf:Description>
              
              <rdf:Description rdf:about="#LocalEntity">
                <ex:testProperty>Verified via rdf:ID shorthand</ex:testProperty>
              </rdf:Description>
            
            </rdf:RDF>
            """.trimIndent()

        val expected = """
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix ex: <http://example.org/ns#> .
            
            # <#LocalEntity> is the relative URI for the fragment ID "LocalEntity"
            <#LocalEntity>
                ex:name "Local Named Resource" ;
                ex:hasReference <#LocalEntity> ;
                ex:testProperty "Verified via rdf:ID shorthand" .
            """.trimIndent()

        doTest(xml, expected)
    }

    @Test
    fun cameo_rootservices() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:Description
              rdf:about="https://cameo.server.com:8443/oslc/api/rootservices"
              xmlns:dc="http://purl.org/dc/terms/"
              xmlns:jfs="http://jazz.net/xmlns/prod/jazz/jfs/1.0/"
              xmlns:oslc="http://open-services.net/ns/core#"
              xmlns:oslc_am="http://open-services.net/ns/am#"
              xmlns:oslc_config="http://open-services.net/ns/config#"
              xmlns:oslc_rm="http://open-services.net/xmlns/rm/1.0/"
              xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
              xmlns:rm="http://www.ibm.com/xmlns/rdm/rdf/"
            >
                <jfs:oauthUserAuthorizationUrl rdf:resource="https://cameo.server.com:8443/authentication/oauth/authorize"/>
                <oslc_am:majorVersion>2024x</oslc_am:majorVersion>
                <oslc_am:version>2024x</oslc_am:version>
                <oslc_am:amServiceProviders rdf:resource="https://cameo.server.com:8443/oslc/api/oslc/am/catalog"/>
                <jfs:oauthRequestConsumerKeyUrl rdf:resource="https://cameo.server.com:8443/authentication/api/oauth/register"/>
                <dc:title>Teamwork Cloud's OSLC Provider (Architecture Management)</dc:title>
                <oslc_config:cmServiceProviders rdf:resource="https://cameo.server.com:8443/oslc/api/oslc/am/cm"/>
                <jfs:oauthAccessTokenUrl rdf:resource="https://cameo.server.com:8443/authentication/api/oauth/access_token"/>
                <jfs:oauthRequestTokenUrl rdf:resource="https://cameo.server.com:8443/authentication/api/oauth/request_token"/>
                <jfs:oauthDomain>https://cameo.server.com:8443/oslc/</jfs:oauthDomain>
                <jfs:oauthRealmName>TWC</jfs:oauthRealmName>
                <dc:description>Teamwork Cloud is CATIA No Magic's model repository enabling collaboration, configuration management and model governance capabilities.</dc:description>
              </rdf:Description>
              """.trimIndent()

        val expected = """
            @prefix dc: <http://purl.org/dc/terms/> .
            @prefix jfs: <http://jazz.net/xmlns/prod/jazz/jfs/1.0/> .
            @prefix oslc: <http://open-services.net/ns/core#> .
            @prefix oslc_am: <http://open-services.net/ns/am#> .
            @prefix oslc_config: <http://open-services.net/ns/config#> .
            @prefix oslc_rm: <http://open-services.net/xmlns/rm/1.0/> .
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix rm: <http://www.ibm.com/xmlns/rdm/rdf/> .
            
            <https://cameo.server.com:8443/oslc/api/rootservices>
                jfs:oauthUserAuthorizationUrl <https://cameo.server.com:8443/authentication/oauth/authorize> ;
                oslc_am:majorVersion "2024x" ;
                oslc_am:version "2024x" ;
                oslc_am:amServiceProviders <https://cameo.server.com:8443/oslc/api/oslc/am/catalog> ;
                jfs:oauthRequestConsumerKeyUrl <https://cameo.server.com:8443/authentication/api/oauth/register> ;
                dc:title "Teamwork Cloud's OSLC Provider (Architecture Management)" ;
                oslc_config:cmServiceProviders <https://cameo.server.com:8443/oslc/api/oslc/am/cm> ;
                jfs:oauthAccessTokenUrl <https://cameo.server.com:8443/authentication/api/oauth/access_token> ;
                jfs:oauthRequestTokenUrl <https://cameo.server.com:8443/authentication/api/oauth/request_token> ;
                jfs:oauthDomain "https://cameo.server.com:8443/oslc/" ;
                jfs:oauthRealmName "TWC" ;
                dc:description "Teamwork Cloud is CATIA No Magic's model repository enabling collaboration, configuration management and model governance capabilities."
                .
        """.trimIndent()

        doTest(xml, expected)
    }
}