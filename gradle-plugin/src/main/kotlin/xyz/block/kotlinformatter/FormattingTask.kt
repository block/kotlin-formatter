package xyz.block.kotlinformatter

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations

@UntrackedTask(because = "Neither inputs nor outputs")
abstract class FormattingTask @Inject constructor(@Internal val execOps: ExecOperations) : DefaultTask() {

  init {
    group = "Formatting"
  }

  @get:Internal abstract val binary: Property<String>
  @get:Internal abstract val rootDir: DirectoryProperty
  @get:Internal abstract val targetDir: DirectoryProperty
  @get:Internal abstract val args: ListProperty<String>

  @TaskAction
  fun check() {
    val result =
      execOps
        .exec {
          it.commandLine(binary.get(), *args.get().toTypedArray(), targetDir.get().asFile.absolutePath)
          it.workingDir = rootDir.get().asFile
          // We check the exit value ourselves to provide a slightly better error message
          it.isIgnoreExitValue = true
        }
        .exitValue
    check(result == 0) { "$name Task failed" }
  }

  companion object {
    fun checkTask(project: Project) {
      val binary = project.findProperty("xyz.block.kotlin-formatter.binary") as? String
      project.tasks.register("checkFormatting", FormattingTask::class.java) {
        it.binary.set(binary ?: "bin/kotlin-format")
        it.rootDir.set(project.rootDir)
        it.targetDir.set(project.projectDir)
        it.args.set(listOf("--pre-push", "--dry-run", "--set-exit-if-changed"))
        it.description = "Checks if the committed code for the project is correctly formatted"
      }
    }

    fun applyTask(project: Project) {
      val binary = project.findProperty("xyz.block.kotlin-formatter.binary") as? String
      project.tasks.register("applyFormatting", FormattingTask::class.java) {
        it.binary.set(binary ?: "bin/kotlin-format")
        it.rootDir.set(project.rootDir)
        it.targetDir.set(project.projectDir)
        it.args.empty()
        it.description = "Reformats the working directory for the project to be correctly formatted"
      }
    }
  }
}
