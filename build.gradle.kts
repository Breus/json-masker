plugins {
    `maven-publish`
    `java-library`
    signing
    id("me.champeau.jmh") version "0.7.2"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("com.adarshr.test-logger") version "4.0.0"
}

description = "High-performance JSON masker in Java with no runtime dependencies"

group = "dev.blaauwendraad"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withJavadocJar()
    withSourcesJar()
    registerFeature("nullabilityAnnotations") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    "nullabilityAnnotationsImplementation"("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")

    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

publishing {
    repositories {
        maven {
            val url = if (version.toString().endsWith("SNAPSHOT")) {
                "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            } else {
                "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            }
            setUrl(url)
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name = project.name
                description = project.description
                url = "https://github.com/Breus/json-masker"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/license/mit/"
                    }
                }
                developers {
                    developer {
                        id = "breus"
                        name = "Breus Blaauwendraad"
                        email = "b.blaauwendraad@gmail.com"
                    }
                    developer {
                        id = "gavlyukovskiy"
                        name = "Arthur Gavlyukovskiy"
                        email = "agavlyukovskiy@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/Breus/json-masker.git"
                    url = "https://github.com/Breus/json-masker"
                }
                issueManagement {
                    url = "https://github.com/Breus/json-masker/issues"
                }
            }
        }
    }
}

val sonatypeUser = System.getenv("SONATYPE_USER")
val sonatypePassword = System.getenv("SONATYPE_PASSWORD")
val gpgKey = System.getenv("GPG_PRIV_KEY")
val gpgPassphrase = System.getenv("GPG_PASS_PHRASE")

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl = uri("https://s01.oss.sonatype.org/service/local/")
            snapshotRepositoryUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            username = sonatypeUser
            password = sonatypePassword
        }
    }
}

signing {
    isRequired = gpgKey != null
    useInMemoryPgpKeys(gpgKey, gpgPassphrase)
    sign(*publishing.publications.toTypedArray())
}

jmh {
    // run this with ./gradlew jmh -PjmhShort to only run these parameters and 4 iterations
    if (project.hasProperty("jmhShort")) {
        benchmarkParameters = mapOf(
            "jsonSize" to rootProject.objects.listProperty<String>().value(listOf("128kb")),
            "maskedKeyProbability" to rootProject.objects.listProperty<String>().value(listOf("0.01")),
            "obfuscationLength" to rootProject.objects.listProperty<String>().value(listOf("none"))
        )

        iterations = 4
    }
    // if you have async profiler installed, you can uncomment this to get a flamegraphs
    // profilers = ["async:libPath=<path-to-async-profiler>/build/libasyncProfiler.so;output=flamegraph;dir=profile-results"]
}

tasks {
    test {
        useJUnitPlatform()
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    compileTestJava {
        options.encoding = "UTF-8"
    }

    javadoc {
        val options = options as StandardJavadocDocletOptions
        options.addBooleanOption("html5", true)
        options.addStringOption("Xdoclint:all,-missing", "-quiet")
    }
}
