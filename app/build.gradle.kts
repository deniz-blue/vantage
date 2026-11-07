plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

val generateBuildInfo = tasks.register<GenerateBuildInfo>("generateBuildInfo") {
    versionName.set(providers.gradleProperty("vantage.versionName"))
    versionCode.set(providers.gradleProperty("vantage.versionCode").map { it.toInt() })
    commit.set(providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }.standardOutput.asText.map { it.trim() })
    outputDir.set(layout.buildDirectory.dir("generated/buildInfo/commonMain/kotlin"))
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
        commonMain {
            kotlin.srcDir(generateBuildInfo.flatMap { it.outputDir })
        }
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

abstract class GenerateBuildInfo : DefaultTask() {
    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val commit: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val source = outputDir.file("blue/deniz/vantage/app/BuildInfo.kt").get().asFile
        source.parentFile.mkdirs()
        source.writeText(
            """
            package blue.deniz.vantage.app

            internal object BuildInfo {
                const val versionName = "${versionName.get()}"
                const val versionCode = ${versionCode.get()}
                const val commit = "${commit.get()}"
            }
            """.trimIndent() + "\n",
        )
    }
}
