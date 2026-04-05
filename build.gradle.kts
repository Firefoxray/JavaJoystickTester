plugins {
    application
    java
}

group = "com.fire"
version = "0.1 Alpha"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.jinput:jinput:2.0.10")
    runtimeOnly("net.java.jinput:jinput-platform:2.0.10:natives-all")
}

application {
    mainClass.set("com.fire.javajoysticktester.Main")
}

tasks.test {
    useJUnitPlatform()
}
