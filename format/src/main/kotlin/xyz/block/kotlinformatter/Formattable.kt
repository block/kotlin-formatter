package xyz.block.kotlinformatter

internal interface Formattable {
  fun name(): String

  fun read(): String

  fun write(content: String)
}
