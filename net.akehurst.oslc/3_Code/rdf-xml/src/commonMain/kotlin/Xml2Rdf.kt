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

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.oslc.rdf.api.*
import net.akehurst.oslc.rdf.asm.*

class Xml2Rdf_v1_1(
    val issues: IssueHolder
) {
    companion object {
        private const val RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        private const val XML_NS = "http://www.w3.org/XML/1998/namespace"

        fun convert(xml: String, baseUri: String = ""): RdfGraph? {
            val holder = IssueHolder()
            return Xml2Rdf_v1_1(holder).convert(xml, baseUri)
        }
    }

    private val namespaces = mutableMapOf<String, String>()

    fun convert(xml: String, baseUri: String = ""): RdfGraph? {
        if (xml.isBlank()) {
            return RdfGraphDefault("ParsedUnit")
        }

        val id = "ParsedUnit"
        val rdf = RdfGraphDefault(id).apply {
            try {
                val xmlDoc = Ksoup.parseXml(xml)
                val root = xmlDoc.root()

                // Collect namespace declarations
                collectNamespaces(root)

                // Add namespace declarations as triples (matching Turtle parser behavior)
                namespaces.forEach { (prefix, uri) ->
                    triples.add(
                        RdfTripleDefault(
                            this,
                            RdfResourceDefault("@prefix"),
                            RdfPredicateDefault("$prefix:"),
                            RdfResourceDefault(uri)
                        )
                    )
                }

                // Check if root is rdf:RDF
                if (root.tagName() == "rdf:RDF" || root.tagName() == "RDF") {
                    root.traverseRdf(this, baseUri)
                } else if (root.childElementsList().isNotEmpty()) {
                    // Handle case where there's no explicit rdf:RDF wrapper
                    root.childElementsList().forEach { child ->
                        if (child.tagName() == "rdf:RDF" || child.tagName() == "RDF") {
                            child.traverseRdf(this, baseUri)
                        }
                    }
                }
            } catch (e: Exception) {
                issues.error(null,  "Error parsing XML: ${e.message}")
            }
        }
        return rdf
    }

    private fun collectNamespaces(element: Element) {
        element.attributes().forEach { attr ->
            if (attr.key.startsWith("xmlns:")) {
                val prefix = attr.key.substring(6)
                namespaces[prefix] = attr.value
            } else if (attr.key == "xmlns") {
                namespaces[""] = attr.value
            }
        }
        element.childElementsList().forEach { collectNamespaces(it) }
    }

    private fun Element.traverseRdf(graph: RdfGraphDefault, baseUri: String) {
        childElementsList().forEach {
            it.processNodeElement(graph, baseUri)
        }
    }

    private fun Element.processNodeElement(graph: RdfGraphDefault, baseUri: String) {
        val tagName = tagName()

        // Determine the subject
        val subject = when {
            hasAttr("rdf:about") -> {
                val about = attr("rdf:about")
                RdfResourceDefault(resolveUri(about, baseUri))
            }
            hasAttr("rdf:ID") -> {
                val id = attr("rdf:ID")
                RdfResourceDefault(resolveUri("#$id", baseUri))
            }
            else -> RdfBlankNodeDefault()
        }

        // Add rdf:type triple if not rdf:Description
        if (tagName != "rdf:Description" && tagName != "Description") {
            val typePrefixed = toPrefixedForm(tagName)
            if (null != typePrefixed) {
                graph.triples.add(
                    RdfTripleDefault(
                        graph,
                        subject,
                        RdfPredicateDefault("a"),  // Turtle uses "a" for rdf:type
                        RdfResourceDefault(typePrefixed)
                    )
                )
            }
        }

        // Process property elements
        childElementsList().forEach { propertyElement ->
            processPropertyElement(graph, subject, propertyElement, baseUri)
        }
    }

    private fun processPropertyElement(
        graph: RdfGraphDefault,
        subject: RdfSubject,
        propertyElement: Element,
        baseUri: String
    ) {
        // Skip rdf:Statement elements - they're special constructs, not properties
        val tagName = propertyElement.tagName()
        if (tagName == "rdf:Statement" || tagName == "Statement") {
            processReification(graph, subject, propertyElement, baseUri)
            return
        }

        val predicateStr = toPrefixedForm(tagName) ?: return
        val predicate = RdfPredicateDefault(predicateStr)

        // Check for special RDF attributes
        when {
            propertyElement.hasAttr("rdf:resource") -> {
                // Object is a resource reference
                val resourceUri = resolveUri(propertyElement.attr("rdf:resource"), baseUri)
                val obj = RdfResourceDefault(resourceUri)
                graph.triples.add(RdfTripleDefault(graph, subject, predicate, obj))
            }
            propertyElement.hasAttr("rdf:parseType") -> {
                val parseType = propertyElement.attr("rdf:parseType")
                when (parseType) {
                    "Literal" -> {
                        // XML Literal - preserve inner XML as string
                        val xmlContent = propertyElement.html().trim()
                        val obj = RdfLiteralDefault("STRING", xmlContent, "^^${RDF_NS}XMLLiteral")
                        graph.triples.add(RdfTripleDefault(graph, subject, predicate, obj))
                    }
                    "Resource" -> {
                        // Blank node with nested properties
                        val blankNode = RdfBlankNodeDefault()
                        graph.triples.add(RdfTripleDefault(graph, subject, predicate, blankNode))
                        propertyElement.childElementsList().forEach { child ->
                            processPropertyElement(graph, blankNode, child, baseUri)
                        }
                    }
                    "Collection" -> {
                        // RDF Collection
                        issues.warn(null,  "parseType='Collection' not yet fully implemented")
                    }
                }
            }
            propertyElement.hasAttr("rdf:datatype") -> {
                // Typed literal
                val datatypePrefixed = toPrefixedUri(propertyElement.attr("rdf:datatype"))
                val literalValue = propertyElement.text().trim()
                val obj = RdfLiteralDefault("STRING", literalValue, "^^$datatypePrefixed")
                graph.triples.add(RdfTripleDefault(graph, subject, predicate, obj))
            }
            propertyElement.childElementsList().isNotEmpty() -> {
                // Nested node element (blank node or resource)
                val firstChild = propertyElement.childElementsList().first()

                // Check if it's a container (Bag, Seq, Alt)
                val childTag = firstChild.tagName()
                if (childTag == "rdf:Bag" || childTag == "rdf:Seq" || childTag == "rdf:Alt" ||
                    childTag == "Bag" || childTag == "Seq" || childTag == "Alt") {
                    processContainer(graph, subject, predicate, firstChild, baseUri)
                } else {
                    // Regular nested element - check if it has its own identity
                    val nestedSubject: RdfSubject = when {
                        firstChild.hasAttr("rdf:about") -> {
                            RdfResourceDefault(resolveUri(firstChild.attr("rdf:about"), baseUri))
                        }
                        firstChild.hasAttr("rdf:ID") -> {
                            RdfResourceDefault(resolveUri("#${firstChild.attr("rdf:ID")}", baseUri))
                        }
                        else -> RdfBlankNodeDefault()
                    }

                    // Add triple with nested subject as object
                    graph.triples.add(RdfTripleDefault(graph, subject, predicate, nestedSubject as RdfObject))

                    // Process the nested element's properties with nestedSubject as the subject
                    val tagName = firstChild.tagName()
                    // Add rdf:type triple if not rdf:Description
                    if (tagName != "rdf:Description" && tagName != "Description") {
                        val typePrefixed = toPrefixedForm(tagName)
                        if (null != typePrefixed) {
                            graph.triples.add(
                                RdfTripleDefault(
                                    graph,
                                    nestedSubject,
                                    RdfPredicateDefault("a"),
                                    RdfResourceDefault(typePrefixed)
                                )
                            )
                        }
                    }

                    // Process child properties
                    firstChild.childElementsList().forEach { childProp ->
                        processPropertyElement(graph, nestedSubject, childProp, baseUri)
                    }
                }
            }
            else -> {
                // Plain literal
                val literalValue = propertyElement.text().trim()
                val langTag = if (propertyElement.hasAttr("xml:lang")) {
                    "@${propertyElement.attr("xml:lang")}"
                } else null
                val obj = RdfLiteralDefault("STRING", literalValue, langTag)
                graph.triples.add(RdfTripleDefault(graph, subject, predicate, obj))
            }
        }
    }

    private fun processContainer(
        graph: RdfGraphDefault,
        subject: RdfSubject,
        predicate: RdfPredicate,
        containerElement: Element,
        baseUri: String
    ) {
        val containerNode = RdfBlankNodeDefault()
        graph.triples.add(RdfTripleDefault(graph, subject, predicate, containerNode))

        // Add type triple for container
        val containerTypePrefixed = toPrefixedForm(containerElement.tagName())
        if (null != containerTypePrefixed) {
            graph.triples.add(
                RdfTripleDefault(
                    graph,
                    containerNode,
                    RdfPredicateDefault("a"),
                    RdfResourceDefault(containerTypePrefixed)
                )
            )
        }

        // Process rdf:li elements
        containerElement.childElementsList().forEach { liElement ->
            val liPredicate = RdfPredicateDefault("rdf:li")

            when {
                liElement.hasAttr("rdf:resource") -> {
                    val resourceUri = resolveUri(liElement.attr("rdf:resource"), baseUri)
                    graph.triples.add(
                        RdfTripleDefault(graph, containerNode, liPredicate, RdfResourceDefault(resourceUri))
                    )
                }
                else -> {
                    val literalValue = liElement.text().trim()
                    graph.triples.add(
                        RdfTripleDefault(graph, containerNode, liPredicate, RdfLiteralDefault("STRING", literalValue, null))
                    )
                }
            }
        }
    }

    private fun processReification(
        graph: RdfGraphDefault,
        subject: RdfSubject,
        statementElement: Element,
        baseUri: String
    ) {
        // Create the reification structure as a blank node
        val reificationNode = RdfBlankNodeDefault()

        // Add type
        graph.triples.add(
            RdfTripleDefault(
                graph,
                reificationNode,
                RdfPredicateDefault("a"),
                RdfResourceDefault("rdf:Statement")
            )
        )

        // Add rdf:subject, rdf:predicate, rdf:object from the rdf:Statement children
        statementElement.childElementsList().forEach { child ->
            val childTagName = child.tagName()
            val childPredicate = when {
                childTagName == "rdf:subject" || childTagName == "subject" -> RdfPredicateDefault("rdf:subject")
                childTagName == "rdf:predicate" || childTagName == "predicate" -> RdfPredicateDefault("rdf:predicate")
                childTagName == "rdf:object" || childTagName == "object" -> RdfPredicateDefault("rdf:object")
                else -> RdfPredicateDefault(toPrefixedForm(childTagName) ?: return@forEach)
            }

            val obj: RdfObject = when {
                child.hasAttr("rdf:resource") -> {
                    val resourceUri = child.attr("rdf:resource")
                    val resolvedUri = resolveUri(resourceUri, baseUri)
                    // Try to convert full URI to prefixed form if it matches a known namespace
                    val prefixedUri = uriToPrefixedForm(resolvedUri)
                    RdfResourceDefault(prefixedUri ?: resolvedUri)
                }
                child.hasAttr("rdf:parseType") && child.attr("rdf:parseType") == "Literal" -> {
                    val xmlContent = child.text().trim()
                    RdfLiteralDefault("STRING", xmlContent, null)
                }
                else -> {
                    RdfLiteralDefault("STRING", child.text().trim(), null)
                }
            }

            graph.triples.add(RdfTripleDefault(graph, reificationNode, childPredicate, obj))
        }
    }

    private fun toPrefixedForm(tagName: String): String? {
        return when {
            tagName.contains(":") -> {
                val parts = tagName.split(":", limit = 2)
                val prefix = parts[0]
                val localName = parts[1]
                // Verify the prefix is declared
                if (namespaces.containsKey(prefix)) {
                    "$prefix:$localName"
                } else null
            }
            else -> null
        }
    }

    private fun uriToPrefixedForm(uri: String): String? {
        // Try to match the URI against known namespaces and convert to prefixed form
        for ((prefix, nsUri) in namespaces) {
            if (uri.startsWith(nsUri)) {
                val localName = uri.substring(nsUri.length)
                return "$prefix:$localName"
            }
        }
        return null
    }

    private fun expandTagToUri(tagName: String): String? {
        return when {
            tagName.contains(":") -> {
                val parts = tagName.split(":", limit = 2)
                val prefix = parts[0]
                val localName = parts[1]
                val nsUri = namespaces[prefix] ?: return null
                // Don't add # if namespace already ends with # or /
                if (nsUri.endsWith("#") || nsUri.endsWith("/")) {
                    "$nsUri$localName"
                } else {
                    "$nsUri#$localName"
                }
            }
            else -> null
        }
    }

    private fun toPrefixedUri(uri: String): String {
        return if (uri.startsWith("&") && uri.contains(";")) {
            // Entity reference like &xsd;integer -> xsd:integer
            // Format is &prefix;localName (no trailing semicolon)
            val withoutAmpersand = uri.substring(1)
            val semicolonIndex = withoutAmpersand.indexOf(';')
            if (semicolonIndex > 0 && semicolonIndex < withoutAmpersand.length - 1) {
                val prefix = withoutAmpersand.substring(0, semicolonIndex)
                val localName = withoutAmpersand.substring(semicolonIndex + 1)
                if (namespaces.containsKey(prefix)) {
                    "$prefix:$localName"
                } else uri
            } else {
                uri
            }
        } else if (uri.contains(":")) {
            // Already prefixed URI
            val parts = uri.split(":", limit = 2)
            val prefix = parts[0]
            if (namespaces.containsKey(prefix)) {
                uri
            } else uri
        } else {
            uri
        }
    }

    private fun expandUri(uri: String): String {
        return if (uri.startsWith("&") && uri.endsWith(";")) {
            // Entity reference like &xsd;integer
            val entity = uri.substring(1, uri.length - 1)
            val parts = entity.split(";", limit = 2)
            if (parts.size == 2) {
                val prefix = parts[0]
                val localName = parts[1]
                val nsUri = namespaces[prefix]
                if (null != nsUri) {
                    if (nsUri.endsWith("#") || nsUri.endsWith("/")) {
                        "$nsUri$localName"
                    } else {
                        "$nsUri#$localName"
                    }
                } else uri
            } else {
                uri
            }
        } else if (uri.contains(":")) {
            // Prefixed URI
            val parts = uri.split(":", limit = 2)
            val prefix = parts[0]
            val localName = parts[1]
            val nsUri = namespaces[prefix]
            if (null != nsUri) {
                if (nsUri.endsWith("#") || nsUri.endsWith("/")) {
                    "$nsUri$localName"
                } else {
                    "$nsUri#$localName"
                }
            } else uri
        } else {
            uri
        }
    }

    private fun resolveUri(uri: String, baseUri: String): String {
        return when {
            uri.isEmpty() -> baseUri
            uri.startsWith("#") -> {
                if (baseUri.isEmpty()) uri else "$baseUri$uri"
            }
            uri.contains("://") -> uri  // Already absolute
            baseUri.isNotEmpty() -> {
                // Resolve relative URI against base
                if (baseUri.endsWith("/") || uri.startsWith("/")) {
                    "$baseUri$uri"
                } else {
                    "$baseUri/$uri"
                }
            }
            else -> uri
        }
    }
}