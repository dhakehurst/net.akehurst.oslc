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
                implementation(libs.nal.agl.processor)
                implementation(project(":rdf"))
                implementation(libs.ksoup)
            }
        }

        commonTest {
            dependencies {
                implementation(project(":rdf-turtle"))
            }
        }
    }
}