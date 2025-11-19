import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("java-gradle-plugin")
  id("org.jetbrains.kotlin.jvm")
  id("com.autonomousapps.dependency-analysis")
  alias(libs.plugins.mavenPublish)
}

dependencies {
  testRuntimeOnly(libs.junitEngine)
  testRuntimeOnly(libs.junitLauncher)
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

gradlePlugin {
  plugins {
    create("kotlin-formatter") {
      id = "xyz.block.kotlinformatter"
      implementationClass = "xyz.block.kotlinformatter.GradlePlugin"
    }
  }
}

// ----------------------
// Publishing Configuration
// ----------------------
mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()

  pom {
    description.set("A gradle plugin to apply and check code formatting for Kotlin.")
    inceptionYear.set("2024")
    url.set("https://github.com/block/kotlin-formatter")

    licenses {
      license {
        name.set("The Apache Software License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("repo")
      }
    }

    developers {
      developer {
        id.set("block")
        name.set("Block")
        url.set("https://github.com/block")
      }
    }

    scm {
      url.set("https://github.com/block/kotlin-formatter")
      connection.set("scm:git:git://github.com/block/kotlin-formatter.git")
      developerConnection.set("scm:git:ssh://github.com/block/kotlin-formatter.git")
    }
  }
}

// ----------------------
// Test Configuration
// ----------------------

tasks.test {
  useJUnitPlatform()
  environment("KOTLIN_FORMATTER_STATS", "false")
}
