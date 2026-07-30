import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const gradlePath = join(import.meta.dirname, "..", "android", "app", "build.gradle");
let gradle = readFileSync(gradlePath, "utf-8");

if (gradle.includes("SIGNING_CONFIG_RELEASE_MARKER")) {
	console.log("build.gradle already patched for release signing");
	process.exit(0);
}

// Replace Expo-template release buildType debug signing with our release config.
gradle = gradle.replace(
	/release \{\n\s*\/\/ Caution! In production, you need to generate your own keystore file\.\n\s*\/\/ see https:\/\/reactnative\.dev\/docs\/signed-apk-android\.\n\s*signingConfig signingConfigs\.debug/,
	`release {
            // SIGNING_CONFIG_RELEASE_MARKER
            signingConfig signingConfigs.release`,
);

// Inject signingConfigs.release alongside signingConfigs.debug.
gradle = gradle.replace(
	/signingConfigs \{\n\s*debug \{[^}]+\}[^}]*\n\s*\}\n\s*buildTypes \{/,
	`signingConfigs {
        debug {
            storeFile file("debug.keystore")
            storePassword "android"
            keyAlias "androiddebugkey"
            keyPassword "android"
        }
        // SIGNING_CONFIG_RELEASE_MARKER
        release {
            def keystorePropsFile = new File(rootDir, "keystore.properties")
            if (keystorePropsFile.exists()) {
                def props = new Properties()
                keystorePropsFile.withInputStream { props.load(it) }
                storeFile file(props["storeFile"])
                storePassword props["storePassword"]
                keyAlias props["keyAlias"]
                keyPassword props["storePassword"]
            } else {
                storeFile file("cos-release.keystore")
                storePassword "android"
                keyAlias "androiddebugkey"
                keyPassword "android"
            }
        }
    }
    buildTypes {`,
);

writeFileSync(gradlePath, gradle);
console.log("Patched android/app/build.gradle with release signing config");
