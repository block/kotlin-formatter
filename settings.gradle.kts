rootProject.name = "kotlin-formatter"

pluginManagement {
  includeBuild("build-logic")

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  // Keep this version in sync with version catalog
  id("com.gradle.develocity") version "3.19.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
  id("block.settings")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

include("kotlin-format")
