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
import net.akehurst.oslc.rdf.api.RdfGraph

object TurtleLanguage {

    val grammarString = """
        namespace net.akehurst.language.turtle
        
        grammar Turtle {
        
            skip WHITE_SPACE = "\s+" ;
            skip SINGLE_LINE_COMMENT = "#[^\n\r]*" ;
        
            unit = directiveOrStatement* ;
            directiveOrStatement = directive | statement '.' ;
            statement = simpleTriple | predicateList | objectList ;
            directive 
              = '@prefix' ID? ':' iri '.'
              | '@base' iri '.'
              | 'PREFIX' ID? ':' iri
              | 'BASE' iri
              ;
            simpleTriple = subject predicate object ;
            predicateList = subject [predicateObject / ';']+ ;
            predicateObject = predicate [object / ',']+ ;
            objectList = subject predicate [object / ',']+ ;
            
            subject = iri | blankNode | collection | blankNodePropertyList ;
            predicate = 'a' | iri ;
            object = iri | blankNode | collection | blankNodePropertyList | literal ;

            collection = '(' object* ')' ;
            blankNodePropertyList = '[' [predicateObject / ';']+ ']' ;

            blankNode = "_:" ID | '[' ']' ;
            iri = IRIREF | prefixedName ;
            prefixedName = ID? ':' ID? ;
            literal = rdfLiteral | turtleLiteral ;
            turtleLiteral = INTEGER | DECIMAL | DOUBLE | BOOLEAN | STRING ;
            rdfLiteral = STRING rdfLiteralTag? ;
            rdfLiteralTag = LANGTAG | '^^' iri ;
            
            leaf STRING = STRING_TRIPLE_DOUBLE_QUOTE | STRING_TRIPLE_SINGLE_QUOTE | STRING_SINGLE_QUOTE | STRING_DOUBLE_QUOTE ;
            leaf STRING_DOUBLE_QUOTE =  "\"(\\.|[^\"\\])*\"" ;
            leaf STRING_SINGLE_QUOTE =  "'(\\.|[^'\\])*'" ;
            leaf STRING_TRIPLE_DOUBLE_QUOTE =  "(\"\"\")(\\.|\"|\"\"|[^\"\\])*?(\"\"\")" ;
            leaf STRING_TRIPLE_SINGLE_QUOTE =  "'''(\\.|'|''|[^'\\])*?'''" ;            
            leaf INTEGER = "[+-]?[0-9]+" ;
            leaf DECIMAL = "[+-]?[0-9]*[.][0-9]+" ;
            leaf DOUBLE = "[+-]?([0-9]+[.][0-9]*[eE][+-]?[0-9]+|[.][0-9]+[eE][+-]?[0-9]+|[0-9]+[eE][+-]?[0-9]+)" ;
            leaf BOOLEAN = "true|false" ;

            leaf IRIREF = "<[^<>\"{}|^`\\]+>" ;
            leaf ID = "[a-zA-Z0-9_-]+" ;
            leaf LANGTAG = "@[a-zA-Z]+([-][a-zA-Z0-9]+)*";
        }
    """.trimIndent()


    val processor by lazy {
        Agl.processorFromString<RdfGraph, Any>(
            grammarDefinitionStr =grammarString,
            configuration = Agl.configuration {
                targetGrammarName("Turtle")
                syntaxAnalyserResolver { ProcessResultDefault(TurtleSyntaxAnalyser()) }
            }
        ).let {
            check(it.issues.errors.isEmpty()) { "Errors from grammar ${it.issues.errors.joinToString("\n")}" }
            it.processor!!
        }
    }

    fun process(sentence: String) : RdfGraph {
        val result = TurtleLanguage.processor.process(
            sentence = sentence,
        )
        check(result.allIssues.errors.isEmpty()) { result.allIssues.toString() }
        return result.asm!!
    }
}