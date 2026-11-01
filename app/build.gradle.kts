plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.plugin.compose")
}

// Push (README.md, "Push"): with the google-services.json of a Firebase project in app/, the demo gets
// a device token and Personyze's pushes. Without it, it builds and runs, and checks for its
// notifications as an app without push does.
if (file("google-services.json").exists()) {
	apply(plugin = "com.google.gms.google-services")
}

android {
	namespace = "com.personyze.androidsdkdemo"
	compileSdk = 36

	defaultConfig {
		applicationId = "com.personyze.androidsdkdemo"
		minSdk = 24
		targetSdk = 36
		versionCode = 1
		versionName = "2.1.0"
	}

	buildTypes {
		release {
			// Minified, to prove the SDK's consumer rules: R8 must keep what the WebView bridge and
			// WorkManager reach by reflection.
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
			signingConfig = signingConfigs.getByName("debug")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		compose = true
	}
}

dependencies {
	implementation("com.personyze:personyze-android:2.1.0")
	implementation("com.personyze:personyze-android-compose:2.1.0")
	implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
	implementation("com.google.firebase:firebase-messaging")
	implementation(platform("androidx.compose:compose-bom:2025.12.00"))
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.activity:activity-compose:1.12.4")
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
}
