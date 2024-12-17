package xyz.block.kotlinformatter

import java.io.File
import java.io.OutputStream

object GitProcessRunner {
  fun start(vararg args: String, workingDir: File? = null): Process {
    val process =
      ProcessBuilder("git", *args)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .apply {
          if (workingDir != null) {
            directory(workingDir)
          } else {
            directory(File(System.getProperty("user.dir")))
          }
        }
        .start()
    return process
  }

  fun run(vararg args: String, workingDir: File? = null, input: (OutputStream.() -> Unit)? = null): String {
    val process = start(args = args, workingDir = workingDir)
    if (input != null) {
      process.outputStream.use { input(it) }
    }
    // TODO: for better performance, allow async processing of this stream instead of reading it all
    // and then returning it
    val output = process.inputStream.bufferedReader().use { it.readText() }
    if (process.waitFor() != 0) {
      throw RuntimeException("git ${args.joinToString(" ")} failed with exit code ${process.exitValue()}")
    }
    return output
  }
}
