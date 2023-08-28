val ktor_version: String by project

plugins {
    application
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization").version("1.8.20")
}

application {
    mainClass.set("MainKt")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
