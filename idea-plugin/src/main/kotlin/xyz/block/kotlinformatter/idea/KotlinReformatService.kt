package xyz.block.kotlinformatter.idea

import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiFile
import java.io.File
import java.io.InputStreamReader
import kotlin.text.Charsets.UTF_8

/**
 * A service that overrides the default IntelliJ formatting behavior for Kotlin files.
 *
 * If a given Kotlin file does not belong to a Cash Server project or is not part of an enabled formatting module, the
 * default IntelliJ formatting behavior will be applied.
 */
class KotlinReformatService : AsyncDocumentFormattingService() {
  override fun getFeatures(): MutableSet<FormattingService.Feature> {
    return mutableSetOf()
  }

  /**
   * If this is false, the default IntelliJ formatting behavior will be applied and createFormattingTask will not be
   * called.
   */
  override fun canFormat(file: PsiFile): Boolean {
    // if (!file.project.isCashServer()) return false TODO: Replace this logic
    if (!file.name.endsWith(".kt")) return false

    return !isFormattingIgnored(file)
  }

  override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
    return object : FormattingTask {
      override fun run() {
        ApplicationManager.getApplication().executeOnPooledThread {
          try {
            val formattedCode = formatFile(request.context.psiElement.containingFile) ?: request.documentText
            request.onTextReady(formattedCode)
          } catch (e: Exception) {
            // If an error occurs, notify IntelliJ
            request.onError("Formatting Error", e.message ?: "Unknown error")
          }
        }
      }

      override fun cancel(): Boolean {
        return true
      }
    }
  }

  override fun getNotificationGroupId(): String {
    return "Reformat Kotlin Files"
  }

  override fun getName(): String {
    return "Reformat Kotlin Files"
  }

  /** Returns formatted content or null if the file is already formatted. */
  private fun formatFile(file: PsiFile): String? {
    //val scriptPath = service<FormatScriptStateService>().state.path ?: return null
    val scriptPath = file.project.service<FormatScriptStateService>().path
    println("scriptPath: $scriptPath")

    val processBuilder =
      ProcessBuilder(scriptPath, "--set-exit-if-changed", "-").directory(file.project.basePath?.let { File(it) })

    val process = processBuilder.start()

    // Stream file content to the process's input
    file.virtualFile.inputStream.use { inputStream ->
      process.outputStream.use { outputStream -> inputStream.copyTo(outputStream) }
    }

    val formattedContent = process.inputStream.bufferedReader(UTF_8).use { it.readText() }

    // Wait for the process to complete
    val exitCode = process.waitFor()

    LOG.info("Process exited with code: $exitCode")
    when (exitCode) {
      3 -> return formattedContent
      0 -> LOG.info("Nothing to format")
      else -> LOG.error("Formatting failed with exit code $exitCode")
    }

    return null
  }

  private fun isFormattingIgnored(file: PsiFile): Boolean {
    val filePath = file.viewProvider.virtualFile.path
    val basePath = file.project.basePath ?: return false // Should never be null in cash-server

    // Check if the file path starts with the base path
    if (!filePath.startsWith(basePath)) return false

    // Extract the module name from the file path
    val module = filePath.substring(basePath.length + 1).split("/").firstOrNull() ?: return false

    val formattingIgnoreModules = file.project.getFileContent(FORMATTING_IGNORE_FILE)?.lines()?.toSet().orEmpty()

    if (formattingIgnoreModules.contains(module)) {
      LOG.info("File in formatting ignore list: $module")
      return true
    }

    return false
  }

  private fun Project.getFileContent(filePath: String): String? {
    val rootDir = this.guessProjectDir()
    if (rootDir == null) {
      // TODO: Replace with logger
      println("The project root directory is null - skipping")
      return null
    }
    val file = rootDir.findFile(filePath)
    if (file == null) {
      // TODO: Replace with logger
      println("The file at $filePath is missing")
      return null
    }
    return file.loadText()
  }

  private fun VirtualFile.loadText(): String =
    InputStreamReader(this.inputStream).use { reader ->
      return String(FileUtilRt.loadText(reader, this.length.toInt()))
    }

  companion object {
    private val LOG = Logger.getInstance(KotlinReformatService::class.java)
    private const val FORMATTING_IGNORE_FILE = "git-hooks/formatting-ignore.txt"
  }
}
