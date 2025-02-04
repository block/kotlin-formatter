package xyz.block.kotlinformatter.idea

import com.intellij.codeInsight.actions.onSave.FormatOnSaveOptions
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * A listener that formats a document when it is saved.
 */
class FormatOnSaveListener(private val project: Project, private val formatter: AsyncDocumentFormattingService) :
  FileDocumentManagerListener {

  /**
   * A set of documents that have already been processed by the listener.
   *
   * This prevents redundant processing when `beforeDocumentSaving` is triggered multiple times for the same document.
   */
  private val processedDocuments = ConcurrentHashMap.newKeySet<Document>()

  override fun beforeDocumentSaving(document: Document) {
    val file: VirtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
    // All open projects will receive this event, skip if the file doesn't belong to the current project.
    if (!ProjectFileIndex.getInstance(project).isInProject(file)) return

    val saveOptions = FormatOnSaveOptions.getInstance(project)
    val isFormatOnSaveEnabledForFile =
      saveOptions.isRunOnSaveEnabled &&
        (saveOptions.isAllFileTypesSelected || saveOptions.isFileTypeSelected(file.fileType))

    // Skip if formatting on save for the file is disabled
    logger.debug("Format on save enabled: $isFormatOnSaveEnabledForFile")
    if (!isFormatOnSaveEnabledForFile) return

    // Skip if this document has already been processed
    if (!processedDocuments.add(document)) return

    try {
      logger.debug("Formatting file: ${file.path}")

      val psiFile: PsiFile = PsiManager.getInstance(project).findFile(file) ?: return
      if (!formatter.canFormat(psiFile)) return

      val formattingRange = TextRange(0, document.textLength)
      val formattingContext = FormattingContext.create(psiFile, CodeStyleSettings.getDefaults())

      formatter.formatDocument(document, listOf(formattingRange), formattingContext, false, false)
    } finally {
      processedDocuments.remove(document)
    }
  }

  companion object {
    private val logger = Logger.getInstance(FormatOnSaveListener::class.java.name)
  }
}
