pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    // JitPack occasionally fails to resolve the Gradle plugin marker artifact for
    // org.jetbrains.kotlin.plugin.serialization. Mapping to the actual Kotlin plugin
    // module makes plugin resolution deterministic across repositories/mirrors.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.plugin.serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
            if (requested.id.id == "com.google.devtools.ksp") {
                // The plugin marker POM may fail to resolve on JitPack; map to the real Gradle plugin JAR.
                useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
    }
}

rootProject.name = "aw-db"

include(":aw-db")
include(":demo")
