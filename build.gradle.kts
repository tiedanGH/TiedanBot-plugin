plugins {
    val kotlinVersion = "2.1.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
}

group = "site.tiedan"
version = "v1.3.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
    compileOnly("top.mrxiaom.mirai:overflow-core-api:1.0.8")
    testConsoleRuntime("top.mrxiaom.mirai:overflow-core:1.0.8")

    api("jakarta.mail:jakarta.mail-api:2.1.5")
    implementation("org.eclipse.angus:angus-mail:2.0.3")
}

mirai {
    jvmTarget = JavaVersion.VERSION_1_8
}
