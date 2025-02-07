package xyz.block.kotlinformatter.idea

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import xyz.block.kotlinformatter.idea.FormatOnSavePostStartupActivity.Companion
import java.io.InputStreamReader
import java.util.Properties

@Service(Service.Level.PROJECT)
class FormatConfigurationService(private val project: Project) {

  // This is calculated once and on demand.
  // Changes to the config file will require a restart.
  private val config: Properties by lazy {
    val configFile = project.getFile(CONFIG_FILE_PATH)
    val properties = Properties()
    configFile?.let { file ->
      InputStreamReader(file.inputStream).use {
        properties.load(it)
      }
    }
    properties
  }

  val formattingEnabled: Boolean by lazy {
    val enabled = config.getProperty(ENABLED_PROPERTY_NAME)?.toBoolean() ?: false
    logger.info("Formatting enabled: $enabled")
    enabled
  }

  val scriptPath: String? by lazy {
    val path = config.getProperty(SCRIPT_PATH_PROPERTY_NAME)
    logger.info("Formatting script: $path")
    path
  }

  companion object {
    private const val CONFIG_FILE_PATH: String = ".idea/kotlin-formater.properties"
    private const val ENABLED_PROPERTY_NAME: String = "kotlin-formatter.enabled"
    private const val SCRIPT_PATH_PROPERTY_NAME: String = "kotlin-formatter.script-path"
    private val logger = Logger.getInstance(FormatConfigurationService::class.java.name)
  }
}