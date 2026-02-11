# kotlin-formatter - AI Agent Guide

This guide helps AI agents integrate kotlin-formatter into projects.

## Quick Integration Checklist

When asked to add kotlin-formatter to a repository, follow these steps:

### 1. Install the Binary via Hermit

```bash
hermit install kotlin-formatter
```

This installs:
- `bin/kotlin-format` - the main CLI binary
- `bin/kotlin-format.pre-commit.sh` - pre-commit hook script
- `bin/kotlin-format.pre-push.sh` - pre-push hook script
- `bin/.kotlin-formatter-<version>.pkg` - Hermit package metadata

### 2. Add Gradle Plugin (if Gradle project)

**In `gradle/libs.versions.toml`:**

```toml
[versions]
kotlin-formatter = "1.6.3"  # Check for latest version

[plugins]
kotlin-formatter = { id = "xyz.block.kotlin-formatter", version.ref = "kotlin-formatter" }
```

**In `build.gradle.kts`:**

```kotlin
plugins {
    // ... other plugins
    alias(libs.plugins.kotlin.formatter)
}

// Wire checkFormatting into the check task for CI
tasks.named("check") {
    dependsOn("checkFormatting")
}
```

### 3. Add Lefthook Configuration

**Install lefthook via Hermit:**

```bash
hermit install lefthook
```

**Create `lefthook.yml` in project root:**

```yaml
pre-commit:
  parallel: true
  commands:
    format:
      glob: "*.{kt,kts}"
      run: bin/kotlin-format --set-exit-if-changed {staged_files}
      stage_fixed: true
```

**Initialize lefthook:**

```bash
lefthook install
```

### 4. Create `.editorconfig`

Add a root `.editorconfig` file to approximate kotlin-formatter style:

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.{kt,kts}]
indent_style = space
indent_size = 2
max_line_length = 120
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true

[*.{json,yml,yaml}]
indent_style = space
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

### 5. Update `.gitignore`

```gitignore
# Kotlin Formatter
.kotlinformatter/
```

### 6. Document in README

Add a "Code Formatting" section to the project README:

```markdown
## Getting Started

### Code Formatting

This project uses [kotlin-formatter](https://github.com/block/kotlin-formatter) to maintain consistent code style. Formatting is enforced in CI and via pre-commit hooks.

**Setup (one-time):**

\`\`\`bash
lefthook install
\`\`\`

This installs a pre-commit hook that automatically formats your Kotlin code before each commit.

**Manual formatting:**

\`\`\`bash
# Format all Kotlin files
gradle applyFormatting

# Check formatting (same as CI)
gradle checkFormatting
\`\`\`

**IDE Plugin (recommended):**

Install the [kotlin-formatter IntelliJ plugin](https://plugins.jetbrains.com/plugin/26482-kotlin-formatter) to format on save.
```

## Initial Bulk Reformatting

When adding kotlin-formatter to an existing codebase, you'll likely need to reformat all existing files. To prevent this bulk reformat from polluting `git blame` history:

### 1. Create `.git-blame-ignore-revs`

After the bulk reformat commit is merged, add the merge commit SHA to `.git-blame-ignore-revs`:

```bash
# .git-blame-ignore-revs
# Bulk reformat with kotlin-formatter
<merge-commit-sha>
```

**Important:** Use the **merge commit SHA**, not the branch commit SHA.

### 2. Configure Git to Use the Ignore File

Developers need to configure their local Git to respect this file:

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

Add this to project setup instructions in your README.

### 3. GitHub Support

GitHub automatically respects `.git-blame-ignore-revs` in the repository root, so blame views on GitHub will automatically skip these commits.

### Example Workflow

```bash
# 1. Apply formatting to all files
gradle applyFormatting

# 2. Commit the changes
git add .
git commit -m "Format all Kotlin files with kotlin-formatter"

# 3. Push and merge the PR
git push
gh pr create --fill
# (merge the PR on GitHub)

# 4. After merging, add the merge commit SHA to .git-blame-ignore-revs
echo "" >> .git-blame-ignore-revs
echo "# Bulk reformat with kotlin-formatter" >> .git-blame-ignore-revs
echo "<merge-commit-sha>" >> .git-blame-ignore-revs
git add .git-blame-ignore-revs
git commit -m "Add bulk reformat commit to git-blame-ignore-revs"
git push
```

**Reference:** See [misk PR #3606](https://github.com/cashapp/misk/pull/3606) for a real-world example.

## Available Gradle Tasks

The plugin creates two tasks:

- **`applyFormatting`** - Reformats all Kotlin files in the working directory
- **`checkFormatting`** - Checks if committed code is formatted (uses `--pre-push`, `--dry-run`, `--set-exit-if-changed`)

These are **not** attached to lifecycle tasks by default. Wire them as needed:

```kotlin
tasks.named("check") {
    dependsOn("checkFormatting")
}

tasks.named("build") {
    dependsOn("applyFormatting")
}
```

## CLI Binary Location

The Gradle plugin expects the binary at `bin/kotlin-format` by default.

Override with a project property in `gradle.properties`:

```properties
xyz.block.kotlin-formatter.binary=path/to/your/kotlin-format
```

## Formatting Configuration

kotlin-formatter uses **ktfmt with Google style** and a **max width of 120 characters**.

This is currently **not configurable** through the Gradle plugin.

## Common Integration Patterns

### Pattern 1: Hermit + Lefthook + Gradle (Recommended)

This is the full-featured setup used by most projects:

1. Hermit manages the `kotlin-format` and `lefthook` binaries
2. Lefthook runs formatting on pre-commit
3. Gradle plugin provides manual tasks and CI verification
4. `.editorconfig` provides IDE hints

**Files to create/modify:**
- `lefthook.yml`
- `.editorconfig`
- `.gitignore` (add `.kotlinformatter/`)
- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `README.md`

### Pattern 2: Gradle Only (Minimal)

If the project doesn't want git hooks:

1. Add Gradle plugin only
2. Developers run `gradle applyFormatting` manually
3. CI runs `gradle checkFormatting`

**Files to create/modify:**
- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `README.md`

### Pattern 3: Git Hooks Without Lefthook

If the project already has git hooks infrastructure:

1. Symlink or copy `bin/kotlin-format.pre-commit.sh` to `.git/hooks/pre-commit`
2. Symlink or copy `bin/kotlin-format.pre-push.sh` to `.git/hooks/pre-push`
3. Make them executable: `chmod +x .git/hooks/pre-{commit,push}`

**Note:** These scripts expect the binary at `bin/kotlin-format` but can be overridden with `KOTLIN_FORMATTER_EXE` environment variable.

## Version Management

**Always use latest stable version.** Check:
- [GitHub Releases](https://github.com/block/kotlin-formatter/releases)
- [Maven Central](https://central.sonatype.com/artifact/xyz.block.kotlin-formatter/kotlin-formatter)

**Never trust pinned versions** in old branches or documentation examples—they are snapshots, not recommendations.

## IntelliJ Plugin Configuration

For IDE integration, create `.idea/kotlin-formatter.properties`:

```properties
kotlin-formatter.enabled=true
kotlin-formatter.script-path=bin/kotlin-format
```

**Important:** Changes require IDE restart.

Enable format-on-save: Settings → Tools → Actions on Save → "Reformat code" (select "Kotlin" file type, **disable** "Optimize imports" for Kotlin).

## CI Integration

The `checkFormatting` task is designed for CI:

```yaml
# GitHub Actions example
- name: Check Kotlin formatting
  run: gradle checkFormatting
```

This checks **committed code** (not working directory), making it safe for CI.

## Troubleshooting

### Binary not found

**Error:** `bin/kotlin-format: No such file or directory`

**Solution:** Run `hermit install kotlin-formatter` or set `xyz.block.kotlin-formatter.binary` in `gradle.properties`.

### Lefthook not running

**Error:** Pre-commit hook doesn't run

**Solution:** Run `lefthook install` to install hooks into `.git/hooks/`.

### Formatting differences between CLI and IDE

**Ensure:** The IntelliJ plugin is configured to use `bin/kotlin-format` in `.idea/kotlin-formatter.properties`.

## Reference Implementations

- [builder-syndicate PR #25](https://github.com/block/builder-syndicate/pull/25) - Full Hermit + Lefthook + Gradle setup
- [misk PR #3602](https://github.com/cashapp/misk/pull/3602) - Adding kotlin-formatter to existing large codebase

## Key Files Reference

| File | Purpose |
|------|---------|
| `bin/kotlin-format` | Main CLI binary (installed by Hermit) |
| `bin/kotlin-format.pre-commit.sh` | Git pre-commit hook script |
| `bin/kotlin-format.pre-push.sh` | Git pre-push hook script |
| `lefthook.yml` | Lefthook configuration for git hooks |
| `.editorconfig` | Editor formatting hints |
| `gradle/libs.versions.toml` | Gradle version catalog entry |
| `build.gradle.kts` | Gradle plugin application |
| `.idea/kotlin-formatter.properties` | IntelliJ plugin configuration |

## Do NOT

- ❌ Add comments to code explaining formatting changes
- ❌ Use old version numbers from examples—always check for latest
- ❌ Assume Hermit is optional—it's the recommended installation method
- ❌ Forget to update README with setup instructions
- ❌ Skip `.editorconfig`—it helps non-IntelliJ users
- ❌ Put logic in git hooks—use provided scripts or lefthook

## DO

- ✅ Check for latest version before adding to version catalog
- ✅ Wire `checkFormatting` into `check` task for CI
- ✅ Add `.kotlinformatter/` to `.gitignore`
- ✅ Document setup steps in README
- ✅ Use lefthook for modern git hook management
- ✅ Create `.editorconfig` to match formatter style
- ✅ Test the setup by running `lefthook install` and making a commit
