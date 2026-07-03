plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.register<JavaExec>("searchDemo") {
    group = "rag"
    description = "Runs a cosine-similarity search over output/structure_index.json. Pass -Pquery=\"your text\"."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("SearchDemoKt")
    args = listOf(project.findProperty("query")?.toString() ?: "rememberSaveable")
}

tasks.register<JavaExec>("validateIndex") {
    group = "rag"
    description = "Validates metadata in output/fixed_index.json and output/structure_index.json."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ValidateIndexKt")
}
