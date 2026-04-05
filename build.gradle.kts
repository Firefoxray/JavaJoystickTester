import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.Sync
import org.gradle.jvm.application.tasks.CreateStartScripts

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
    runtimeOnly("net.java.jinput:jinput:2.0.10:natives-all")
}

val jinputNativesDir = layout.buildDirectory.dir("jinput-natives")
val jinputLibraryPath = jinputNativesDir.map { it.asFile.absolutePath }

val extractJinputNatives by tasks.registering(Sync::class) {
    group = "build setup"
    description = "Unpacks JInput native libraries from the natives-all runtime artifact."

    val runtimeArtifacts = configurations.runtimeClasspath.get().incoming.artifactView {
        attributes.attribute(
            Attribute.of("artifactType", String::class.java),
            ArtifactTypeDefinition.JAR_TYPE
        )
        lenient(true)
    }.files

    from(runtimeArtifacts.elements.map { files ->
        files
            .filter { it.asFile.name.contains("natives-all") }
            .map { zipTree(it.asFile) }
    }) {
        include("**/*.so", "**/*.dll", "**/*.dylib", "**/*.jnilib")
        eachFile {
            path = name
        }
        includeEmptyDirs = false
    }

    into(jinputNativesDir)
}

application {
    mainClass.set("com.fire.javajoysticktester.Main")
}

tasks.withType<JavaExec>().configureEach {
    dependsOn(extractJinputNatives)
    systemProperty("java.library.path", jinputLibraryPath.get())
}

tasks.withType<CreateStartScripts>().configureEach {
    dependsOn(extractJinputNatives)

    defaultJvmOpts = (defaultJvmOpts ?: emptyList()) +
            "-Djava.library.path=${jinputLibraryPath.get()}"
}

tasks.test {
    useJUnitPlatform()
}
