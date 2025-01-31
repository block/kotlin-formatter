import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

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
  id("org.jetbrains.intellij.platform.settings") version "2.1.0"
  id("block.settings")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    intellijPlatform {
      defaultRepositories()
    }
  }
}

include("kotlin-format")
include("idea-plugin")
