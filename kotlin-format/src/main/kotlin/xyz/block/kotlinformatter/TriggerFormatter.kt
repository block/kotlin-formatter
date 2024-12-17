package xyz.block.kotlinformatter

import java.io.*
import java.util.concurrent.atomic.AtomicLong

internal class TriggerFormatter(private val formatter: Formatter) {
  private val charsProcessed = AtomicLong(0)

  fun format(formattable: Formattable, configs: FormattingConfigs): FormattingResult {
    val fileName = formattable.name()

    return try {
      val code = formattable.read()

      val formattedCode = formatter.format(code)
      charsProcessed.addAndGet(code.length.toLong())

      when {
        // Already formatted
        code == formattedCode -> FormattingResult.AlreadyFormatted(fileName)

        // Dry run
        configs.dryRun -> {
          FormattingResult.WouldFormat(fileName)
        }

        // Write formatted code
        else -> {
          formattable.write(formattedCode)
          FormattingResult.Formatted(fileName)
        }
      }
    } catch (e: IOException) {
      val errorMessage = "I/O error while processing $fileName: ${e.message}; skipping."

      FormattingResult.FormattingError(fileName, errorMessage, e)
    } catch (e: Exception) {
      val errorMessage = "Unexpected error formatting $fileName: ${e.message}"

      FormattingResult.FormattingError(fileName, errorMessage, e)
    }
  }

  internal fun charsProcessed(): Long {
    return charsProcessed.get()
  }

  companion object {
    sealed class FormattingResult(private val compareOrder: Int, val fileName: String) : Comparable<FormattingResult> {
      class AlreadyFormatted(fileName: String) : FormattingResult(0, fileName)

      class WouldFormat(fileName: String) : FormattingResult(1, fileName)

      class Formatted(fileName: String) : FormattingResult(2, fileName)

      class FormattingError(fileName: String, val message: String, val e: Exception) : FormattingResult(3, fileName)

      override fun compareTo(other: FormattingResult): Int {
        if (this.compareOrder != other.compareOrder) {
          return this.compareOrder - other.compareOrder
        }
        return this.fileName.compareTo(other.fileName)
      }

      // Override equals and hashCode so they agree with compareTo
      override fun equals(other: Any?): Boolean {
        return (other as? FormattingResult)?.compareTo(this) == 0
      }

      override fun hashCode(): Int {
        return compareOrder.hashCode() xor fileName.hashCode()
      }

      // This is useful for debugging test failures
      override fun toString(): String {
        return when (this) {
          is AlreadyFormatted -> "Already formatted $fileName"
          is WouldFormat -> "Would format $fileName"
          is Formatted -> "Formatted $fileName"
          is FormattingError -> "Formatting error for $fileName: $message"
        }
      }
    }
  }
}
