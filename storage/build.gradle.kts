plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

kotlin {
    jvmToolchain(21)
    jvm()
    android {
        namespace = "blue.deniz.vantage.storage"
        compileSdk = 37
        minSdk = 26
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            api(libs.room3.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.mock)
        }
    }
}

dependencies {
    add("kspJvm", libs.room3.compiler)
    add("kspAndroid", libs.room3.compiler)
    add("kspWasmJs", libs.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
