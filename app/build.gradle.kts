plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("maven-publish")
}

android {
    namespace = "com.github.teranes10.androidutils"
    compileSdk = 34
    viewBinding.isEnabled = true

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packagingOptions {
        resources.excludes += listOf(
            "META-INF/*.kotlin_module",
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt",
            "META-INF/notice.txt",
            "META-INF/services/**",
            "META-INF/*.properties",
            "META-INF/*.version",
            "/META-INF/{AL2.0,LGPL2.1}",
            "*.json"
        )
    }
}

dependencies {
    // Core Android libraries
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.3")

    // WebRTC
    implementation("com.mesibo.api:webrtc:1.0.5")

    // Room
    implementation("androidx.room:room-runtime:2.5.2")
    annotationProcessor("androidx.room:room-compiler:2.5.2")

    //ftp
    implementation("commons-net:commons-net:3.10.0")

    //SignalR
    implementation("com.microsoft.signalr:signalr:8.0.8")
    implementation("org.slf4j:slf4j-api:2.0.7")

    //Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-places:17.0.0")
    implementation("com.google.maps.android:android-maps-utils:3.4.0")

    //firebase
    implementation("com.google.firebase:firebase-firestore:24.10.0")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.teranes10"
                artifactId = "androidutils"
                version = "1.2.1"
                pom {
                    name.set("AndroidUtils")
                    description.set("A utility library for Android applications")
                    url.set("https://github.com/teranes10/androidutils")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                }
            }
        }
        repositories {
            mavenLocal()
        }
    }
}
