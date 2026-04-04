plugins {
    application
    java
}

group = "com.fire"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.fire.javajoysticktester.Main")
}

tasks.test {
    useJUnitPlatform()
}