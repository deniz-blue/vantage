import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.play.publisher)
}

android {
    namespace = "blue.deniz.vantage"
    compileSdk = 37

    defaultConfig {
        applicationId = "lt.tsx.vantage"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "0.2.0"
        manifestPlaceholders["appLabel"] = "Vantage"
        providers.gradleProperty("androidAbis").orNull?.let { abis -> ndk { abiFilters += abis.split(",") } }
    }

    signingConfigs {
        providers.gradleProperty("signingProperties").orNull?.let { path ->
            val properties = Properties().apply { File(path).inputStream().use(::load) }
            create("signingProperties") {
                storeFile = File(providers.gradleProperty("signingKeystore").get())
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword") ?: properties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        val signing = signingConfigs.findByName("signingProperties") ?: signingConfigs.getByName("debug")
        debug {
            signingConfig = signing
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signing
        }
        create("dev") {
            initWith(getByName("release"))
            applicationIdSuffix = ".dev"
            manifestPlaceholders["appLabel"] = "Vantage Dev"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    lint {
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

play {
    providers.gradleProperty("playCredentials").orNull?.let { path -> serviceAccountCredentials.set(File(path)) }
    track.set("alpha")
    defaultToAppBundles.set(true)
}

dependencies {
    implementation(project(":app"))
    implementation(libs.androidx.activity.compose)
}
