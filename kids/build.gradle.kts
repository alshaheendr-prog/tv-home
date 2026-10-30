plugins {
    id("com.android.application")
}

android {
    namespace = "com.family.kids"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.family.kids"
        minSdk = 33
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    // Same fixed key as the launcher, so updates install over the old version.
    signingConfigs {
        create("home") {
            storeFile = file("../app/tvhome.keystore")
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
