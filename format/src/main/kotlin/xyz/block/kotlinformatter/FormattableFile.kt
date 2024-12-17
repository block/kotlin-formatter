package xyz.block.kotlinformatter

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Path

class FormattableFile(private val file: File, private val rootPath: Path? = null) : Formattable {
  override fun name(): String =
    if (rootPath != null) {
      file.relativeTo(rootPath.toFile()).toString()
    } else {
      file.toString()
    }

  override fun read(): String {
    return FileInputStream(file).use { bytes ->
      BufferedReader(InputStreamReader(bytes, Charsets.UTF_8)).use { it.readText() }
    }
  }

  override fun write(content: String) {
    file.writeText(content, Charsets.UTF_8)
  }

  override fun equals(other: Any?): Boolean {
    return (other as? FormattableFile)?.file == file
  }
}
