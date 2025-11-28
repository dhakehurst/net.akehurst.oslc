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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.oslc.rdf.api.RdfGraph
import kotlin.test.Test
import kotlin.test.assertTrue

class test_Turtle {

    companion object {

        fun doTestParse(data: TestData) {
            println("--- ${data.name} ---")
            val result = TurtleLanguage.processor.parse(data.sentence)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        }

        fun doTestProcess(data: TestData) {
            println("--- ${data.name} ---")
            val result = TurtleLanguage.processor.process(
                data.sentence
            )
            assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
            println(result.asm!!.asString())
        }

        data class TestData(val name: String, val sentence: String, val expected: Any? = null)

        val testData = listOf(
            TestData(
                name = "empty",
                sentence = """""".trimIndent()
            ),
            TestData(
                name = "example1",
                sentence = """
                    @base <http://example.org/> .
                    @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                    @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                    @prefix rel: <http://www.perceive.net/schemas/relationship/> .
                    
                    <#green-goblin>
                        rel:enemyOf <#spiderman> ;
                        a foaf:Person ;    # in the context of the Marvel universe
                        foaf:name "Green Goblin" .
                    
                    <#spiderman>
                        rel:enemyOf <#green-goblin> ;
                        a foaf:Person ;
                        foaf:name "Spiderman", "Человек-паук@ru" .
                """.trimIndent()
            ),
            TestData(
                name = "example2",
                sentence = """
                    <http://example.org/#spiderman> <http://www.perceive.net/schemas/relationship/enemyOf> <http://example.org/#green-goblin> .
                """.trimIndent()
            ),
            TestData(
                name = "example3",
                sentence = """
                    <http://example.org/#spiderman> <http://www.perceive.net/schemas/relationship/enemyOf> <http://example.org/#green-goblin> ;
				                                    <http://xmlns.com/foaf/0.1/name> "Spiderman" .
                """.trimIndent()
            ),
            TestData(
                name = "example4",
                sentence = """
                    <http://example.org/#spiderman> <http://www.perceive.net/schemas/relationship/enemyOf> <http://example.org/#green-goblin> .
                    <http://example.org/#spiderman> <http://xmlns.com/foaf/0.1/name> "Spiderman" .
                """.trimIndent()
            ),
            TestData(
                name = "example5",
                sentence = """
                    <http://example.org/#spiderman> <http://xmlns.com/foaf/0.1/name> "Spiderman", "Человек-паук"@ru .
                """.trimIndent()
            ),
            TestData(
                name = "example6",
                sentence = """
                    <http://example.org/#spiderman> <http://xmlns.com/foaf/0.1/name> "Spiderman" .
                    <http://example.org/#spiderman> <http://xmlns.com/foaf/0.1/name> "Человек-паук"@ru .
                """.trimIndent()
            ),
            TestData(
                name = "example7",
                sentence = """
                    @prefix somePrefix: <http://www.perceive.net/schemas/relationship/> .
                    <http://example.org/#green-goblin> somePrefix:enemyOf <http://example.org/#spiderman> .
                """.trimIndent()
            ),
            TestData(
                name = "example8",
                sentence = """
                    PREFIX somePrefix: <http://www.perceive.net/schemas/relationship/>
                    <http://example.org/#green-goblin> somePrefix:enemyOf <http://example.org/#spiderman> .
                """.trimIndent()
            ),
            TestData(
                name = "example9",
                sentence = """
                    # A triple with all absolute IRIs
                    <http://one.example/subject1> <http://one.example/predicate1> <http://one.example/object1> .

                    @base <http://one.example/> .
                    <subject2> <predicate2> <object2> .     # relative IRIs, e.g. http://one.example/subject2

                    BASE <http://one.example/>
                    <subject2> <predicate2> <object2> .     # relative IRIs, e.g. http://one.example/subject2

                    @prefix p: <http://two.example/> .
                    p:subject3 p:predicate3 p:object3 .     # prefixed name, e.g. http://two.example/subject3

                    PREFIX p: <http://two.example/>
                    p:subject3 p:predicate3 p:object3 .     # prefixed name, e.g. http://two.example/subject3

                    @prefix p: <path/> .                    # prefix p: now stands for http://one.example/path/
                    p:subject4 p:predicate4 p:object4 .     # prefixed name, e.g. http://one.example/path/subject4

                    @prefix : <http://another.example/> .    # empty prefix
                    :subject5 :predicate5 :object5 .        # prefixed name, e.g. http://another.example/subject5

                    :subject6 a :subject7 .                 # same as :subject6 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :subject7 .

                    <http://伝言.example/?user=أكرم&amp;channel=R%26D> a :subject8 . # a multi-script subject IRI .
                """.trimIndent()
            ),
            TestData(
                name = "example10",
                sentence = """
                    @prefix foaf: <http://xmlns.com/foaf/0.1/> .

                    <http://example.org/#green-goblin> foaf:name "Green Goblin" .

                    <http://example.org/#spiderman> foaf:name "Spiderman" .
                """.trimIndent()
            ),
            TestData(
                name = "example11",
                sentence = """
                    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                    @prefix show: <http://example.org/vocab/show/> .
                    @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

                    show:218 rdfs:label "That Seventies Show"^^xsd:string .            # literal with XML Schema string datatype
                    show:218 rdfs:label "That Seventies Show"^^<http://www.w3.org/2001/XMLSchema#string> . # same as above
                    show:218 rdfs:label "That Seventies Show" .                                            # same again
                    show:218 show:localName "That Seventies Show"@en .                 # literal with a language tag
                    show:218 show:localName 'Cette Série des Années Soixante-dix'@fr . # literal delimited by single quote
                    show:218 show:localName "Cette Série des Années Septante"@fr-be .  # literal with a region subtag
                    show:218 show:blurb '''This is a multi-line                        # literal with embedded new lines and quotes
                    literal with many quotes (""${'"'}${'"'}${'"'})
                    and up to two sequential apostrophes ('').''' .
                """.trimIndent()
            ),
            TestData(
                name = "example12",
                sentence = """
                    @prefix : <http://example.org/elements> .                                                                              
                    <http://en.wikipedia.org/wiki/Helium>                                                                                  
                        :atomicNumber 2 ;               # xsd:integer                                                                      
                        :atomicMass 4.002602 ;          # xsd:decimal                                                                      
                        :specificGravity 1.663E-4 .     # xsd:double   
                """.trimIndent()
            ),
            TestData(
                name = "example13",
                sentence = """
                    @prefix : <http://example.org/stats> .
                    <http://somecountry.example/census2007>
                        :isLandlocked false .           # xsd:boolean
                """.trimIndent()
            ),
            TestData(
                name = "example14",
                sentence = """
                    @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                    _:alice foaf:knows _:bob .
                    _:bob foaf:knows _:alice .
                """.trimIndent()
            ),
            TestData(
                name = "example15",
                sentence = """
                    @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                    # Someone knows someone else, who has the name "Bob".
                    [] foaf:knows [ foaf:name "Bob" ] .
                """.trimIndent()
            ),
            TestData(
                name = "example16",
                sentence = """
                    @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                    [ foaf:name "Alice" ] foaf:knows [
                        foaf:name "Bob" ;
                        foaf:knows [
                            foaf:name "Eve" ] ;
                        foaf:mbox <bob@example.com> ] .
                """.trimIndent()
            ),
            TestData(
                name = "example17",
                sentence = """
                    _:a <http://xmlns.com/foaf/0.1/name> "Alice" .
                    _:a <http://xmlns.com/foaf/0.1/knows> _:b .
                    _:b <http://xmlns.com/foaf/0.1/name> "Bob" .
                    _:b <http://xmlns.com/foaf/0.1/knows> _:c .
                    _:c <http://xmlns.com/foaf/0.1/name> "Eve" .
                    _:b <http://xmlns.com/foaf/0.1/mbox> <bob@example.com> .
                """.trimIndent()
            ),
            TestData(
                name = "example18",
                sentence = """
                    @prefix : <http://example.org/foo> .
                    # the object of this triple is the RDF collection blank node
                    :subject :predicate ( :a :b :c ) .

                    # an empty collection value - rdf:nil
                    :subject :predicate2 () .
                """.trimIndent()
            ),
            TestData(
                name = "example19",
                sentence = """
                    @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                    @prefix dc: <http://purl.org/dc/elements/1.1/> .
                    @prefix ex: <http://example.org/stuff/1.0/> .
                    
                    <http://www.w3.org/TR/rdf-syntax-grammar>
                      dc:title "RDF/XML Syntax Specification (Revised)" ;
                      ex:editor [
                        ex:fullname "Dave Beckett";
                        ex:homePage <http://purl.org/net/dajobe/>
                      ] .
                """.trimIndent()
            ),
            TestData(
                name = "example20",
                sentence = """
                    PREFIX : <http://example.org/stuff/1.0/>
                    :a :b ( "apple" "banana" ) .
                """.trimIndent()
            ),
            TestData(
                name = "example21",
                sentence = """
                    @prefix : <http://example.org/stuff/1.0/> .
                    @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                    :a :b
                      [ rdf:first "apple";
                        rdf:rest [ rdf:first "banana";
                                   rdf:rest rdf:nil ]
                      ] .
                """.trimIndent()
            ),
            TestData(
                name = "example22",
                sentence = """
                    @prefix : <http://example.org/stuff/1.0/> .
                    
                    :a :b "The first line\nThe second line\n  more" .
                    
                    :a :b ""${'"'}The first line
                    The second line
                      more""${'"'} .
                """.trimIndent()
            ),
            TestData(
                name = "example23",
                sentence = """
                    @prefix : <http://example.org/stuff/1.0/> .
                    (1 2.0 3E1) :p "w" .
                """.trimIndent()
            ),
            TestData(
                name = "example24",
                sentence = """
                    @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                        _:b0  rdf:first  1 ;
                              rdf:rest   _:b1 .
                        _:b1  rdf:first  2.0 ;
                              rdf:rest   _:b2 .
                        _:b2  rdf:first  3E1 ;
                              rdf:rest   rdf:nil .
                        _:b0  :p         "w" . 
                """.trimIndent()
            ),
            TestData(
                name = "example25",
                sentence = """
                    PREFIX : <http://example.org/stuff/1.0/>
                    (1 [:p :q] ( 2 ) ) :p2 :q2 .
                """.trimIndent()
            ),
            TestData(
                name = "example26",
                sentence = """
                    _:b0  rdf:first  1 ;
                          rdf:rest   _:b1 .
                    _:b1  rdf:first  _:b2 .
                    _:b2  :p         :q .
                    _:b1  rdf:rest   _:b3 .
                    _:b3  rdf:first  _:b4 .
                    _:b4  rdf:first  2 ;
                          rdf:rest   rdf:nil .
                    _:b3  rdf:rest   rdf:nil .
                """.trimIndent()
            ),
            TestData(
                name = "example27",
                sentence = """
                    @prefix ericFoaf: <http://www.w3.org/People/Eric/ericP-foaf.rdf#> .
                    @prefix : <http://xmlns.com/foaf/0.1/> .
                    ericFoaf:ericP :givenName "Eric" ;
                                  :knows <http://norman.walsh.name/knows/who/dan-brickley> ,
                                          [ :mbox <mailto:timbl@w3.org> ] ,
                                          <http://getopenid.com/amyvdh> .
                """.trimIndent()
            ),
            TestData(
                name = "example28",
                sentence = """
                    @prefix dc: <http://purl.org/dc/terms/> .
                    @prefix frbr: <http://purl.org/vocab/frbr/core#> .
                    
                    <http://books.example.com/works/45U8QJGZSQKDH8N> a frbr:Work ;
                         dc:creator "Wil Wheaton"@en ;
                         dc:title "Just a Geek"@en ;
                         frbr:realization <http://books.example.com/products/9780596007683.BOOK>,
                             <http://books.example.com/products/9780596802189.EBOOK> .
                    
                    <http://books.example.com/products/9780596007683.BOOK> a frbr:Expression ;
                         dc:type <http://books.example.com/product-types/BOOK> .
                    
                    <http://books.example.com/products/9780596802189.EBOOK> a frbr:Expression ;
                         dc:type <http://books.example.com/product-types/EBOOK> .
                """.trimIndent()
            ),
            TestData(
                name = "example29",
                sentence = """
                    @prefix frbr: <http://purl.org/vocab/frbr/core#> .
                    <http://books.example.com/works/45U8QJGZSQKDH8N> a frbr:Work .
                """.trimIndent()
            ),
            TestData(
                name = "other 1",
                sentence = """
                    [
                        a rdf:Statement ;
                        rdf:subject <http://example.org/book/456> ;
                        rdf:predicate ex:title ;
                        rdf:object "Novel Title"
                    ] .
                """.trimIndent()
            ),
        )
    }

    @Test
    fun parse() {
        for (data in testData) {
            doTestParse(data)
        }
    }

    @Test
    fun process() {
        for (data in testData) {
            doTestProcess(data)
        }
    }

}