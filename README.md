# Kotlin Formatter

[![release](https://img.shields.io/maven-central/v/xyz.block.kotlin-formatter/kotlin-formatter?label=release&color=blue)](https://central.sonatype.com/namespace/xyz.block.kotlin-formatter)
[![main](https://github.com/block/kotlin-formatter/actions/workflows/push.yml/badge.svg)](https://github.com/block/kotlin-formatter/actions/workflows/push.yml)

This project provides:
- **A command-line tool for formatting Kotlin source code files**, implemented as a wrapper around [ktfmt](https://github.com/facebook/ktfmt/tree/main).
- **An IntelliJ idea plugin** for formatting Kotlin source code files.

It can be used to automate code formatting, ensuring a **clean and consistent codebase**, while integrating seamlessly into development workflows.

## Why Kotlin Formatter?
The main goal of this project is to establish a **single, consistent formatting standard** across CLI, Gradle, and IntelliJ, while integrating smoothly into existing developer workflows.

While ktfmt provides a solid foundation, we built Kotlin Formatter to address additional use cases:
- **Configurable max-width** – When we first explored ktfmt, its CLI [didn’t support configuring max-width](https://github.com/facebook/ktfmt/pull/470). Different projects and repositories have varying formatting standards, and we wanted to support this flexibility. In our case, we use a standard formatting width of 120 characters.
- **Automated Formatting Support in Git Workflow** – We wanted formatting to be automatically applied before code is committed to prevent formatting drift as early as possible, keeping it closest to the inner development loop.
- **Format-on-save for IntelliJ** – Format-on-save for IntelliJ – While ktfmt provides IntelliJ integration, our plugin includes format-on-save support in addition to manual formatting, reducing manual steps for developers.
- **Consistent formatting experience across tools** – We ensure consistency by having CLI, Gradle, and IntelliJ all use the same CLI under the hood, applying the same formatting rules across all workflows.

We hope these tools will make it easier for teams to maintain a **consistent formatting experience with automated formatting**—whether through Git hook integration or format-on-save—while seamlessly integrating into existing development workflows. 🚀

## CLI Overview
The CLI provides the following capabilities:
- **Format files and directories**: Apply consistent formatting to files, directories, or standard input.
- **Integrate with Git workflows**:
  - **Pre-commit**: Format staged files before committing.
  - **Pre-push**: Check committed files before pushing.

### Usage
```bash
kotlin-format [OPTIONS] [FILES...]
```
#### Options

| Option                  | Description                                                                                |
|-------------------------|-------------------------------------------------------------------------------------------|
| `--set-exit-if-changed` | Exit with a non-zero code if any files needed changes.                                    |
| `--dry-run`             | Display the changes that would be made without writing them to disk.                     |
| `--pre-commit`          | Format staged files as part of the pre-commit process. *Mutually exclusive with `--pre-push`.* |
| `--pre-push`            | Check committed files as part of the pre-push process. *Mutually exclusive with `--pre-commit`.* |
| `--push-commit=<text>`  | The SHA of the commit to use for pre-push. Defaults to `HEAD`.                            |
| `--print-stats`         | Emit performance-related statistics to help diagnose performance issues.                 |
| `-h, --help`            | Show help message and exit.                                                          |

#### Arguments

| Argument      | Description                                |
|---------------|--------------------------------------------|
| `<files>`     | Files or directories to format. Use `-` for standard input. |

### Installing the CLI

There are multiple ways to install and use the Kotlin Formatter CLI:

#### 1. Using [Hermit](https://github.com/cashapp/hermit)
If you don't have Hermit installed, follow the [Hermit Getting Started Guide](https://cashapp.github.io/hermit/usage/get-started/) to install it first. Once Hermit is installed, you can install the Kotlin Formatter CLI using:
```bash

hermit install kotlin-format
```
Once installed, you can run the CLI with:
```bash
kotlin-format [OPTIONS] [FILES...]
```

#### 2. Downloading the pre-packaged distribution with a script
A pre-packaged distribution is available on [Maven Central](https://repo1.maven.org/maven2/xyz/block/kotlin-formatter/kotlin-formatter-dist/) and [GitHub Releases](https://github.com/block/kotlin-formatter/releases)
```bash
VERSION=X.Y.Z
curl -L -o kotlin-formatter-dist.zip https://github.com/block/kotlin-formatter/releases/download/$VERSION/kotlin-formatter-dist-$VERSION.zip
unzip kotlin-formatter-dist.zip
cd kotlin-format-shadow-$VERSION
```
Once downloaded and extracted, you can run the CLI with:
```bash
./bin/kotlin-format [OPTIONS] [FILES...]
```
    
#### 3. Downloading the JAR manually
A fat JAR of the CLI is available on [Maven Central](https://repo1.maven.org/maven2/xyz/block/kotlin-formatter/kotlin-formatter/) and [GitHub Releases](https://github.com/block/kotlin-formatter/releases). Once downloaded, you can run the CLI with:
```bash
java -jar path/to/kotlin-formatter-$version-all.jar [OPTIONS] [FILES...]
```

## IntelliJ IDEA Plugin Overview
The plugin enables Kotlin file formatting **on save** or via the **format action**.

### Usage
To configure the plugin for a project, create a properties file named kotlin-formatter.properties and place it in the `.idea` directory. The following properties are supported
- `kotlin-formatter.enabled`: Enable or disable the plugin, disabled by default.
- `kotlin-formatter.script-path`: Path to the Kotlin Formatter script. The `kotlin-format` library in this project is used if this is not specified.

Example:
```properties
kotlin-formatter.enabled=true
kotlin-formatter.script-path=bin/kotlin-format
```

🚨 Changes to this configuration require an IDE restart to take effect.

#### Enabling Format-on-Save
To enable formatting of files on save, navigate to "Settings" > "Tools" > Actions on Save", activate the "Reformat code" checkbox, and ensure that the "Kotlin" file type is selected.
Make sure "Optimize imports" is NOT enabled for the "Kotlin" file type.

### IntelliJ IDEA Plugin Installation
[Download from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26482-kotlin-formatter)