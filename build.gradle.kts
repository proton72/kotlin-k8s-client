import java.util.Base64

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    `maven-publish`
    signing
}

group = "io.github.proton72"
version = project.findProperty("version") as String? ?: "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor client
    implementation("io.ktor:ktor-client-core:3.3.2")
    implementation("io.ktor:ktor-client-cio:3.3.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.2")

    // Kotlinx serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.14")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("io.ktor:ktor-client-mock:3.3.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Kotlin K8s Client")
                description.set("A lightweight Kubernetes client library for Kotlin using Ktor")
                url.set("https://github.com/proton72/kotlin-k8s-client")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("proton72")
                        name.set("Proton72")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/proton72/kotlin-k8s-client.git")
                    developerConnection.set("scm:git:ssh://github.com/proton72/kotlin-k8s-client.git")
                    url.set("https://github.com/proton72/kotlin-k8s-client")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/proton72/kotlin-k8s-client")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.token") as String?
            }
        }

        maven {
            name = "SonatypeCentral"
            url = uri("https://central.sonatype.com/api/v1/publisher")
            credentials {
                username = System.getenv("SONATYPE_USERNAME") ?: project.findProperty("sonatype.username") as String?
                password = System.getenv("SONATYPE_PASSWORD") ?: project.findProperty("sonatype.password") as String?
            }
        }
    }
}

signing {
    // Sign only if signing key is available (e.g., in CI or when explicitly configured)
    val hasSigningKey = project.hasProperty("signing.keyId") ||
                        System.getenv("GPG_PRIVATE_KEY") != null ||
                        System.getenv("GPG_PRIVATE_KEY_BASE64") != null
    isRequired = hasSigningKey

    // Support both plain and base64-encoded GPG keys (for GitHub Secrets)
    val signingKey: String? = System.getenv("GPG_PRIVATE_KEY")
        ?: System.getenv("GPG_PRIVATE_KEY_BASE64")?.let { base64Key ->
            String(Base64.getDecoder().decode(base64Key))
        }
    val signingPassword: String? = System.getenv("GPG_PASSWORD") ?: System.getenv("GPG_PASSPHRASE")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications["maven"])
}
