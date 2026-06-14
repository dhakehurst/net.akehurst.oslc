package net.akehurst.oslc.rdf.xml

fun main() {
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

    val rdf = Xml2Rdf_v1_1.convert(xml)
    println("=== RDF Graph ===")
    println(rdf?.asString())
}

