plugins {
    kotlin("jvm") version "1.8.20"
}

group = "waterfall.microservices"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("../libs/DaylinMicroservices-Core-0.1.jar"))
    implementation("com.orbitz.consul:consul-client:1.5.3")

    // json
    implementation("org.json:json:20230227")

    // logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.6")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}