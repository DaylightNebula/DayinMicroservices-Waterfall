plugins {
    kotlin("jvm") version "1.8.10"
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.2.4")
    implementation("io.ktor:ktor-server-netty:2.2.4")
}

sourceSets {
    main {
        kotlin {
            srcDirs("src")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}