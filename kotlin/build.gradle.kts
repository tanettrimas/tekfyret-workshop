plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    kotlin {
        jvmToolchain(21)
    }
}
