import net.ltgt.gradle.errorprone.errorprone
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    `java`
    id("org.springframework.boot") version "3.4.2"
    alias(libs.plugins.errorprone)
    alias(libs.plugins.test.logger)
}

if (version == "unspecified") {
    version = "0.1.0-SNAPSHOT"
}

group = "dev.blaauwendraad"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.2"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("dev.blaauwendraad:json-masker:1.1.0")

    implementation(libs.jspecify)

    testImplementation(libs.assertj.core)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.engine)

    errorprone(libs.nullaway)
    errorprone(libs.errorprone.core)
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<JavaCompile>().configureEach {
        options.errorprone {
            error(
                "CheckedExceptionNotThrown",
                "FunctionalInterfaceClash",
                "NonFinalStaticField",
                "NullAway",
                "RedundantOverride",
                "RedundantThrows",
                "RemoveUnusedImports",
                "DefaultCharset",
                "UnnecessarilyFullyQualified",
                "UnnecessarilyUsedValue",
                "UnnecessaryBoxedAssignment",
                "UnnecessaryBoxedVariable",
                "UnnecessaryFinal",
                "UnusedException",
                "UnusedLabel",
                "UnusedMethod",
                "UnusedNestedClass",
                "UnusedVariable",
                "WildcardImport",
            )
            if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
                disable("MisleadingEscapedSpace") // good stuff
            }
            disable(
                "StringCaseLocaleUsage",
                "MissingSummary",
            )
            option("NullAway:JSpecifyMode")
            option("NullAway:AnnotatedPackages", "dev.blaauwendraad.masker")
            excludedPaths = ".*/build/generated/.*"
        }
        options.encoding = "UTF-8"
    }
}
