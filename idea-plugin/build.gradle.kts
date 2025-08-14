import kotlinx.coroutines.selects.select
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
  alias(libs.plugins.kotlin)
  id("org.jetbrains.intellij.platform")
}

dependencies {
  implementation(project(":kotlin-format"))
  implementation(libs.kotlinStdLib)
  intellijPlatform {
    intellijIdeaUltimate("2024.3.2")
    pluginVerifier("1.381")
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

val pluginName = "kotlin-formatter"
val sinceIdeVersionForVerification = "243.21565.193" // corresponds to the 2024.3 version
val sinceBuildMajorVersion = "241" // corresponds to 2024.1.x versions
val untilIdeVersion = properties["IIC.release.version"] as String
val untilBuildMajorVersion = untilIdeVersion.substringBefore('.')
val pluginVersion = project.version.toString()

intellijPlatform {
  version = pluginVersion
  buildSearchableOptions = false
  projectName = project.name
  instrumentCode = false // We don't need to scan codebase for jetbrains annotations
  pluginConfiguration {
    id = "xyz.block.kotlin-formatter"
    name = pluginName
    version = pluginVersion
    description = "A lightweight plugin for formatting Kotlin code using ktfmt, with the ability to format on save"
    vendor {
      name = "Block"
      url = "https://block.xyz/"
    }
    ideaVersion {
      sinceBuild = sinceBuildMajorVersion
      untilBuild = "$untilBuildMajorVersion.*"
    }
  }
  pluginVerification {
    ides {
      select {
        types = listOf(
          IntelliJPlatformType.IntellijIdeaCommunity,
          IntelliJPlatformType.IntellijIdeaUltimate
        )
        sinceBuild = sinceIdeVersionForVerification
        untilBuild = untilIdeVersion

      }
    }
  }
}

tasks {
  buildPlugin {
    archiveBaseName = pluginName
  }

  check {
    dependsOn("verifyPlugin")
  }

  patchPluginXml {
    version = System.getenv("IJ_PLUGIN_VERSION") // IJ_PLUGIN_VERSION env var available in CI
  }

  publishPlugin {
    token.set(System.getenv("JETBRAINS_TOKEN")) // JETBRAINS_TOKEN env var available in CI
  }
}
