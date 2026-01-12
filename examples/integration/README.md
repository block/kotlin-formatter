# Integration Examples

This directory contains example configuration files for integrating kotlin-formatter into your project.

## Quick Start

To add kotlin-formatter to your project, follow these steps:

### 1. Install the CLI Binary

```bash
hermit install kotlin-formatter
```

This installs the `kotlin-format` binary to `bin/kotlin-format`.

### 2. Add Git Hooks (Recommended)

Install lefthook:

```bash
hermit install lefthook
```

Copy the example lefthook configuration:

```bash
cp examples/integration/lefthook.yml lefthook.yml
lefthook install
```

### 3. Add Gradle Plugin (Optional)

For Gradle projects, add the plugin to your version catalog and build files:

1. Add to `gradle/libs.versions.toml` (see [libs.versions.toml](./libs.versions.toml))
2. Apply in `build.gradle.kts` (see [build.gradle.kts](./build.gradle.kts))

### 4. Add Editor Configuration

Copy the `.editorconfig` file to your project root:

```bash
cp examples/integration/.editorconfig .editorconfig
```

### 5. Update `.gitignore`

Add `.kotlinformatter/` to your `.gitignore` (see [gitignore](./gitignore)).

### 6. Configure IntelliJ Plugin (Optional)

1. Install the [kotlin-formatter IntelliJ plugin](https://plugins.jetbrains.com/plugin/26482-kotlin-formatter)
2. Copy the example configuration to `.idea/kotlin-formatter.properties` (see [kotlin-formatter.properties](./kotlin-formatter.properties))
3. Restart IntelliJ IDEA

### 7. Document in README

Add setup instructions to your README (see [README-snippet.md](./README-snippet.md)).

## Files in This Directory

| File | Purpose | Target Location |
|------|---------|-----------------|
| [lefthook.yml](./lefthook.yml) | Lefthook configuration for git hooks | Project root |
| [.editorconfig](./.editorconfig) | Editor formatting hints | Project root |
| [libs.versions.toml](./libs.versions.toml) | Gradle version catalog snippet | `gradle/libs.versions.toml` |
| [build.gradle.kts](./build.gradle.kts) | Gradle plugin configuration | Project root or module |
| [gradle.properties](./gradle.properties) | Optional binary location override | Project root |
| [gitignore](./gitignore) | `.gitignore` additions | Append to `.gitignore` |
| [kotlin-formatter.properties](./kotlin-formatter.properties) | IntelliJ plugin config | `.idea/kotlin-formatter.properties` |
| [README-snippet.md](./README-snippet.md) | README documentation | Add to project README |

## Complete Setup Script

For a complete automated setup, run:

```bash
# Install binaries
hermit install kotlin-formatter
hermit install lefthook

# Copy configuration files
cp examples/integration/lefthook.yml lefthook.yml
cp examples/integration/.editorconfig .editorconfig

# Install git hooks
lefthook install

# Add to .gitignore
echo "" >> .gitignore
echo "# Kotlin Formatter" >> .gitignore
echo ".kotlinformatter/" >> .gitignore

# For Gradle projects, manually add the plugin to:
# - gradle/libs.versions.toml
# - build.gradle.kts
```

## Reference Implementations

See these PRs for real-world integration examples:

- [builder-syndicate #25](https://github.com/block/builder-syndicate/pull/25) - Full setup with Hermit + Lefthook + Gradle
- [misk #3602](https://github.com/cashapp/misk/pull/3602) - Integration into existing large codebase

## Support

For questions or issues:
- [kotlin-formatter Issues](https://github.com/block/kotlin-formatter/issues)
- [AGENTS.md](../../AGENTS.md) - AI agent integration guide
- [README.md](../../README.md) - Main documentation
