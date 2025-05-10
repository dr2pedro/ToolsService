plugins {
    kotlin("jvm") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.codeplaydata"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")
    implementation("io.ktor:ktor-client-logging:3.1.2")
    implementation("io.ktor:ktor-client-core:3.1.2")
    implementation("com.aallam.openai:openai-client:4.0.1")
    implementation("io.ktor:ktor-client-apache5:3.1.2")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.4.0")
    implementation("com.anthropic:anthropic-java:0.8.0")
    implementation("org.slf4j:slf4j-nop:2.0.9")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(23)
}