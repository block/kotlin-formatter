import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
  alias(libs.plugins.kotlin)
  id("org.jetbrains.intellij.platform")
}

dependencies {
  implementation(libs.kotlinStdLib)
  intellijPlatform {
    intellijIdeaUltimate("2024.3.2")
    pluginVerifier("1.381")
  }
}

// TODO
val pluginName = "kotlin-formatter"
val sinceIdeVersion = "233.15619.7" // corresponds to 2023.3.x versions
val sinceBuildMajorVersion = sinceIdeVersion.substringBefore('.')
val untilIdeVersion = properties["IIC.release.version"] as String
val untilBuildMajorVersion = untilIdeVersion.substringBefore('.')

intellijPlatform {
  buildSearchableOptions = false
  projectName = project.name
  instrumentCode = false // We don't need to scan codebase for jetbrains annotations
  pluginConfiguration {
    id = "xyz.block.kotlin-formatter"
    name = pluginName
    version = project.version.toString()
    description = "TODO"
    vendor {
      name = "TODO"
      url = "TODO"
    }
    ideaVersion {
      sinceBuild = sinceBuildMajorVersion
      untilBuild = "$untilBuildMajorVersion.*"
    }
  }
  pluginVerification {
    ides {
      recommended()
      select {
        types = listOf(
          IntelliJPlatformType.IntellijIdeaCommunity,
          IntelliJPlatformType.IntellijIdeaUltimate
        )
        sinceBuild = sinceIdeVersion
        untilBuild = untilIdeVersion
      }
    }
  }
}

tasks {
  publishPlugin {
    enabled =
      false // We handle all publishing internally and don't use the hosting mechanism offered by JetBrains.
  }

  buildPlugin {
    archiveBaseName = pluginName
  }

  check {
    dependsOn("verifyPlugin")
  }
}
