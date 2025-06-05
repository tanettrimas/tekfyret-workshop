plugins {
    application
}

group = "com.example.rag"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.kotlin.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.jsoup)
    implementation(libs.dotenv)
    implementation(libs.logback.classic)
    implementation(libs.jtokkit)
}

// Configure the application plugin
application {
    mainClass.set("com.example.rag.ChatCliKt")
}

// Custom task to run the IndexWebsite script
tasks.register<JavaExec>("runIndex") {
    group = "application"
    description = "Runs the IndexWebsite.kt script to scrape and index a website."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.example.rag.IndexWebsiteKt")
    standardOutput = System.out 
}

// Custom task to run the ChatCli script (can also use the default 'run' task)
tasks.register<JavaExec>("runChat") {
    group = "application"
    description = "Runs the ChatCli.kt script for interactive chat."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.example.rag.ChatCliKt")
    standardInput = System.`in`
    standardOutput = System.out
}