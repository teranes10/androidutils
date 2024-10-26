# AndroidUtils Setup Guide
This guide provides instructions to add the `AndroidUtils` library to your Android project using Gradle.

## 1. Add JitPack Repository
To include the `AndroidUtils` library, add JitPack to your repositories in `settings.gradle.kts`:

```kotlin
// settings.gradle.kts

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") } // add this line
    }
}
```

## 2. Add the Dependency
You have two options for adding `AndroidUtils` to your project.

#### Option 1: Directly in `build.gradle.kts`
Add the library directly to your `build.gradle.kts`:

```kotlin
// build.gradle.kts

dependencies {
    implementation("com.github.teranes10:androidutils:1.0.4")
}
```

#### Option 2: Using Version Catalog (`libs.versions.toml`)
If you’re using a version catalog (`libs.versions.toml`) to manage dependencies:

1. In your `libs.versions.toml` file, add:

    ```toml
    [versions]
    androidutils = "1.0.4"

    [libraries]
    androidutils = { module = "com.github.teranes10:androidutils", version.ref = "androidutils" }
    ```

2. Then, in your `build.gradle.kts`, reference the library:

    ```kotlin
    dependencies {
        implementation(libs.androidutils)
    }
    ```
