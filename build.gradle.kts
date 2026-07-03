import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "org.maude"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2024.2")
        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
        zipSigner()
    }
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.opentest4j:opentest4j:1.3.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

kotlin {
    jvmToolchain(17)
}

val generateMaudeLexer = tasks.register<GenerateLexerTask>("generateMaudeLexer") {
    sourceFile.set(file("src/main/jflex/Maude.flex"))
    targetOutputDir.set(file("build/generated/jflex/org/maude/intellij"))
    purgeOldFiles.set(true)
}

tasks.named("compileKotlin") {
    dependsOn(generateMaudeLexer)
}

sourceSets["main"].java.srcDir("build/generated/jflex")
