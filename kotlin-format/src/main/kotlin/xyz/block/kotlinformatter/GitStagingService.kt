package xyz.block.kotlinformatter

import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension

/** Provides services related to managing and querying the staging area in Git. */
internal object GitStagingService {
  fun getUnstagedFiles(): Set<Path> {
    return GitProcessRunner.run("diff", "--name-only")
      .lines()
      .map { line -> Path(line) }
      .filter { path -> path.extension == "kt" }
      .toSet()
  }

  fun getStagedFiles(): Set<Path> {
    // the --diff-filter=d filters out deleted files.
    return GitProcessRunner.run("diff", "--name-only", "--cached", "--diff-filter=d")
      .lines()
      .map { line -> Path(line) }
      .filter { path -> path.extension == "kt" }
      .toSet()
  }

  /** Retrieves a list of [FormattableBlob]s for files that are staged without any merge conflicts. */
  fun getStagedFormattableBlobs(gitRoot: File, stagedPaths: Set<Path>): List<FormattableBlob> {
    val whitespace = Regex("\\s+")
    return GitProcessRunner.run("ls-files", "--stage", workingDir = gitRoot).trim().lines().mapNotNull { line ->
      val tokens = line.split(whitespace, limit = 4)
      val mode = tokens[0]
      val hash = tokens[1]
      val stageNumber = tokens[2]
      val path = Path(tokens[3])

      // Ignore files in a merge conflict (stageNumber != "0")
      if (path in stagedPaths && stageNumber == "0") {
        FormattableBlob(path, mode, hash)
      } else {
        null
      }
    }
  }

  fun getCommittedBlobs(gitRoot: File, commitRef: String): List<FormattableBlob> {
    val whitespace = Regex("\\s+")
    return GitProcessRunner.run("ls-tree", "-r", commitRef, workingDir = gitRoot).trim().lines().mapNotNull { line ->
      val tokens = line.split(whitespace, limit = 4)
      val mode = tokens[0]
      val type = tokens[1]
      val hash = tokens[2]
      val path = Path(tokens[3])

      // Ignore anything that's not a kotlin file
      if (type == "blob" && path.extension == "kt") {
        FormattableBlob(path, mode, hash)
      } else {
        null
      }
    }
  }
}
