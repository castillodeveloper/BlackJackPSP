plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") // ¡Sin versión! Ahora la cogerá del padre.
}

group = "org.example.blackjackpsp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":shared"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}