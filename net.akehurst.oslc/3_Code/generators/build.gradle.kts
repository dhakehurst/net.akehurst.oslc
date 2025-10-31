plugins {
    id("project-conventions")
}

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
                implementation(project(":rdf-turtle"))
                implementation(libs.nal.agl.processor)
                implementation(libs.korlibs.korio)
            }
        }
    }
}