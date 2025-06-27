import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("application")
  id("org.jetbrains.kotlin.jvm")
  id("com.autonomousapps.dependency-analysis")
  alias(libs.plugins.shadow)
  alias(libs.plugins.mavenPublish)
}

dependencies {
  implementation(libs.clikt)
  implementation(libs.cliktCore)
  implementation(libs.ktfmt)

  testImplementation(libs.junitApi)
  testImplementation(libs.assertj)
  testImplementation(libs.mordantCore)

  testRuntimeOnly(libs.junitEngine)
  testRuntimeOnly(libs.junitLauncher)
}

val javaTarget = JavaLanguageVersion.of(libs.versions.java.get())
val kotlinTarget = JvmTarget.fromTarget(libs.versions.java.get())
val artifactName = "kotlin-formatter"

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

application {
  mainClass.set("xyz.block.kotlinformatter.CliKt")
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

val shadowJar = tasks.named("shadowJar", ShadowJar::class) {
  group = "Build"
  description = "Creates a fat jar"
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true

  from(sourceSets.main.map { it.output })
  from(project.configurations.runtimeClasspath)

  // Excluding these helps shrink our binary dramatically
  exclude("**/*.kotlin_metadata")
  exclude("**/*.kotlin_module")
  exclude("META-INF/maven/**")
}

tasks.register("buildBinary", Sync::class.java) {
  from(shadowJar)
  into(layout.projectDirectory.dir("build/release"))
}

// ----------------------
// Publishing Configuration
// ----------------------
mavenPublishing {
  coordinates(group.toString(), artifactName, version.toString())
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()

  pom {
    name.set("kotlin-formatter")
    description.set("A command-line tool designed to enforce consistent code formatting for Kotlin.")
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

publishing {
  publications {
    create<MavenPublication>("distZip") {
      artifactId = "$artifactName-dist"
      artifact(tasks.shadowDistZip)
    }
  }
}

// ----------------------
// Test Configuration
// ----------------------

tasks.withType<Test>().configureEach { useJUnitPlatform() }

tasks.named("test", Test::class.java).configure {
  useJUnitPlatform {
    excludeTags("integration")
  }
}

tasks.register("integrationTest", Test::class.java) {
  useJUnitPlatform {
    includeTags("integration")
  }
  dependsOn(shadowJar)
  environment("JAR_UNDER_TEST", shadowJar.map { it.outputs.files.singleFile.absolutePath }.get())
}
