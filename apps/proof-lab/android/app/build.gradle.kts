plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("plugin.compose")
}

android {
	namespace = "com.blahmonster.prooflab"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.blahmonster.prooflab"
		minSdk = 26
		targetSdk = 35
		versionCode = 1
		versionName = "1.0"
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	buildFeatures {
		compose = true
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
		// java.time on API 26 is fine, but desugaring keeps the door open for a lower minSdk.
		isCoreLibraryDesugaringEnabled = true
	}

	kotlin {
		jvmToolchain(17)
	}

	sourceSets["main"].java.srcDirs("src/main/kotlin")
}

dependencies {
	// Substituted by the composite build in settings.gradle.kts.
	implementation("com.blahmonster.prooflab:doughcore:1.0.0")

	implementation(platform("androidx.compose:compose-bom:2024.10.01"))
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.material:material-icons-extended")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	debugImplementation("androidx.compose.ui:ui-tooling")

	implementation("androidx.activity:activity-compose:1.9.3")
	implementation("androidx.navigation:navigation-compose:2.8.4")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
	implementation("androidx.core:core-ktx:1.15.0")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

	coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
}
