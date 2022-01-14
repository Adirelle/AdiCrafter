import com.github.gundy.semver4j.model.Version
import com.modrinth.minotaur.TaskModrinthUpload
import com.modrinth.minotaur.request.VersionType
import org.jetbrains.changelog.date
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom")

    val kotlinVersion: String by System.getProperties()
    kotlin("jvm").version(kotlinVersion)

    id("org.jetbrains.changelog") version "1.3.1"
    id("com.modrinth.minotaur") version "1.2.1"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}

val env = System.getenv()

val minecraftVersion: String by project
val mavenGroup: String by project

val isSnapshot = env["GITHUB_REF"] != "tag"
val modVersion =
    if (!isSnapshot) env["GITHUB_REF_NAME"]!!
    else "${project.property("modVersion")}-SNAPSHOT"

val versionInfo = Version.fromString("${modVersion}+mc${minecraftVersion}")
val baseVersion = with(versionInfo) { "${major}.${minor}.${patch}" }

version = versionInfo.toString()
group = mavenGroup

minecraft {}

repositories {
    maven {
        name = "CottonMC"
        url = uri("https://server.bbkr.space/artifactory/libs-release")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    val yarnMappings: String by project
    mappings("net.fabricmc:yarn:$yarnMappings:v2")

    val loaderVersion: String by project
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    val fabricVersion: String by project
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    val fabricKotlinVersion: String by project
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    val libGuiVersion: String by project
    modImplementation("io.github.cottonmc:LibGui:$libGuiVersion")
    include("io.github.cottonmc:LibGui:$libGuiVersion")
}

loom {
    runs {
        getByName("client").apply {
            property("fabric.log.level", "info")
            property("fabric.log.debug.level", "debug")
        }
    }
}

changelog {
    version.set(modVersion)
    header.set(provider { "[${version.get()}] - ${date()}" })
    itemPrefix.set("*")
}

val versionChangelog = changelog.getOrNull(baseVersion) ?: changelog.getLatest()

task<TaskModrinthUpload>("modrinth") {
    group = "upload"
    onlyIf { !isSnapshot && "MODRINTH_TOKEN" in env.keys }
    dependsOn("build")

    val modrinthProjectId: String by project

    projectId = modrinthProjectId
    token = env["MODRINTH_TOKEN"]
    uploadFile = tasks["remapJar"]
    changelog = versionChangelog.toText()

    versionNumber = version.toString()
    versionName = modVersion
    versionType = when {
        "-alpha" in modVersion -> VersionType.ALPHA
        "-beta" in modVersion -> VersionType.BETA
        else -> VersionType.RELEASE
    }

    addGameVersion(minecraftVersion)
    addLoader("fabric")
}

githubRelease {
    token(env["GH_RELEASE_TOKEN"])
    owner("Adirelle")
    repo("AdiCrafter")
    tagName(modVersion)
    body(versionChangelog.toText())
    prerelease(versionInfo.preReleaseIdentifiers.isNotEmpty())
    overwrite(true)
    draft(isSnapshot)
}

tasks {
    val javaVersion = JavaVersion.VERSION_17

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }

    withType<KotlinCompile> {
        kotlinOptions { jvmTarget = javaVersion.toString() }
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }

    jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }

    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
}
