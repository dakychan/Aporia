import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.20-RC"
    id("fabric-loom") version "1.16.0-alpha.10"
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.lombok") version "2.3.20-RC"
}

version = project.property("mod_version").toString()
group = project.property("maven_group").toString()

base {
    archivesName.set(project.property("archives_base_name").toString())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

val targetJavaVersion = 25

repositories {
    maven("https://jitpack.io")
    maven("https://maven.maxhenkel.de/repository/public")
}

loom {
    accessWidenerPath.set(file("src/main/resources/accesswidener"))
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    include(modImplementation("io.netty:netty-handler-proxy:4.1.82.Final")!!)
    include(modImplementation("io.netty:netty-codec-socks:4.1.82.Final")!!)
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    modCompileOnly("de.maxhenkel.voicechat:voicechat-api:2.6.0")
    implementation("ai.catboost:catboost-common:1.2.10")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.version,
                "minecraft_version" to project.property("minecraft_version"),
                "loader_version" to project.property("loader_version"),
                "kotlin_loader_version" to project.property("kotlin_loader_version")
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

tasks.register<ChaosObfuscator>("chaosObfuscate") {
    group = "build"
    description = "Chaos obfuscation with daily rotating mappings"
    dependsOn("classes")
}

tasks.register<Jar>("obfuscatedJar") {
    group = "build"
    description = "Create obfuscated JAR before remapping"
    dependsOn("chaosObfuscate")
    archiveClassifier.set("obfuscated-dev")
    from(File(project.buildDir, "chaos-obfuscated"))
    from(zipTree(tasks.jar.get().archiveFile)) {
        exclude("**/*.class")
    }
    manifest {
        from(tasks.jar.get().manifest)
    }
}

tasks.named("remapJar") {
    dependsOn("obfuscatedJar")
    doFirst {
        val obfJar = tasks.named<Jar>("obfuscatedJar").get()
        val normalJar = tasks.jar.get().archiveFile.get().asFile
        val obfuscatedJar = obfJar.archiveFile.get().asFile
        if (obfuscatedJar.exists()) {
            normalJar.delete()
            obfuscatedJar.copyTo(normalJar, overwrite = true)
        }
    }
}

tasks.register<Jar>("chaosJar") {
    group = "build"
    description = "Create standalone chaos obfuscated JAR"
    dependsOn("chaosObfuscate")
    archiveClassifier.set("chaos")
    from(File(project.buildDir, "chaos-obfuscated"))
    manifest {
        attributes(
            "Main-Class" to "aporia.su.Initialization"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }
    repositories {
    }
}