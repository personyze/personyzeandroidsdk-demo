pluginManagement {
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
	}
}

/*	Which Personyze SDK the demo builds with:

	- the SDK's source, when github.com/personyze/personyzeandroidsdk is cloned beside this repository
	  (`../personyzeandroidsdk`) - a composite build, the SDK's own code substituted for its coordinates.
	  This is how to try a version not released yet, and how to step into the SDK in the debugger;
	- otherwise, or with -Ppersonyze.maven=central, the published version from Maven Central, as an app gets it. */
val sdkSource = file("../personyzeandroidsdk")
val fromSource = providers.gradleProperty("personyze.maven").orNull != "central" && sdkSource.resolve("settings.gradle.kts").exists()

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
	}
}

rootProject.name = "personyzeandroidsdk-demo"
include(":app")

if (fromSource) {
	includeBuild(sdkSource) {
		dependencySubstitution {
			substitute(module("com.personyze:personyze-android")).using(project(":personyze-android"))
			substitute(module("com.personyze:personyze-android-compose")).using(project(":personyze-android-compose"))
		}
	}
}
