package com.squareup.cash.codeformatter

internal interface Formattable {
  fun name(): String

  fun read(): String

  fun write(content: String)
}
