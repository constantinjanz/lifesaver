// AGP 9 ships built-in Kotlin (bundles KGP 2.2.10). We override to 2.3.10 so the
// Kotlin compiler matches the Compose compiler + KSP plugins (all 2.3.10) below.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    }
}

plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
}
