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

package net.akehurst.oslc.generators

import korlibs.io.file.std.resourcesVfs
import net.akehurst.oslc.rdf.api.RdfResource
import net.akehurst.oslc.rdf.api.RdfStructure
import net.akehurst.oslc.rdf.asm.RdfResourceDefault
import net.akehurst.oslc.rdf.asm.RdfStructureDefault
import net.akehurst.oslc.rdf.turtle.TurtleLanguage

suspend fun main() {

    val core_vocab = resourcesVfs["2021-Aug-26"]["core-vocab.ttl"].readString()
    val core_shapes = resourcesVfs["2021-Aug-26"]["core-shapes.ttl"].readString()
    val vocab_rdf = TurtleLanguage.process(core_vocab)
    val shapes_rdf = TurtleLanguage.process(core_shapes)
    val core_rdf =  vocab_rdf + shapes_rdf
    val core_model = core_rdf.asModel()

   // println(core_rdf.asString())
    val resourceShapes_rdfModel = core_model.findStructuresWithPropertyValue("a", RdfStructureDefault(core_model.graph, RdfResourceDefault("oslc:ResourceShape")))

    val resourceShapes = resourceShapes_rdfModel.map { rdfS ->
        val describes = (rdfS.propertyValue["oslc:describes"]?.first() as? RdfStructure)?.let {
            it.propertyValue["rdfs:label"]?.first() as? String
        } ?: (rdfS.subject as? RdfResource)?.iri ?: "???"
        val title = rdfS.propertyValue["dcterms:title"]?.first() as String
        val properties = rdfS.propertyValue["oslc:property"] as List<RdfStructure>
        OslcResourceShape(describes).also { rs ->
            rs.title = title
            properties.forEach { rdfP ->
                val name = rdfP.propertyValue["oslc:name"]?.first() as String
                val valueType = ((rdfP.propertyValue["oslc:valueType"]?.first() as? RdfStructure)?.subject as? RdfResource)?.iri
                val readOnly = rdfP.propertyValue["oslc:readOnly"]?.first() as? Boolean ?: false
                rs.property[name] = OslcResourceShapeProperty(readOnly,name, valueType ?: "Unknown")
            }
        }
    }

    resourceShapes.forEach {
        println(it.asString())

    }
}

data class OslcResourceShape(
    val describes: String
) {
    var title: String? = null
    val property = mutableMapOf<String, OslcResourceShapeProperty>()

    fun asString(): String {
        val sb = StringBuilder()
        sb.appendLine("OslcResourceShape $describes {")
        title?.let {
            sb.appendLine("  title = $it")
        }
        property.values.forEach {
            val rw = if(it.readOnly) "val" else "var"
            sb.appendLine("  $rw ${it.name}: ${it.valueType}")
        }
        sb.appendLine("}")
        return sb.toString()
    }
}

data class OslcResourceShapeProperty(
    val readOnly:Boolean,
    val name:String,
    val valueType:String
)

object Templates {
    const val INTERFACE =
        $$"""
interface $name {
"""


}
