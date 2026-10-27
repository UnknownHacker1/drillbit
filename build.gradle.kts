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

// Prints how much production Kotlin is shared: lines in commonMain versus platform source sets.
// Counts non-blank lines that are not pure comments. Run with `./gradlew sharedCodeReport`.
tasks.register("sharedCodeReport") {
    group = "verification"
    description = "Counts shared (commonMain) versus platform-specific Kotlin lines."
    val root = layout.projectDirectory.asFile
    doLast {
        fun codeLines(dir: File): Int =
            dir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .sumOf { file ->
                    var inBlockComment = false
                    file.readLines().count { raw ->
                        val line = raw.trim()
                        val isComment = inBlockComment || line.startsWith("//") || line.startsWith("/*")
                        if (line.startsWith("/*")) inBlockComment = true
                        if (inBlockComment && line.endsWith("*/")) inBlockComment = false
                        line.isNotEmpty() && !isComment
                    }
                }

        fun filesUnder(dir: File): List<File> =
            dir
                .walkTopDown()
                .filter { it.isFile }
                .toList()

        val shared =
            mapOf(
                "core/commonMain" to "core/src/commonMain",
                "composeApp/commonMain" to "composeApp/src/commonMain",
            )
        val platform =
            mapOf(
                "composeApp/androidMain" to "composeApp/src/androidMain",
                "composeApp/desktopMain" to "composeApp/src/desktopMain",
                "composeApp/wasmJsMain" to "composeApp/src/wasmJsMain",
                "androidApp/main" to "androidApp/src/main",
            )
        val counts = (shared + platform).mapValues { (_, path) -> codeLines(root.resolve(path)) }
        val sharedTotal = shared.keys.sumOf { counts.getValue(it) }
        val platformTotal = platform.keys.sumOf { counts.getValue(it) }
        counts.forEach { (name, lines) -> println("%-26s %6d".format(name, lines)) }
        val percent = 100.0 * sharedTotal / (sharedTotal + platformTotal)
        println("Production Kotlin: shared $sharedTotal, platform $platformTotal, shared = ${"%.1f".format(percent)}%")

        // Platform-only files that are not Kotlin: manifest, launcher icon, web page, app build script.
        val platformExtras =
            listOf("androidApp/src/main/res", "composeApp/src/wasmJsMain/resources")
                .flatMap { dir -> filesUnder(root.resolve(dir)) } +
                root.resolve("androidApp/src/main/AndroidManifest.xml") +
                root.resolve("androidApp/build.gradle.kts")
        val extraLines = platformExtras.sumOf { file -> file.readLines().count { it.isNotBlank() } }
        val withExtras = 100.0 * sharedTotal / (sharedTotal + platformTotal + extraLines)
        println(
            "Counting platform XML/HTML/Gradle too ($extraLines lines): shared = ${"%.1f".format(withExtras)}%",
        )
    }
}
