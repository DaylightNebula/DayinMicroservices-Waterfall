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
    implementation("com.orbitz.consul:consul-client:1.5.3")

    // json
    implementation("org.json:json:20230227")

    // logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.6")

    // paper
    implementation("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.getByName<Jar>("jar") {
    destinationDirectory.set(file("../testserver/plugins"))
}

tasks.getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    destinationDirectory.set(file("../templates/hub/plugins"))
}

//publishing {
//    publications {
//        create<MavenPublication>("mavenJava") {
//            pom {
//                name.set("Waterfall-Microservices-Node")
//                url.set("https://github.com/DaylightNebula/DayinMicroservices-Waterfall")
//
//                licenses {
//                    license {
//                        name.set("The Apache License, Version 2.0")
//                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                    }
//                }
//                developers {
//                    developer {
//                        id.set("DaylightNebula")
//                        name.set("Noah Shaw")
//                        email.set("noah.w.shaw@gmail.com")
//                    }
//                }
//            }
//        }
//    }
//}