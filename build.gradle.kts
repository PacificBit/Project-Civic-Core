import groovy.json.JsonSlurper

plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val manifestFile = layout.projectDirectory.file("src/main/resources/manifest.json")

val manifestProvider = providers.fileContents(manifestFile).asText.map { text ->
    @Suppress("UNCHECKED_CAST")
    (JsonSlurper().parseText(text) as Map<String, Any?>)
}

fun manifestString(key: String, default: String): Provider<String> =
    manifestProvider.map { (it[key] as? String)?.takeIf { s -> s.isNotBlank() } ?: default }

val modName        = manifestString("Name", project.name)
val modVersion     = manifestString("Version", "0.0.0")
val serverVersion  = manifestString("ServerVersion", "unknown")

base {
    archivesName.set(modName)
}
version = modVersion.get()

tasks.withType<Jar>().configureEach {
    archiveVersion.set(modVersion)
    archiveClassifier.set(serverVersion.map { "server-$it" })
}

tasks.register("printArtifactInfo") {
    doLast {
        println("Name         = ${modName.get()}")
        println("Version      = ${modVersion.get()}")
        println("ServerVersion= ${serverVersion.get()}")
        println("Gradle version = $version")
    }
}
