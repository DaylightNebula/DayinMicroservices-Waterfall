plugins {
    kotlin("jvm") version "1.8.20"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "waterfall.microservices"
version = "0.0.1"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven("https://jitpack.io/")
}

// https://github.com/DaylightNebula/DaylinMicroservices/releases/download/0.1/DaylinMicroservices-Core-0.1.jar
dependencies {
    implementation(files("../libs/DaylinMicroservices-Core-0.2.jar"))
    implementation("com.orbitz.consul:consul-client:1.5.3")

    // json
    implementation("org.json:json:20230227")

    // logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.6")

    // waterfall
    implementation("io.github.waterfallmc:waterfall-api:1.19-R0.1-SNAPSHOT")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    destinationDirectory.set(file("../testwaterfall/plugins"))
}