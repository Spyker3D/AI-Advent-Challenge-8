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

dependencies {
    implementation(project(":rag-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
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
