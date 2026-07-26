buildscript {
    dependencies {
        // Built-in Kotlin support in AGP 9 defaults to KGP 2.2.10; override to the version we target.
        // See: https://developer.android.com/build/migrate-to-built-in-kotlin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}
