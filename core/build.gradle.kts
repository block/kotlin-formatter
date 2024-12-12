import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("application")

  alias(libs.plugins.kotlin)
  alias(libs.plugins.shadow)
}

dependencies {
  implementation(libs.clikt)
  implementation(libs.ktfmt)

  testImplementation(libs.junitApi)
  testImplementation(libs.assertj)
  testImplementation(libs.junitParams)

  testRuntimeOnly(libs.junitEngine)
  testRuntimeOnly(libs.junitLauncher)
}

val javaTarget = JavaLanguageVersion.of(libs.versions.java.get())

java {
  toolchain {
    languageVersion = javaTarget
  }
}

tasks.withType<JavaCompile> {
  options.release.set(javaTarget.asInt())
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = javaTarget.toString()
  }
}

application {
  mainClass.set("com.squareup.cash.codeformatter.CodeFormatterKt")
}

val shadowJar = tasks.named("shadowJar", ShadowJar::class) {
  group = "Build"
  description = "Creates a fat jar"
  archiveFileName = "kotlin-formatter-all.jar"
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
