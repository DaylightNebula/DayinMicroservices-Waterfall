plugins {
    kotlin("jvm") version "1.8.20-RC2"
}

group = "waterfall.microservices"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    // json
    implementation("org.json:json:20230227")

    // ktor server
    implementation("io.ktor:ktor-server-core:2.2.4")
    implementation("io.ktor:ktor-server-netty:2.2.4")

    // ktor client
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation("io.ktor:ktor-client-logging:2.2.4")

    // logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.6")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}



tasks.getByName<Test>("test") {
    useJUnitPlatform()
}