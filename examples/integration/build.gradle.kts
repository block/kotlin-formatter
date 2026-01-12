// Example build.gradle.kts snippet for kotlin-formatter integration
//
// This demonstrates how to apply the kotlin-formatter Gradle plugin and
// wire it into your build lifecycle.
//
// Setup:
//   1. Add kotlin-formatter to your gradle/libs.versions.toml (see libs.versions.toml example)
//   2. Add the plugin application and task wiring to your build.gradle.kts

plugins {
    // ... your other plugins
    kotlin("jvm") version "1.9.22"
    
    // Apply the kotlin-formatter plugin
    alias(libs.plugins.kotlin.formatter)
}

// Wire checkFormatting into the check task for CI
tasks.named("check") {
    dependsOn("checkFormatting")
}

// Optional: Wire applyFormatting into build if you want automatic formatting
// tasks.named("build") {
//     dependsOn("applyFormatting")
// }

// The plugin provides two tasks:
// - applyFormatting: Formats all Kotlin files
// - checkFormatting: Checks if committed code is formatted (for CI)
//
// Run manually:
//   ./gradlew applyFormatting  # Format all Kotlin files
//   ./gradlew checkFormatting  # Check formatting (same as CI)
