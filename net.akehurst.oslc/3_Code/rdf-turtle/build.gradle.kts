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
                api(libs.nal.agl.processor)
                api(project(":rdf"))
            }
        }
    }
}