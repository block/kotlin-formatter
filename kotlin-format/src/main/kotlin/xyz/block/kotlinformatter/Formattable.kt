package xyz.block.kotlinformatter

interface Formattable {
  fun name(): String

  fun read(): String

  fun write(content: String)
}
