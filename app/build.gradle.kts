plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.schabi.bettertype"
    compileSdk {
        version = release(37)
    }

    ndkVersion = "30.0.16248370" // lets AGP strip debug info from lt:hunspell-android's native library

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
    // Network transport for LT's remote ML servers (never used on device), incl. desktop natives: ~13 MB.
    exclude(group = "io.grpc", module = "grpc-netty-shaded")
    // Lucene backs only LT's n-gram language model (needs GBs of data, not shipped);
    // Google's common protos are only used by gRPC status handling for the remote servers.
    exclude(group = "org.apache.lucene")
    exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
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
    implementation(project(":lt:hunspell-android"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material) // TextInputLayout around the framework EditText the spell checker attaches to
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}