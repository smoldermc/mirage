plugins {
    `maven-publish`
    signing
}

pluginManager.withPlugin("java") {
    extensions.configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
    }

    tasks.named<Javadoc>("javadoc") {
        isFailOnError = false
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                pom {
                    name.set("${project.group}:${project.name}")
                    description.set(project.description ?: "${project.group}:${project.name}")
                    packaging = "jar"
                }
            }
        }
    }
}

val smolderUsername = providers.gradleProperty("smolderUsername").orNull
val smolderPassword = providers.gradleProperty("smolderPassword").orNull

val repositoryName: String by project
val snapshotRepository: String by project
val releaseRepository: String by project

publishing {
    repositories {
        maven {
            val snapshot = project.version.toString().endsWith("-SNAPSHOT")

            name = repositoryName
            url = if (snapshot) {
                uri(snapshotRepository)
            } else {
                uri(releaseRepository)
            }

            if (!smolderUsername.isNullOrBlank() && !smolderPassword.isNullOrBlank()) {
                credentials {
                    username = smolderUsername
                    password = smolderPassword
                }
            }
        }
    }
}
