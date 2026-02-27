import org.gradle.jvm.tasks.Jar

plugins {
    application
    id("antlr")
    id("com.diffplug.spotless") version "8.2.1"
    alias(libs.plugins.native.image)
}

repositories {
    mavenCentral()
}

val javaVersion = 25

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            mainClass.set("com.databricks.jsonnetjvm.Main")
            imageName.set("jsonnet-jvm")
            sharedLibrary.set(false)
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(javaVersion))
                },
            )
            buildArgs.add("--no-fallback")
            buildArgs.add("-O3")
            buildArgs.add("--static-nolibc")
            buildArgs.add("-R:MaxHeapSize=2G")
            buildArgs.add("--initialize-at-build-time")
            buildArgs.add("-H:+ReportExceptionStackTraces")
        }
        named("test") {
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(javaVersion))
                },
            )
            buildArgs.add("--no-fallback")
            buildArgs.add("-O3")
            buildArgs.add("-R:MaxHeapSize=2G")
            buildArgs.add("--initialize-at-build-time")
            buildArgs.add("-H:+ReportExceptionStackTraces")
        }
    }
}

spotless {
    java {
        removeUnusedImports()
        googleJavaFormat("1.34.1")
        formatAnnotations()
        target("src/*/java/**/*.java")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    implementation(libs.antlr.runtime)
    implementation(libs.jackson.databind)
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)

    // Truffle dependencies
    implementation(libs.truffle.api)
    implementation(libs.graal.sdk)
    runtimeOnly(libs.truffle.runtime)
    runtimeOnly(libs.truffle.compiler)
    annotationProcessor(libs.truffle.dsl.processor)

    implementation(libs.snakeyaml)
    implementation(libs.re2j)
    implementation(libs.xz)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truffle.api)
    testImplementation(libs.graal.sdk)
    testRuntimeOnly(libs.truffle.runtime)
    testRuntimeOnly(libs.truffle.compiler)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.databricks.jsonnetjvm.Main")
    applicationDefaultJvmArgs =
        listOf(
            "-Xmx2g",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCI",
            "-Dpolyglot.engine.WarnInterpreterOnly=true",
            "--enable-native-access=ALL-UNNAMED",
            "--sun-misc-unsafe-memory-access=allow",
        )
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-package", "com.databricks.jsonnetjvm")
    outputDirectory = file("build/generated/source/antlr/main")
}

tasks.named<Jar>("jar") {
    enabled = true // Ensure the main jar is built
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    maxHeapSize = "2g"
    jvmArgs(
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+EnableJVMCI",
        "-Dpolyglot.engine.WarnInterpreterOnly=true",
        "--enable-native-access=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow",
    )
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }
}

// Ensure generated source is visible to Java compiler
sourceSets {
    main {
        java {
            srcDir("build/generated/source/antlr/main")
        }
    }
}
