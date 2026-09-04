plugins {
	kotlin("jvm") version "2.1.0"
	kotlin("plugin.serialization") version "2.1.0"
}

group = "com.blahmonster.prooflab"
version = "1.0.0"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
	testImplementation(kotlin("test"))
}

kotlin {
	jvmToolchain(21)
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		events("passed", "failed", "skipped")
	}
}

/** Regenerates ../fixtures/conformance.json, the golden values the iOS tests assert against. */
tasks.register<JavaExec>("writeFixtures") {
	group = "verification"
	mainClass.set("com.blahmonster.prooflab.core.ConformanceKt")
	classpath = sourceSets["main"].runtimeClasspath
	args("../fixtures/conformance.json")
}
