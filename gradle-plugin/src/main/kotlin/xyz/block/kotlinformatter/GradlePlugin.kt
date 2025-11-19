package xyz.block.kotlinformatter

import org.gradle.api.Plugin
import org.gradle.api.Project

class GradlePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    FormattingTask.checkTask(target)
    FormattingTask.applyTask(target)
  }
}
