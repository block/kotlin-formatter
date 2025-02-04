package xyz.block.kotlinformatter.idea

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class FormatOnSavePostStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    try {
      logger.debug("Registering KotlinFormatOnSaveListener for file document-related events for ${project.name}")
      project.messageBus
        .connect()
        .subscribe(FileDocumentManagerListener.TOPIC, FormatOnSaveListener(project, KotlinReformatService()))
    } catch (e: Exception) {
      logger.error("Failed to register FormatOnSaveListener for project: ${project.name}", e)
    }
  }

  companion object {
    private val logger = Logger.getInstance(FormatOnSavePostStartupActivity::class.java.name)
  }
}
