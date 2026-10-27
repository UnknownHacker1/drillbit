plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.ktlint)
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(
            rootProject.extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")
                .findVersion("ktlint")
                .get()
                .requiredVersion,
        )
        filter {
            exclude { it.file.path.contains("${File.separator}build${File.separator}") }
        }
    }
}
