/*
 * Copyright 2024 (c) 2024 Block, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.block.kotlinformatter

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import xyz.block.kotlinformatter.TriggerFormatter.Companion.FormattingResult
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

private const val EXIT_CODE_FAILURE = 1
private const val EXIT_CODE_BAD_ARGS = 2
private const val EXIT_CODE_FILE_CHANGED = 3

fun main(args: Array<String>) = KotlinFormatter().main(args)

/**
 * Command-line interface to format Kotlin source code files.
 *
 * ## Usage:
 * ```
 * - kotlin-format [OPTIONS] <File1.kt> <File2.kt> ...
 * - kotlin-format --help
 * ```
 */
class KotlinFormatter(private val inputStream: InputStream = System.`in`) :
  CliktCommand(name = "kotlin-format") {
  private val files: List<String> by argument(help = HELP_FILES).multiple()
  private val setExitIfChanged: Boolean by
    option("--set-exit-if-changed", help = HELP_SET_EXIT_IF_CHANGED).flag(default = false)
  private val dryRun: Boolean by option("--dry-run", help = HELP_DRY_RUN).flag(default = false)
  private val preCommit: Boolean by option("--pre-commit", help = HELP_PRE_COMMIT).flag(default = false)
  private val prePush: Boolean by option("--pre-push", help = HELP_PRE_PUSH).flag(default = false)
  private val pushCommit: String by option("--push-commit", help = HELP_PUSH_COMMIT).default("HEAD")
  private val stats: Boolean by
    option("--print-stats", help = HELP_PRINT_STATS, envvar = "KOTLIN_FORMATTER_STATS").flag(default = false)

  override fun help(context: Context) = HELP_MESSAGE

  override fun run() {
    // 1. Validate inputs
    if (files.isEmpty()) {
      throw PrintHelpMessage(this.currentContext)
    }
    if (preCommit && prePush) {
      // These two options are mutually exclusive
      throw PrintMessage("--pre-push and --pre-commit are mutually exclusive", EXIT_CODE_BAD_ARGS, true)
    }
    if (prePush && !dryRun) {
      throw PrintMessage("--pre-push without --dry-run is currently unsupported", EXIT_CODE_BAD_ARGS, true)
    }

    val timeStart = Instant.now()
    val stdStreamsMode = files.size == 1 && files.contains("-")

    // 2. Create configs from args
    val formattingConfigs =
      if (stdStreamsMode) {
        FormattingConfigs.forStdStreams(inputStream) { echo(it, trailingNewline = false) }
      } else if (preCommit) {
        FormattingConfigs.forPreCommit(files, dryRun)
      } else if (prePush) {
        FormattingConfigs.forPrePush(files, dryRun, pushCommit)
      } else {
        FormattingConfigs.forWorkingDir(files, dryRun)
      }

    val timeConfigged = Instant.now()

    // 3. Trigger the formatter
    val formatter = TriggerFormatter(Ktfmt())

    val formattingResults =
      formattingConfigs.formattables
        .parallelStream()
        .map { formattable -> formatter.format(formattable, formattingConfigs) }
        .toSortedSet()

    val timeFormatted = Instant.now()

    // Making a single string and outputting it once is much faster than calling echo for each
    // result individually
    val output = StringBuilder()

    if (formattingConfigs.formattables.isEmpty()) {
      output.appendLine("Nothing to format")
    }

    var hasFailure = false
    var hasFileChanged = false

    formattingResults.forEach { formattingResult ->
      when (formattingResult) {
        is FormattingResult.AlreadyFormatted -> Unit
        is FormattingResult.WouldFormat -> {
          output.appendLine("🛠️ Would format ${formattingResult.fileName}")
          hasFileChanged = true
        }

        is FormattingResult.Formatted -> {
          output.appendLine("✅ Formatted ${formattingResult.fileName}")
          hasFileChanged = true
        }

        is FormattingResult.FormattingError -> {
          output.appendLine("⛔️ ${formattingResult.message}")
          hasFailure = true
        }
      }
    }

    if (hasFileChanged && prePush) {
      output.appendLine(
        "⚠️ The committed files have formatting errors. Please format the files and commit the formatting changes."
      )
    }

    // Do not output anything apart from formatted code when in stdStreamsMode
    if (!stdStreamsMode || dryRun) {
      echo(output, trailingNewline = false)

      val timeEnd = Instant.now()
      if (stats) {
        echo("Time to configure: ${Duration.between(timeStart, timeConfigged).toNanos() / 1.0e6f}ms", err = true)
        echo("   Time to format: ${Duration.between(timeConfigged, timeFormatted).toNanos() / 1.0e6f}ms", err = true)
        echo("   Time to report: ${Duration.between(timeFormatted, timeEnd).toNanos() / 1.0e6f}ms", err = true)
        echo("Formattable count: ${formattingConfigs.formattables.size}", err = true)
        echo(
          "       Blob count: ${formattingConfigs.formattables.filterIsInstance<FormattableBlob>().size}",
          err = true,
        )
        echo(
          "       File count: ${formattingConfigs.formattables.filterIsInstance<FormattableFile>().size}",
          err = true,
        )
        echo("  Chars processed: ${formatter.charsProcessed()}", err = true)
      }
    }

    if (hasFailure) {
      throw ProgramResult(EXIT_CODE_FAILURE)
    }

    if (setExitIfChanged && hasFileChanged) {
      throw ProgramResult(EXIT_CODE_FILE_CHANGED)
    }
  }

  // Stream doesn't have a toSortedSet() method, so we need to convert it to a list first.
  private fun <E : Comparable<E>> Stream<E>.toSortedSet() = toList().toSortedSet()

  companion object {
    const val HELP_MESSAGE = "Command-line interface to format Kotlin source code files."
    const val HELP_FILES = "Files or directory to format."
    const val HELP_SET_EXIT_IF_CHANGED =
      "Set the program to exit with a non-zero exit code if any files needed to be changed."
    const val HELP_DRY_RUN = "Displays the changes that would be made but does not write the changes to disk."
    const val HELP_PRE_COMMIT =
      "Format staged files as part of the pre-commit process. Mutually exclusive with --pre-push."
    const val HELP_PRE_PUSH =
      "Check committed files as part of the pre-push process. Mutually exclusive with --pre-commit."
    const val HELP_PUSH_COMMIT = "The SHA of the commit to use for pre-push. Defaults to 'HEAD'."
    const val HELP_PRINT_STATS = "Emit performance-related statistics to help diagnose performance issues."
  }
}
