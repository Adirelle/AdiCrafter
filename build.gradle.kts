import com.github.gundy.semver4j.model.Version
import com.modrinth.minotaur.TaskModrinthUpload
import com.modrinth.minotaur.request.VersionType
import org.jetbrains.changelog.date
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom")

    val kotlinVersion: String by System.getProperties()
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

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
val isSnapshot = env["GITHUB_REF_TYPE"] != "tag"
val modVersion =
    if (!isSnapshot) env["GITHUB_REF_NAME"]!!
    else "${project.property("modVersion")}-SNAPSHOT"
val semver: Version = Version.fromString("${modVersion}+mc${minecraftVersion}")
val baseVersion = with(semver) { "${major}.${minor}.${patch}" }
val releaseType = with(semver.preReleaseIdentifiers) {
    when {
        any { it.toString() == "alpha" } -> VersionType.ALPHA
        any { it.toString() == "beta" }  -> VersionType.BETA
        else                             -> VersionType.RELEASE
    }
}
val isPrerelease = !isSnapshot && releaseType != VersionType.RELEASE

version = semver.toString()
println("Version: %s %s%s".format(version, if (isSnapshot) "snapshot" else "", if (isPrerelease) "prerelease" else ""))

val mavenGroup: String by project
group = mavenGroup

changelog {
    // cf. https://github.com/JetBrains/gradle-changelog-plugin
    version.set(modVersion)
    header.set(provider { "[${version.get()}] - ${date()}" })
    itemPrefix.set("*")
}

val versionChangelog by lazy { changelog.getOrNull(baseVersion) ?: changelog.getLatest() }

minecraft {}

repositories {
    maven {
        name = "CottonMC"
        url = uri("https://server.bbkr.space/artifactory/libs-release")
    }
    maven {
        name = "shedaniel"
        url = uri("https://maven.shedaniel.me")
    }

    dependencies {
        minecraft("com.mojang:minecraft:${minecraftVersion}")

        val yarnMappings: String by project
        mappings("net.fabricmc:yarn:$yarnMappings:v2")

        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

        val loaderVersion: String by project
        modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

        val fabricVersion: String by project
        modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

        val fabricKotlinVersion: String by project
        modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

        val libGuiVersion: String by project
        modImplementation("io.github.cottonmc:LibGui:$libGuiVersion")
        include("io.github.cottonmc:LibGui:$libGuiVersion")

        val roughlyEnoughItemsVersion: String by project
        modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:$roughlyEnoughItemsVersion")
        modCompileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin-fabric:$roughlyEnoughItemsVersion")
        modRuntimeOnly("me.shedaniel:RoughlyEnoughItems-fabric:$roughlyEnoughItemsVersion")
    }
}

loom {
    // cf. https://github.com/FabricMC/fabric-loom

    runs {
        getByName("client").apply {
            property("fabric.log.level", "info")
            property("fabric.log.debug.level", "debug")
        }
    }
}

task<TaskModrinthUpload>("modrinth") {
    // cf. https://github.com/modrinth/minotaur

    group = "publishing"
    onlyIf { !isSnapshot && "MODRINTH_TOKEN" in env.keys }
    dependsOn("build")

    val modrinthProjectId: String by project
    projectId = modrinthProjectId

    token = env["MODRINTH_TOKEN"]
    uploadFile = tasks["remapJar"]
    changelog = versionChangelog.toText()

    versionNumber = version.toString()
    versionName = modVersion
    versionType = releaseType

    addGameVersion(minecraftVersion)
    addLoader("fabric")
}

githubRelease {
    // cf. https://github.com/BreadMoirai/github-release-gradle-plugin

    token(env["GITHUB_TOKEN"])
    owner("Adirelle")
    repo("AdiCrafter")
    tagName(modVersion)
    body(versionChangelog.toText())
    prerelease(isPrerelease)
    overwrite(true)
    targetCommitish(env["GITHUB_SHA"] ?: "1.18.x")
    releaseAssets(tasks.jar.get().destinationDirectory.asFile.get().listFiles())
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

    get("githubRelease").apply {
        dependsOn("build")
        onlyIf { !isSnapshot && "GITHUB_TOKEN" in env.keys }
    }

    jar {
        val archivesBaseName: String by project
        from("LICENSE") { rename { "${it}_${archivesBaseName}" } }
        from("LICENSE.md") { rename { "${it}_${archivesBaseName}.md" } }
    }

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
