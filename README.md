# Kotlin Formatter

[![release](https://img.shields.io/maven-central/v/xyz.block.kotlin-formatter/kotlin-formatter?label=release&color=blue)](https://central.sonatype.com/namespace/xyz.block.kotlin-formatter)
[![main](https://github.com/block/kotlin-formatter/actions/workflows/push.yml/badge.svg)](https://github.com/block/kotlin-formatter/actions/workflows/push.yml)

This project provides:
- **A command-line tool for formatting Kotlin source code files**, implemented as a wrapper around [ktfmt](https://github.com/facebook/ktfmt/tree/main).
- **An IntelliJ idea plugin** for formatting Kotlin source code files.
- **A gradle plugin** for formatting and checking the format of Kotlin source code files.

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
- **Daemon mode (experimental)**: Run a background process to avoid JVM startup time on repeated formatting calls.

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
| `--daemon`              | Run the CLI in daemon mode.                                                               |
| `--stop-daemon`         | Stop the daemon if it's running.                                                          |
| `-h, --help`            | Show help message and exit.                                                          |

#### Arguments

| Argument      | Description                                |
|---------------|--------------------------------------------|
| `<files>`     | Files or directories to format. Use `-` for standard input. |

### ⚡ Daemon Mode (Experimental)
⚠️ _Warning: daemon mode is experimental and subject to change_ ⚠️
Formatting via the CLI involves slow Java startup. To avoid this, Kotlin Formatter can be run in daemon mode.
```bash
kotlin-format --daemon
```

In daemon mode, the CLI starts a server that listens for requests to format files. The port is chosen dynamically, and written to .kotlinformatter/kf.lock in the root of the git dir where kotlin-format is run.
Only one daemon will run per git dir. If a daemon is already running, invoking `kotlin-format --daemon` will be a no-op, unless the running daemon is an older version.

.kotlinformatter/kf.lock should be added to your .gitignore.

The daemon only supports a limited set of formatting options compared to the CLI. To format files using the daemon, send a message containing the options:
```bash
pre-commit <files>
pre-push <sha> <files>
```

The daemon will respond with a status code on the first line of the response, followed by the output of the formatting operation.

A full example of using the daemon might look like this:
```bash
# start the daemon in the background
nohup kotlin-format --daemon > /dev/null 2>&1 &
# read values from lockfile
line=$(head -n 1 "$daemon_lock_file")
version=$(echo "$line" | cut -d ' ' -f 1)
port=$(echo "$line" | cut -d ' ' -f 2)
# format files
echo pre-commit file1.kt file2.kt | nc localhost $port
```

Additionally, the daemon knows the following commands:
```bash
exit # stop the daemon
status # get the status of the daemon
```

The daemon will automatically shut down after one hour of inactivity, or if it's been running for more than 24 hours.
You can also manually shut down the daemon by sending the `exit` command, or running `kotlin-format --stop-daemon`.

### Installing the CLI

There are multiple ways to install and use the Kotlin Formatter CLI:

#### 1. Using [Hermit](https://github.com/cashapp/hermit)
If you don't have Hermit installed, follow the [Hermit Getting Started Guide](https://cashapp.github.io/hermit/usage/get-started/) to install it first. Once Hermit is installed, you can install the Kotlin Formatter CLI using:
```bash

hermit install kotlin-formatter
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
cd kotlin-formatter-dist-$VERSION # dir was kotlin-format-shadow-$VERSION up to and including v1.6.1
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

## Gradle Plugin Overview
The gradle plugin provides `applyFormatting` and `checkFormatting` tasks, that apply and check the formatting respectively.
Note that `checkFormatting` checks the formatting of the **committed** code, and therefore is primarily meant to be used together with a pre-commit git hook (coming soon) that does the formatting.

### Usage
You can apply the plugin by adding the necessary entry to your version catalog:
```
...
[plugins]
kotlinFormatter = { id = "xyz.block.kotlin-formatter", version = "1.6.2" } # replace version with latest
```

and then applying it in your gradle projects:
```
plugins {
  alias(libs.plugins.kotlinFormatter)
}
```

Although the tasks are created, they are not attached to any lifecycle tasks by default. You may attach them as dependencies depending on your preferred workflow.

### Configuration
The gradle plugin by default looks for the kotlin-format binary at `bin/kotlin-format` relative to the root project of the Gradle build.
This is intended to work with the recommended configuration of using hermit to manage your build tooling.
If your `kotlin-format` binary is somewhere else, you can set a project property in your gradle.properties file to tell the plugin where to look. For example:

```properties
# root gradle.properties
xyz.block.kotlin-formatter.binary=../some/other/path/bin/kotlin-format
```

## Git Hooks Overview
The git hooks in the `git-hooks/` folder provide a convenient way to automatically format Kotlin code on commit and verify it is formatted on push.
Notably, the pre-commit hook uses the `--pre-commit` command-line option for the binary, which ensures that for partially staged files (e.g. via `git add -p`) only the staged component is formatted, while the on-disk file is left unformatted.
Likewise, the pre-push hook ensures the committed code being pushed is formatted, rather than checking what's on-disk.

To use these, you can symlink the hook scripts into your git hooks directory, or invoke them from existing pre-commit or pre-push scripts as needed.
Note that the pre-push hook in particular expects arguments and stdin matching the standard git pre-push hook contract, so if you are invoking this hook as a subroutine from your own pre-push hook, you will need to ensure the arguments and stdin are set up correctly.
Both scripts assume the formatter binary is available at `bin/kotlin-format` relative to the git root, which conforms to the expected setup when using the Hermit package.
However, the `KOTLIN_FORMATTER_EXE` environment variable may be set in order to override this location if needed.
