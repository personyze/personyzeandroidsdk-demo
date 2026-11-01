// The same AGP and Kotlin as the SDK's build (personyzeandroidsdk's gradle/libs.versions.toml): AGP 9
// compiles Kotlin itself, with the Kotlin Gradle plugin it depends on (2.2.10).
plugins {
	id("com.android.application") version "9.4.1" apply false
	id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
	id("com.google.gms.google-services") version "4.5.0" apply false
}
