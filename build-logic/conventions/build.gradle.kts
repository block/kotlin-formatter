import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
val kotlinTarget = JvmTarget.fromTarget(libs.versions.java.get())

java {
  toolchain {
    languageVersion = javaTarget
  }
}

kotlin {
  compilerOptions {
    jvmTarget = kotlinTarget
  }
}

tasks.withType<JavaCompile> {
  options.release.set(javaTarget.asInt())
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
