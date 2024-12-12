rootProject.name = "kotlin-formatter"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  // Keep this version in sync with version catalog
  id("com.gradle.develocity") version "3.17.5"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

include("core")
