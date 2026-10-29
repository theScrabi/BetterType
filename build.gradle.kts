// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Shared setup for the vendored LanguageTool modules (lt/*), mirroring LT's Maven build.
subprojects {
    plugins.withId("java-library") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion = JavaLanguageVersion.of(17)
        }
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
        }
        tasks.withType<Test>().configureEach {
            useJUnit()
            maxHeapSize = "3372m"
            include("**/Test*.class", "**/*Test.class", "**/*Tests.class", "**/*TestCase.class")
            exclude("**/*ConcurrencyTest.class", "**/Abstract*.class")
            // JNA (desktop Hunspell) extracts its native helper; keep that inside the build dir.
            systemProperty("jna.tmpdir", temporaryDir.absolutePath)
        }
    }
}
