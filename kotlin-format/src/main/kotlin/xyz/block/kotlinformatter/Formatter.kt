package xyz.block.kotlinformatter

import com.facebook.ktfmt.format.Formatter as KtfmtFormatter
import com.facebook.ktfmt.format.FormattingOptions

internal interface Formatter {
  fun format(code: String): String
}

class Ktfmt : Formatter {
  override fun format(code: String): String {
    return KtfmtFormatter.format(formattingStyle, code)
  }

  companion object {
    private const val MAX_WIDTH: Int = 120
    private val formattingStyle: FormattingOptions = KtfmtFormatter.GOOGLE_FORMAT.copy(maxWidth = MAX_WIDTH)
  }
}
