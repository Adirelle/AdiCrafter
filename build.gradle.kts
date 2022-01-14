import com.modrinth.minotaur.TaskModrinthUpload
import com.modrinth.minotaur.request.VersionType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom")

    val kotlinVersion: String by System.getProperties()
    kotlin("jvm").version(kotlinVersion)

    id("com.modrinth.minotaur").version("1.2.1")
}

base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}

val env = System.getenv()

val modVersion: String by project
val releaseType = when {
    "-alpha" in modVersion -> "ALPHA"
    "-beta" in modVersion  -> "BETA"
    else                   -> "RELEASE"
}

val minecraftVersion: String by project

version = "${modVersion}+mc${minecraftVersion}"

val mavenGroup: String by project
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

task<TaskModrinthUpload>("modrinth") {
    group = "upload"
    onlyIf { env.contains("MODRINTH_TOKEN") }
    dependsOn("build")

    val modrinthProjectId: String by project

    projectId = modrinthProjectId
    token = env["MODRINTH_TOKEN"]
    uploadFile = tasks["remapJar"]

    versionNumber = version.toString()
    versionName = modVersion
    versionType = VersionType.valueOf(releaseType)
    addGameVersion(minecraftVersion)

    addLoader("fabric")
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

