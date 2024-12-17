package xyz.block.kotlinformatter

import java.io.InputStream
import kotlin.text.Charsets.UTF_8

class FormattableStdStreams(private val inputStream: InputStream, private val output: (String) -> Unit) : Formattable {
  override fun name(): String {
    return "<stdin>"
  }

  override fun read(): String {
    return inputStream.bufferedReader(UTF_8).use { it.readText() }
  }

  override fun write(content: String) {
    output(content)
  }
}
