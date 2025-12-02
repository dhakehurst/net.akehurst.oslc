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

package net.akehurst.oslc.by.rdf.v2_0

import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import net.akehurst.oslc.core.oslcClient
import net.akehurst.oslc.rdf.xml.Xml2Rdf_v1_1
import kotlin.test.Test
import kotlin.test.assertEquals

class test_impl_rdf_v2 {

    @Test
    fun ServiceProviderCatalog_construction() {
        val rdfXml = """
<?xml version="1.0" encoding="UTF-8"?>
<oslc_disc:ServiceProviderCatalog
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:dcterms="http://purl.org/dc/terms/"
	xmlns:oslc_disc="http://open-services.net/xmlns/discovery/1.0/"
	xmlns:jp="http://jazz.net/xmlns/prod/jazz/process/1.0/"
	rdf:about="https://doorsng-7.somewhere.com:9443/rm/oslc_rm/catalog">
	<dcterms:title>RMCatalog</dcterms:title>
	<oslc_disc:entry>
		<oslc_disc:ServiceProvider>
			<dcterms:title>JKE_Banking</dcterms:title>
			<oslc_disc:details rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_XnwmACMTEe-5aeHy5FVRZg"/>
			<jp:consumerRegistry rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_XnwmACMTEe-5aeHy5FVRZg/links"/>
			<oslc_disc:services rdf:resource="https://doorsng-7.somewhere.com:9443/rm/oslc_rm/_XnwmACMTEe-5aeHy5FVRZg/services.xml"/>
		</oslc_disc:ServiceProvider>
	</oslc_disc:entry>
	<oslc_disc:entry>
		<oslc_disc:ServiceProvider>
			<dcterms:title>TestProjectArea</dcterms:title>
			<oslc_disc:details rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_eamLwCMTEe-5aeHy5FVRZg"/>
			<jp:consumerRegistry rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_eamLwCMTEe-5aeHy5FVRZg/links"/>
			<oslc_disc:services rdf:resource="https://doorsng-7.somewhere.com:9443/rm/oslc_rm/_eamLwCMTEe-5aeHy5FVRZg/services.xml"/>
		</oslc_disc:ServiceProvider>
	</oslc_disc:entry>
	<oslc_disc:entry>
		<oslc_disc:ServiceProvider>
			<dcterms:title>Test_Project_RM_2</dcterms:title>
			<oslc_disc:details rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_pLFS0CMTEe-5aeHy5FVRZg"/>
			<jp:consumerRegistry rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_pLFS0CMTEe-5aeHy5FVRZg/links"/>
			<oslc_disc:services rdf:resource="https://doorsng-7.somewhere.com:9443/rm/oslc_rm/_pLFS0CMTEe-5aeHy5FVRZg/services.xml"/>
		</oslc_disc:ServiceProvider>
	</oslc_disc:entry>
</oslc_disc:ServiceProviderCatalog>            
        """
        val grph = Xml2Rdf_v1_1.convert(rdfXml)!!
        println(grph.asString())
        val struct = grph.asModel().findStructureWithIdentity("https://doorsng-7.somewhere.com:9443/rm/oslc_rm/catalog")!!
        val sut = ServiceProviderCatalogRdf(struct)

        println(sut.asString())

        assertEquals("RMCatalog", sut.title)
        assertEquals(3, sut.serviceProvider.size)
        assertEquals(listOf("JKE_Banking","TestProjectArea","Test_Project_RM_2"), sut.serviceProvider.map { it.value?.title })
    }

    @Test
    fun ServiceProvider_fetchDetails() = runTest {
        val rdfXml = """
<?xml version="1.0" encoding="UTF-8"?>
<oslc_disc:ServiceProviderCatalog
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:dcterms="http://purl.org/dc/terms/"
	xmlns:oslc_disc="http://open-services.net/xmlns/discovery/1.0/"
	xmlns:jp="http://jazz.net/xmlns/prod/jazz/process/1.0/"
	rdf:about="https://doorsng-7.somewhere.com:9443/rm/oslc_rm/catalog">
	<dcterms:title>RMCatalog</dcterms:title>
	<oslc_disc:entry>
		<oslc_disc:ServiceProvider>
			<dcterms:title>JKE_Banking</dcterms:title>
			<oslc_disc:details rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_XnwmACMTEe-5aeHy5FVRZg"/>
			<jp:consumerRegistry rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_XnwmACMTEe-5aeHy5FVRZg/links"/>
			<oslc_disc:services rdf:resource="https://doorsng-7.somewhere.com:9443/rm/oslc_rm/_XnwmACMTEe-5aeHy5FVRZg/services.xml"/>
		</oslc_disc:ServiceProvider>
	</oslc_disc:entry>
	<oslc_disc:entry>
		<oslc_disc:ServiceProvider>
			<dcterms:title>TestProjectArea</dcterms:title>
			<oslc_disc:details rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_eamLwCMTEe-5aeHy5FVRZg"/>
			<jp:consumerRegistry rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_eamLwCMTEe-5aeHy5FVRZg/links"/>
			<oslc_disc:services rdf:resource="https://doorsng-7.somewhere.com:9443/rm/oslc_rm/_eamLwCMTEe-5aeHy5FVRZg/services.xml"/>
		</oslc_disc:ServiceProvider>
	</oslc_disc:entry>
	<oslc_disc:entry>
		<oslc_disc:ServiceProvider>
			<dcterms:title>Test_Project_RM_2</dcterms:title>
			<oslc_disc:details rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_pLFS0CMTEe-5aeHy5FVRZg"/>
			<jp:consumerRegistry rdf:resource="https://doorsng-7.somewhere.com:9443/rm/process/project-areas/_pLFS0CMTEe-5aeHy5FVRZg/links"/>
			<oslc_disc:services rdf:resource="https://doorsng-7.somewhere.com:9443/rm/oslc_rm/_pLFS0CMTEe-5aeHy5FVRZg/services.xml"/>
		</oslc_disc:ServiceProvider>
	</oslc_disc:entry>
</oslc_disc:ServiceProviderCatalog>            
        """
        val grph = Xml2Rdf_v1_1.convert(rdfXml)!!
        println(grph.asString())
        val struct = grph.asModel().findStructureWithIdentity("https://doorsng-7.somewhere.com:9443/rm/oslc_rm/catalog")!!
        val sut = ServiceProviderCatalogRdf(struct)

        println(sut.asString())

        val sp = sut.serviceProvider.first().value!!

        val client = oslcClient(
            baseUrl = Url("..."),
        )
        val spdetails = sp.details?.fetch(client)!!
        println(spdetails)
        assertEquals("", spdetails.title)
    }

}