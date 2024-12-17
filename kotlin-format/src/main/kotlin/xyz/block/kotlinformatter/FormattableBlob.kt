package xyz.block.kotlinformatter

import java.nio.file.Path
import kotlin.io.path.pathString

internal class FormattableBlob(internal val path: Path, private val mode: String, private val hash: String) :
  Formattable {
  override fun name(): String = path.pathString

  override fun read(): String {
    return GitProcessRunner.run("cat-file", "-p", hash)
  }

  override fun write(content: String) {
    val newHash = GitProcessRunner.run("hash-object", "-w", "--stdin") { write(content.toByteArray()) }.trim()

    // update-index commands are apparently not threadsafe, and can result in errors or corruption
    // in the Git index.
    synchronized(GitProcessRunner) {
      GitProcessRunner.run("update-index", "--cacheinfo", "${mode},${newHash},${path.pathString}")
    }
  }

  override fun equals(other: Any?): Boolean {
    // For ease of testing we only check the path
    return (other as? FormattableBlob)?.path == path
  }

  // Having human-readable toString() makes debugging test failures easier
  override fun toString() = "Blob at $path"
}
