plugins {
    id("project-conventions")
}

//TODO: remove this
repositories {
    mavenLocal {
        content {
            includeGroupByRegex("net\\.akehurst.+")
            includeGroupByRegex("com\\.itemis.+")
        }
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":rdf"))
                implementation(project(":rdf-turtle"))
                implementation(project(":rdf-xml"))

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.hmac.sha1)
                implementation(libs.nak.json)
            }
        }
    }
}

