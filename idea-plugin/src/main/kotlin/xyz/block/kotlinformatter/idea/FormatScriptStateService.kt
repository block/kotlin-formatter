package xyz.block.kotlinformatter.idea

import com.intellij.openapi.project.Project

interface FormatScriptStateService {
  val path: String

  //fun locateScriptFile()
}

//@Service(Service.Level.PROJECT)
//@State(name = "formatScriptPath", storages = [(Storage("kotlin-formatter.xml"))])
class DefaultFormatScriptStateService(val project: Project): FormatScriptStateService {
  //: SimplePersistentStateComponent<PathState>(PathState()) {

  override var path: String = "bin/format"

  //override fun locateScriptFile() {
    // var pluginVersion = getPluginVersion()
    // if (pluginVersion == null || pluginVersion.endsWith("-SNAPSHOT")) {
    //   pluginVersion = "1.0.2"
    // }
    //
    // val unzipDir = project.basePath!!.toPath().resolve("build").resolve("kotlin-formatter-binary").toFile().also {
    //   if (!it.exists()) {
    //     it.mkdirs()
    //   }
    // }
    //
    // val releaseUrl = "https://github.com/block/kotlin-formatter/releases/download/$pluginVersion/kotlin-formatter-dist-$pluginVersion.zip"
    // logger.info("Downloading Kotlin formatter binary from $releaseUrl")
    // val zipFile = File(project.basePath, "kotlin-formatter-dist.zip")
    // downloadFile(releaseUrl, zipFile)
    // unzip(zipFile, unzipDir)
    //
    // //state.path = unzipDir.toPath().resolve("bin").resolve("kotlin-format").toString()
    // path = unzipDir.toPath().resolve("bin").resolve("kotlin-format").toString()
  //}

  // class PathState : BaseState() {
  //   var path: String? = null
  // }

  //companion object {
    // private val logger = Logger.getInstance(FormatScriptStateService::class.java.name)
    //
    // private fun downloadFile(fileURL: String, outputFile: File) {
    //   URI(fileURL).toURL().openStream().use { input ->
    //     FileOutputStream(outputFile).use { output ->
    //       input.copyTo(output)
    //     }
    //   }
    // }
    //
    // private fun unzip(zipFile: File, outputFolder: File) {
    //   if (!outputFolder.exists()) {
    //     outputFolder.mkdirs()
    //   }
    //
    //   ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
    //     var entry: ZipEntry?
    //
    //     while (zipInputStream.nextEntry.also { entry = it } != null) {
    //       val newFile = File(outputFolder, entry!!.name)
    //       if (entry!!.isDirectory) {
    //         newFile.mkdirs()
    //       } else {
    //         File(newFile.parent).mkdirs()
    //         FileOutputStream(newFile).use { output ->
    //           zipInputStream.copyTo(output)
    //         }
    //       }
    //     }
    //   }
    // }
    //
    // private fun getPluginVersion(): String? {
    //   val pluginId = PluginId.getId("xyz.block.kotlin-formatter")
    //   val descriptor = PluginManager.getInstance().findEnabledPlugin(pluginId)
    //   return descriptor?.version
    // }
  //}
}