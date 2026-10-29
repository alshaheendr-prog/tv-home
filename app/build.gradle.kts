plugins {
    id("com.android.application")
}

android {
    namespace = "com.family.tvhome"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.family.tvhome"
        minSdk = 28
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    // A fixed key kept in the repo, so each new build installs over the old one.
    signingConfigs {
        create("home") {
            storeFile = file("tvhome.keystore")
            storePassword = "tvhome-family"
            keyAlias = "tvhome"
            keyPassword = "tvhome-family"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("home")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
