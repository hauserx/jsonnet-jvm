import org.gradle.jvm.tasks.Jar

plugins {
    application
    id("antlr")
    id("com.diffplug.spotless") version "6.25.0"
    alias(libs.plugins.native.image)
}

repositories {
    mavenCentral()
}

graalvmNative {
    binaries {
        named("main") {
            mainClass.set("jsonnetjvm.Main")
            imageName.set("jsonnet-jvm")
            sharedLibrary.set(false)
            buildArgs.add("-H:+UnlockExperimentalVMOptions")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--initialize-at-build-time=jsonnetjvm")
            buildArgs.add("--no-fallback")
        }
    }
}

spotless {
    java {
        eclipse()
        indentWithSpaces(4)
        target("src/*/java/**/*.java")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

val javaVersion = 25

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    implementation(libs.antlr.runtime)
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)

    // Truffle dependencies
    implementation(libs.truffle.api)
    implementation(libs.graal.sdk)
    runtimeOnly(libs.truffle.runtime)
    runtimeOnly(libs.truffle.compiler)
    annotationProcessor(libs.truffle.dsl.processor)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truffle.api)
    testImplementation(libs.graal.sdk)
    testRuntimeOnly(libs.truffle.runtime)
    testRuntimeOnly(libs.truffle.compiler)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("jsonnetjvm.Main")
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
    arguments = arguments + listOf("-visitor", "-package", "jsonnetjvm")
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
