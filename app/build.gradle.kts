plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.casanesapps3"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.casanesapps3"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Fix for "task 'testClasses' not found" error
tasks.register("testClasses") {
    dependsOn(tasks.matching { it.name.contains("UnitTest") && it.name.startsWith("compile") })
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    
    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
