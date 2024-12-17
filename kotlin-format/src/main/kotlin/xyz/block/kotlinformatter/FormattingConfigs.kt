package xyz.block.kotlinformatter

import java.io.File
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

/** Configuration for the formatter. */
internal data class FormattingConfigs private constructor(val formattables: List<Formattable>, val dryRun: Boolean) {
  companion object {
    fun forWorkingDir(paths: List<String>, dryRun: Boolean = false): FormattingConfigs {
      val files = expandFileNamesFromWorkingDir(paths)
      return FormattingConfigs(files, dryRun)
    }

    fun forPreCommit(paths: List<String>, dryRun: Boolean = false): FormattingConfigs {
      val files = expandFileNamesFromIndexDirs(paths)
      return FormattingConfigs(files, dryRun)
    }

    fun forPrePush(paths: List<String>, dryRun: Boolean = false, commitRef: String): FormattingConfigs {
      val files = expandFileNamesFromCommitted(commitRef, paths)
      return FormattingConfigs(files, dryRun)
    }

    fun forStdStreams(inputStream: InputStream, output: (String) -> Unit): FormattingConfigs {
      return FormattingConfigs(listOf(FormattableStdStreams(inputStream, output)), false)
    }

    private fun expandFileNamesFromIndexDirs(paths: List<String>): List<Formattable> {
      val rootPath = Paths.get(GitProcessRunner.run("rev-parse", "--show-toplevel").trim())

      var gitRelativePaths =
        paths.map {
          // Note that rootPath is the canonical path (because git always returns the canonicalized
          // path), so we need to ensure we canonicalize our given path before making it relative to
          // rootPath.
          File(System.getProperty("user.dir")).resolve(it).canonicalFile.toPath().relativeTo(rootPath)
        }
      if (gitRelativePaths.any { it.name == "" }) {
        // If any of the given paths turned out to be the same as the rootPath, then the
        // corresponding entry in gitRelativePaths will be the "empty path" (""),
        // which is detectable by checking the name is empty.
        // In that case, we will want to process all files in the repository, so we clear the list
        // of paths.
        gitRelativePaths = emptyList()
      }

      val stagedFiles =
        GitStagingService.getStagedFiles()
          .filter { path -> gitRelativePaths.isEmpty() || gitRelativePaths.any { path.startsWith(it) } }
          .toSet()

      val unstagedFiles =
        GitStagingService.getUnstagedFiles()
          .filter { path -> gitRelativePaths.isEmpty() || gitRelativePaths.any { path.startsWith(it) } }
          .toSet()

      val formattableBlobs = GitStagingService.getStagedFormattableBlobs(rootPath.toFile(), stagedFiles)
      val formattableFiles =
        (stagedFiles - unstagedFiles).map {
          val absolutePath = rootPath.resolve(it).normalize()
          FormattableFile(absolutePath.toFile(), rootPath)
        }

      return formattableBlobs + formattableFiles
    }

    private fun expandFileNamesFromCommitted(commitRef: String, paths: List<String>): List<Formattable> {
      val rootPath = Paths.get(GitProcessRunner.run("rev-parse", "--show-toplevel").trim())

      var gitRelativePaths =
        paths.map {
          // Note that rootPath is the canonical path (because git always returns the canonicalized
          // path), so we need to ensure we canonicalize our given path before making it relative to
          // rootPath.
          File(System.getProperty("user.dir")).resolve(it).canonicalFile.toPath().relativeTo(rootPath)
        }
      if (gitRelativePaths.any { it.name == "" }) {
        // If any of the given paths turned out to be the same as the rootPath, then the
        // corresponding entry in gitRelativePaths will be the "empty path" (""),
        // which is detectable by checking the name is empty.
        // In that case, we will want to process all files in the repository, so we clear the list
        // of paths.
        gitRelativePaths = emptyList()
      }

      val formattableBlobs =
        GitStagingService.getCommittedBlobs(rootPath.toFile(), commitRef).filter { blob ->
          gitRelativePaths.isEmpty() || gitRelativePaths.any { blob.path.startsWith(it) }
        }
      return formattableBlobs
    }

    @OptIn(ExperimentalPathApi::class)
    private fun expandFileNamesFromWorkingDir(args: List<String>): List<Formattable> {
      val result = mutableListOf<Formattable>()
      args
        // Filter out any arguments that don't exist on the file system, otherwise we get exceptions
        // during the tree walk later.
        .filter { Path(it).exists() }
        // Filter out any arguments that are already inside a gradle build dir. The visitor function
        // below will prevent traversing into gradle build directories, but doesn't catch the case
        // where we start the walk from inside the build directory, so this catches that case.
        .filterNot { Path(it).isInGradleBuildDir() }
        // Walk the file tree and add all .kt files to the result list. Skip subtrees that are
        // gradle build directories.
        .forEach { arg ->
          Path(arg).visitFileTree {
            onPreVisitDirectory { path, _ ->
              if (path.isGradleBuildDir()) {
                return@onPreVisitDirectory FileVisitResult.SKIP_SUBTREE
              }
              return@onPreVisitDirectory FileVisitResult.CONTINUE
            }
            onVisitFile { path, _ ->
              if (path.isKtFile()) {
                result.add(FormattableFile(path.toFile()))
              }
              return@onVisitFile FileVisitResult.CONTINUE
            }
          }
        }

      return result
    }

    private fun Path.isKtFile() = isRegularFile() && extension == "kt"

    /** Heuristic-based function to see if a given path is a gradle build directory. */
    private fun Path.isGradleBuildDir(): Boolean {
      return (name == "build" && isDirectory() && resolveSibling("build.gradle.kts").exists())
    }

    /** Recursive function to see if the given path or any of its parents are a gradle build directory. */
    private fun Path.isInGradleBuildDir(): Boolean {
      return isGradleBuildDir() || parent?.isInGradleBuildDir() == true
    }
  }
}
