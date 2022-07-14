plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.12.0"
}

group = "com.evolvedghost.mirai.steamhelper"
version = "1.0.10"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        name = "ktor-eap"
    }
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        name = "ktor-eap"
    }
}

dependencies {
    implementation("org.jsoup:jsoup:1.15.2")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.quartz-scheduler:quartz:2.3.2") {
        exclude(group = "org.slf4j")
    }
}