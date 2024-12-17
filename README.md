# Kotlin Formatter

**A command-line tool for formatting Kotlin source code files**. It ensures consistent code style across projects and integrates seamlessly into development workflows.

The tool can:

- **Format files and directories**: Apply consistent formatting to files, directories, or standard input.
- **Integrate with Git workflows**:
  - **Pre-commit**: Format staged files before committing. 
  - **Pre-push**: Check committed files before pushing.

Kotlin Formatter helps automate code formatting, ensuring a clean and consistent codebase.

## Usage

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

