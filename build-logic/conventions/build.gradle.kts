import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-gradle-plugin")
  alias(libs.plugins.kotlin)
  alias(libs.plugins.dependencyAnalysis)
}

gradlePlugin {
  plugins {
    create("settings") {
      id = "block.settings"
      implementationClass = "xyz.block.gradle.SettingsPlugin"
    }
  }
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(libs.develocityPlugin)
}

val javaTarget = JavaLanguageVersion.of(libs.versions.java.get())

java {
  toolchain {
    languageVersion = javaTarget
  }
}

tasks.withType<JavaCompile> {
  options.release.set(javaTarget.asInt())
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = javaTarget.toString()
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
