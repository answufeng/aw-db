plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.ktlint) apply false
}

// JitPack calls `./gradlew publishToMavenLocal` at the root project.
// In a multi-module setup, forward that to the library module publication.
tasks.register("publishToMavenLocal") {
    dependsOn(":aw-db:publishToMavenLocal")
}

// Let JitPack pass -Pgroup and -Pversion to control publication coordinates.
// Defaults are kept for local/dev usage.
allprojects {
    group = (findProperty("group") as String?)?.takeIf { it.isNotBlank() } ?: "com.github.answufeng"
    version = (findProperty("version") as String?)?.takeIf { it.isNotBlank() } ?: (findProperty("VERSION_NAME") as String?)
    ?: "1.0.0"
}
