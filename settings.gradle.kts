rootProject.name = "kotlin-formatter"

pluginManagement {
  includeBuild("build-logic")

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
  id("block.settings")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

include("kotlin-format")
