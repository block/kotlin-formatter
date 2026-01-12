# Example README snippet for kotlin-formatter integration
#
# Add this section to your project's README to document the formatting setup
# for developers.

## Getting Started

### Code Formatting

This project uses [kotlin-formatter](https://github.com/block/kotlin-formatter) to maintain consistent code style. Formatting is enforced in CI and via pre-commit hooks.

**Setup (one-time):**

```bash
lefthook install
```

This installs a pre-commit hook that automatically formats your Kotlin code before each commit.

**Manual formatting:**

```bash
# Format all Kotlin files
gradle applyFormatting

# Check formatting (same as CI)
gradle checkFormatting
```

**IDE Plugin (recommended):**

Install the [kotlin-formatter IntelliJ plugin](https://plugins.jetbrains.com/plugin/26482-kotlin-formatter) to format on save.

To configure the plugin, create `.idea/kotlin-formatter.properties`:

```properties
kotlin-formatter.enabled=true
kotlin-formatter.script-path=bin/kotlin-format
```

Enable format-on-save: Settings → Tools → Actions on Save → "Reformat code" (select "Kotlin" file type, **disable** "Optimize imports" for Kotlin).
