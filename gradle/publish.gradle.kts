apply(plugin = "maven-publish")

extensions.configure<org.gradle.api.publish.PublishingExtension> {
    publications {
        register<org.gradle.api.publish.maven.MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }

            groupId = project.group.toString()
            artifactId = "aw-db"
            version = project.version.toString()

            pom {
                name.set("aw-db")
                description.set("Room database utility library for Android")
                url.set("https://github.com/answufeng/aw-db")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("answufeng")
                        name.set("answufeng")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/answufeng/aw-db.git")
                    url.set("https://github.com/answufeng/aw-db")
                }
            }
        }
    }
}
