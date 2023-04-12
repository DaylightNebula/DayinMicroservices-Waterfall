plugins {
    `maven-publish`
    kotlin("jvm") version "1.8.20"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "waterfall.microservices"
version = "0.1-alpha"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven("https://jitpack.io/")
}

dependencies {
    implementation("com.github.DaylightNebula.DaylinMicroservices:DaylinMicroservices-Core:0.2")
    implementation("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
}

tasks.getByName<Jar>("jar") {
    destinationDirectory.set(file("../testserver/plugins"))
}

tasks.getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    destinationDirectory.set(file("../templates/hub/plugins"))
}