# Kotlin Formatter

[![release](https://img.shields.io/maven-central/v/xyz.block.kotlin-formatter/kotlin-formatter?label=release&color=blue)](https://central.sonatype.com/namespace/xyz.block.kotlin-formatter)
[![main](https://github.com/block/kotlin-formatter/actions/workflows/push.yml/badge.svg)](https://github.com/block/kotlin-formatter/actions/workflows/push.yml)

This project provides:
- **A command-line tool for formatting Kotlin source code files**, implemented as a wrapper around [ktfmt](https://github.com/facebook/ktfmt/tree/main).
- **An IntelliJ idea plugin** for formatting Kotlin source code files.

It can be used to automate code formatting, ensuring a clean and consistent codebase, while integrating seamlessly into development workflows.

The CLI tool can:

- **Format files and directories**: Apply consistent formatting to files, directories, or standard input.
- **Integrate with Git workflows**:
  - **Pre-commit**: Format staged files before committing. 
  - **Pre-push**: Check committed files before pushing.

The plugin can format Kotlin files on save, or on the format action.

## CLI Usage

```bash
kotlin-format [OPTIONS] [FILES...]
```
### Options

| Option                  | Description                                                                                |
|-------------------------|-------------------------------------------------------------------------------------------|
| `--set-exit-if-changed` | Exit with a non-zero code if any files needed changes.                                    |
| `--dry-run`             | Display the changes that would be made without writing them to disk.                     |
| `--pre-commit`          | Format staged files as part of the pre-commit process. *Mutually exclusive with `--pre-push`.* |
| `--pre-push`            | Check committed files as part of the pre-push process. *Mutually exclusive with `--pre-commit`.* |
| `--push-commit=<text>`  | The SHA of the commit to use for pre-push. Defaults to `HEAD`.                            |
| `--print-stats`         | Emit performance-related statistics to help diagnose performance issues.                 |
| `-h, --help`            | Show help message and exit.                                                          |

### Arguments

| Argument      | Description                                |
|---------------|--------------------------------------------|
| `<files>`     | Files or directories to format. Use `-` for standard input. |

## Installing CLI

There are multiple ways to install and use the Kotlin Formatter CLI:

### 1. Using [Hermit](https://github.com/cashapp/hermit)
If you don't have Hermit installed, follow the [Hermit Getting Started Guide](https://cashapp.github.io/hermit/usage/get-started/) to install it first. Once Hermit is installed, you can install the Kotlin Formatter CLI using:
```bash

hermit install kotlin-format
```
Once installed, you can run the CLI with:
```bash
kotlin-format [OPTIONS] [FILES...]
```

### 2. Downloading the pre-packaged distribution with a script
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
    
### 3. Downloading the JAR manually
A fat JAR of the CLI is available on [Maven Central](https://repo1.maven.org/maven2/xyz/block/kotlin-formatter/kotlin-formatter/) and [GitHub Releases](https://github.com/block/kotlin-formatter/releases). Once downloaded, you can run the CLI with:
```bash
java -jar path/to/kotlin-formatter-$version-all.jar [OPTIONS] [FILES...]
```

## Idea Plugin Usage

A properties file can be used to configure the plugin for each project. The properties file should be named `kotlin-formatter.properties` and placed in the `.idea` of the project. The following properties are supported:

- `kotlin-formatter.enabled`: Enable or disable the plugin, disabled by default.
- `kotlin-formatter.script-path`: Path to the Kotlin Formatter script. The `kotlin-format` library in this project is used if this is not specified.

Example:
```properties
kotlin-formatter.enabled=true
kotlin-formatter.script-path=bin/kotlin-format
```