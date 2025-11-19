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
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("org.jetbrains.intellij.platform.settings") version "2.10.4"
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
include("gradle-plugin")
include("idea-plugin")
