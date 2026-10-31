plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
    android {
        namespace = "blue.deniz.vantage.app"
        compileSdk = 37
        minSdk = 26
        androidResources { enable = true }
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "vantage"
        compilerOptions { optIn.add("kotlin.js.ExperimentalWasmJsInterop") }
        browser {
            commonWebpackConfig { outputFileName = "vantage.js" }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":storage"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.reorderable)
            implementation(libs.compose.unstyled.drawer)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlite.bundled)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime)
            implementation(libs.maplibre.compose)
            runtimeOnly(libs.maplibre.compose.runtime.opengl)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.sqlite.web)
            implementation(npm("@sqlite.org/sqlite-wasm", "3.54.1-build1"))
            implementation(npm("maplibre-gl", libs.versions.maplibre.gl.get()))
        }
    }
}

compose.resources {
    packageOfResClass = "blue.deniz.vantage.app.resources"
}
