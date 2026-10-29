plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.schabi.bettertype"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.schabi.bettertype"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            // Every LT language module ships this file; languages are registered in code instead.
            excludes += "META-INF/org/languagetool/language-module.properties"
        }
    }
}

// JAXB implementations come in transitively (segment, LT core) but LanguageTool never uses them,
// and their overlapping copies of com.sun.istack/com.sun.xml.bind classes break dexing.
configurations.configureEach {
    exclude(group = "com.sun.xml.bind")
    exclude(group = "org.glassfish.jaxb")
    exclude(group = "com.sun.istack")
    exclude(group = "com.intellij", module = "annotations") // old duplicate of org.jetbrains:annotations
    // Desktop JNA Hunspell binding; Android uses lt:hunspell-android instead.
    exclude(group = "net.java.dev.jna")
    exclude(group = "com.gitlab.dumonts", module = "hunspell")
}

// Guava's Gradle metadata hands Android builds the -android flavor, whose Supplier etc. don't extend the
// java.util.function types LanguageTool is compiled against (-jre), so pin the JRE variant.
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.google.guava:guava")).using(variant(module("com.google.guava:guava:33.5.0-jre")) { // version as pinned by languagetool-parent 6.8
            attributes {
                attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
            }
        })
    }
}

dependencies {
    implementation(project(":lt:lang-en"))
    implementation(project(":lt:lang-de"))
    implementation(project(":lt:lang-fr"))
    implementation(project(":lt:lang-es"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}