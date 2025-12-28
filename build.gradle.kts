plugins {
    application
    id("antlr")
    id("com.diffplug.spotless") version "6.25.0"
}

repositories {
    mavenCentral()
}

spotless {
    java {
        eclipse()
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

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("jsonnetjvm.Main")
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-package", "jsonnetjvm")
    outputDirectory = file("build/generated/source/antlr/main")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// Ensure generated source is visible to Java compiler
sourceSets {
    main {
        java {
            srcDir("build/generated/source/antlr/main")
        }
    }
}
