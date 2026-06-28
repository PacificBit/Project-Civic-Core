import groovy.json.JsonSlurper
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.SourceFolder
import java.util.jar.JarFile

plugins {
    `java-library`
    eclipse
    id("com.diffplug.eclipse.apt") version "4.4.1"
}

repositories {
    mavenCentral()
}

val generatedAnnotationProcessorSourcesDir =
    layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main")

tasks.withType<JavaCompile>().configureEach {
    options.generatedSourceOutputDirectory.set(generatedAnnotationProcessorSourcesDir)
}

tasks.register("prepareGeneratedSourcesDir") {
    group = "hytale"
    description = "Create the annotation-processor generated source directory used by JDTLS/Eclipse."

    doLast {
        generatedAnnotationProcessorSourcesDir.get().asFile.mkdirs()
    }
}

eclipse {
    classpath {
        file {
            whenMerged {
                val classpath = this as Classpath
                val generatedPath =
                    "build/generated/sources/annotationProcessor/java/main"

                if (classpath.entries.filterIsInstance<SourceFolder>().none { it.path == generatedPath }) {
                    classpath.entries.add(SourceFolder(generatedPath, null))
                }
            }
        }
    }
}

tasks.named("eclipseClasspath") {
    dependsOn("prepareGeneratedSourcesDir")
}

val hytaleHome: Provider<String> =
    providers.gradleProperty("hytale_home").orElse(
        providers.provider {
            val flatpak =
                "${System.getProperty("user.home")}/.var/app/com.hypixel.HytaleLauncher/data/Hytale"

            val xdg =
                System.getenv("XDG_DATA_HOME")?.let { "$it/Hytale" }
                    ?: "${System.getProperty("user.home")}/.local/share/Hytale"

            if (file(flatpak).exists()) flatpak else xdg
        }
    )

val hytalePatchline: Provider<String> =
    providers.gradleProperty("hytale_patchline")
        .orElse(System.getenv("HYTALE_CHANNEL") ?: "pre-release")

val hytaleGameDir: Provider<String> =
    providers.provider {
        val requested =
            "${hytaleHome.get()}/install/${hytalePatchline.get()}/package/game/latest"

        val fallback =
            "${hytaleHome.get()}/install/release/package/game/latest"

        if (file("$requested/Server/HytaleServer.jar").exists()) requested else fallback
    }

val hytaleServerJar: Provider<String> =
    hytaleGameDir.map { "$it/Server/HytaleServer.jar" }

val hytaleAssetsZip: Provider<String> =
    hytaleGameDir.map { "$it/Assets.zip" }

val hytaleMainClass: Provider<String> =
    hytaleServerJar.map { jarPath ->
        JarFile(file(jarPath)).use { jar ->
            jar.manifest?.mainAttributes?.getValue("Main-Class")
                ?: "com.hypixel.hytale.Main"
        }
    }

/*
 * Dev workflow:
 *
 * - Hytale server jar: libs/HytaleServer.jar at compile time
 * - CodecButler annotations/processor: libs/CodecButler.jar when present
 * - generated codecs: build/generated/sources/annotationProcessor/java/main
 * - live source assets: src/main/resources
 * - Hytale --assets: Assets.zip, then src/main/resources
 */
val hytaleResourcesDir =
    layout.projectDirectory.dir("src/main/resources")

val hytaleProjectServerJar =
    layout.projectDirectory.file("libs/HytaleServer.jar")

val codecButlerJar =
    layout.projectDirectory.file("libs/CodecButler.jar")

val bundledPluginDependencies by configurations.creating

dependencies {
    compileOnly(files(hytaleProjectServerJar))

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    if (codecButlerJar.asFile.exists()) {
        implementation(files(codecButlerJar))
        annotationProcessor(files(codecButlerJar))
        bundledPluginDependencies(files(codecButlerJar))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val manifestFile =
    layout.projectDirectory.file("src/main/resources/manifest.json")

val manifestProvider =
    providers.fileContents(manifestFile).asText.map { text ->
        @Suppress("UNCHECKED_CAST")
        JsonSlurper().parseText(text) as Map<String, Any?>
    }

fun manifestString(key: String, default: String): Provider<String> =
    manifestProvider.map { manifest ->
        (manifest[key] as? String)?.takeIf { it.isNotBlank() } ?: default
    }

val modName =
    manifestString("Name", project.name)

val modVersion =
    manifestString("Version", "0.0.0")

val serverVersion =
    manifestString("ServerVersion", "unknown")

// The manifest carries a semver *range* (e.g. "^0.5.5") because Hytale parses
// ServerVersion as a range. Only the jar file name uses a stripped tag.
val serverVersionTag =
    serverVersion.map { it.trimStart('=', '^', '~', '<', '>') }

val includeAssets: Provider<Boolean> =
    providers.gradleProperty("includeAssets")
        .map { it.toBoolean() }
        .orElse(true)

base {
    archivesName.set(modName)
}

version = modVersion.get()

tasks.processResources {
    if (!includeAssets.get()) {
        exclude("Common/**")
    }
}

tasks.withType<Jar>().configureEach {
    archiveVersion.set(modVersion)
    archiveClassifier.set(serverVersionTag.map { "server-$it" })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        bundledPluginDependencies.map { dependency ->
            if (dependency.isDirectory) dependency else zipTree(dependency)
        }
    })
}

tasks.register("printArtifactInfo") {
    group = "hytale"

    doLast {
        println("Name           = ${modName.get()}")
        println("Version        = ${modVersion.get()}")
        println("ServerVersion  = ${serverVersion.get()}")
        println("Gradle version = $version")
        println("Patchline      = ${hytalePatchline.get()}")
        println("Hytale home    = ${hytaleHome.get()}")
        println("Game dir       = ${hytaleGameDir.get()}")
        println("Server jar     = ${hytaleServerJar.get()}")
        println("Assets zip     = ${hytaleAssetsZip.get()}")
        println("Main class     = ${hytaleMainClass.get()}")
        println("Include assets = ${includeAssets.get()}")
        println("Dev assets dir = ${hytaleResourcesDir.asFile.absolutePath}")
        println("Generated src  = ${generatedAnnotationProcessorSourcesDir.get().asFile.absolutePath}")
        println("Manifest       = ${manifestFile.asFile.absolutePath}")
        println("CodecButler    = ${codecButlerJar.asFile.absolutePath} exists=${codecButlerJar.asFile.exists()}")
    }
}

tasks.register("syncJdtls") {
    group = "hytale"
    description = "Refresh Eclipse/JDTLS metadata and ensure the generated source directory exists."

    dependsOn("prepareGeneratedSourcesDir", "eclipse")
}

tasks.register<JavaExec>("runHytaleServer") {
    group = "hytale"
    description = "Run a local Hytale development server for this plugin/mod."

    dependsOn("classes")

    standardInput = System.`in`

    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    )

    mainClass.set(hytaleMainClass)

    classpath =
        files(hytaleServerJar) + sourceSets.main.get().runtimeClasspath

    val runDir =
        layout.projectDirectory.dir("run").asFile

    workingDir = runDir

    doFirst {
        runDir.mkdirs()

        val serverJarFile =
            file(hytaleServerJar.get())

        val assetsFile =
            file(hytaleAssetsZip.get())

        require(serverJarFile.exists()) {
            "HytaleServer.jar not found: ${serverJarFile.absolutePath}"
        }

        require(assetsFile.exists()) {
            "Assets.zip not found: ${assetsFile.absolutePath}"
        }

        require(manifestFile.asFile.exists()) {
            "manifest.json not found: ${manifestFile.asFile.absolutePath}"
        }

        require(hytaleResourcesDir.asFile.exists()) {
            "resources dir not found: ${hytaleResourcesDir.asFile.absolutePath}"
        }

        println("Hytale dev server")
        println(" - run dir      = ${runDir.absolutePath}")
        println(" - base assets  = ${assetsFile.absolutePath}")
        println(" - dev assets   = ${hytaleResourcesDir.asFile.absolutePath}")
        println(" - manifest     = ${manifestFile.asFile.absolutePath}")
        println(" - generated    = ${generatedAnnotationProcessorSourcesDir.get().asFile.absolutePath}")
    }

    args(
        "--allow-op",
        "--accept-early-plugins",
        "--disable-sentry",
        "--assets=${hytaleAssetsZip.get()}",
        "--assets=${hytaleResourcesDir.asFile.absolutePath}"
    )
}
