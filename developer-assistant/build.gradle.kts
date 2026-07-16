plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin { jvmToolchain(17) }
application { mainClass.set("com.aiassistant.developer.MainKt") }

tasks.named<JavaExec>("run") {
    // Make relative --project-root paths resolve from the shared Gradle project,
    // not from the developer-assistant module directory.
    workingDir = rootProject.projectDir
    standardInput = System.`in`
}

dependencies {
    implementation(project(":rag-core"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }
