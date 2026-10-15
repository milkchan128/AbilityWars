import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    java
    kotlin("jvm") version "2.2.21"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("com.gradleup.shadow") version "9.2.2"
}

group = "kr.daybreak"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("com.github.koca2000:NoteBlockAPI:1.6.1")
    implementation("commons-lang:commons-lang:2.6")
    compileOnly("net.essentialsx:EssentialsX:2.19.0") { isTransitive = false }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)
kotlin.jvmToolchain(21)
paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

sourceSets {
    main {
        java.setSrcDirs(listOf(
            "modules/compatibility/Common/src",
            "modules/Plugin/src",
            "modules/Patch/src",
            "modules/compatibility/v1_13_R1/src/daybreak/abilitywar/utils/base/minecraft/block/flat",
            "modules/compatibility/paper_1_21_11/src"
        ))
        resources.setSrcDirs(listOf("modules/Plugin/resources", "src/main/resources"))
        java.exclude("**/FakePlayer.java")
    }
    create("smoke") {
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += output + compileClasspath
    }
}
kotlin.sourceSets.main {
    kotlin.setSrcDirs(listOf("modules/compatibility/Compatibility/src"))
    kotlin.srcDirs(sourceSets.main.get().java.srcDirs)
    kotlin.exclude("**/FakePlayer.java")
}

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        filter { line ->
            line.replace("\${project.version}", project.version.toString()) +
                if (line.startsWith("name:")) "\napi-version: '1.21.11'" else ""
        }
    }
}
tasks.jar {
    archiveClassifier = "plain"
    manifest.attributes["paperweight-mappings-namespace"] = "mojang"
}
tasks.shadowJar {
    archiveClassifier = ""
    relocate("org.bstats", "daybreak.bstats")
    manifest.attributes["paperweight-mappings-namespace"] = "mojang"
}
tasks.assemble { dependsOn(tasks.shadowJar) }
tasks.test { useJUnitPlatform() }
tasks.register<Jar>("smokeJar") {
    dependsOn(tasks.named("smokeClasses"))
    from(sourceSets["smoke"].output)
    archiveFileName = "AbilityWar-PaperSmoke.jar"
    manifest.attributes["paperweight-mappings-namespace"] = "mojang"
}
