pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "AIAssistant"

include(":app")
include(":core:network")
include(":core:ui")
include(":core:domain")
include(":core:data")
include(":feature:chat")
include(":feature:settings")
include(":rag-indexer")
include(":rag-core")
include(":developer-assistant")
include(":day10-micro-first")
project(":day10-micro-first").projectDir = file("tools/day10-micro-first")
