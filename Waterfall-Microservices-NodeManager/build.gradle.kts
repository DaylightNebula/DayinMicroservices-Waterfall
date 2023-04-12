plugins {
    kotlin("jvm") version "1.8.20"
}

group = "waterfall.microservices"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    implementation("com.github.DaylightNebula.DaylinMicroservices:DaylinMicroservices-Core:0.2")
}