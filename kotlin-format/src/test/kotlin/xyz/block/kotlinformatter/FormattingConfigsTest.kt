package xyz.block.kotlinformatter

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FormattingConfigsTest {
  @Test
  fun `convert list of filenames to files`(@TempDir tempDir: File) {
    // Create server directory and files
    val serverDir = tempDir.resolve("server").apply { mkdirs() }
    val serverExample1 = serverDir.resolve("Example1.kt").apply { writeText("", UTF_8) }
    val serverExample2 = serverDir.resolve("Example2.kt").apply { writeText("", UTF_8) }
    serverDir.resolve("Example3.kt").apply { writeText("", UTF_8) }
    serverDir.resolve("build.gradle.kts").apply { writeText("", UTF_8) }

    // Create build directory and file
    val serviceBuildDir = serverDir.resolve("build").apply { mkdirs() }
    val buildExample = serviceBuildDir.resolve("BuildExample.kt").apply { writeText("", UTF_8) }

    val configs =
      FormattingConfigs.forWorkingDir(
        listOf(serverExample1.toString(), serverExample2.toString(), buildExample.toString(), "other_file.kt")
      )

    // Exclude other_file.kt and BuildExample.kt in service/build directory
    assertThat(configs.formattables)
      .containsExactlyInAnyOrder(FormattableFile(serverExample1), FormattableFile(serverExample2))
  }

  @Test
  fun `expand directory to list of files`(@TempDir tempDir: File) {
    // Create server directory and files
    val serverDir = tempDir.resolve("server").apply { mkdirs() }
    val serverExample1 = serverDir.resolve("Example1.kt").apply { writeText("", UTF_8) }
    val serverExample2 = serverDir.resolve("Example2.kt").apply { writeText("", UTF_8) }

    serverDir.resolve("build.gradle.kts").apply { writeText("", UTF_8) }

    // Create build directory and file
    serverDir.resolve("build").apply { mkdirs() }.resolve("BuildExample.kt").writeText("", UTF_8)

    // Create client directory and files
    val clientDir = tempDir.resolve("client").apply { mkdirs() }
    clientDir.resolve("Example1.kt").apply { writeText("", UTF_8) }
    clientDir.resolve("Example2.kt").apply { writeText("", UTF_8) }

    val configs = FormattingConfigs.forWorkingDir(listOf(serverDir.toString()))

    // Exclude kt files in service/build directory and client directory
    assertThat(configs.formattables)
      .containsExactlyInAnyOrder(FormattableFile(serverExample1), FormattableFile(serverExample2))
  }

  @Test
  fun `include files in non Gradle build dir`(@TempDir tempDir: File) {
    // Create server directory and files
    val serverDir = tempDir.resolve("server").apply { mkdirs() }
    val serverExample1 = serverDir.resolve("Example1.kt").apply { writeText("", UTF_8) }

    // Create build directory and file but without build.gradle.kts
    val buildDir = serverDir.resolve("build").apply { mkdirs() }
    val buildExample = buildDir.resolve("BuildExample.kt").apply { writeText("", UTF_8) }

    val configs = FormattingConfigs.forWorkingDir(listOf(serverDir.toString()))
    assertThat(configs.formattables)
      .containsExactlyInAnyOrder(FormattableFile(serverExample1), FormattableFile(buildExample))
  }

  @Test
  fun `include all staged files when running in precommit mode`(@TempDir tempDir: File) {
    // Create server directory and files
    val serverDir = tempDir.resolve("server").apply { mkdirs() }
    val serverExample1 = serverDir.resolve("Example1.kt").apply { writeText("", UTF_8) }
    val serverExample2 = serverDir.resolve("Example2.kt").apply { writeText("", UTF_8) }
    serverDir.resolve("Example3.kt").apply { writeText("", UTF_8) }
    serverDir.resolve("build.gradle.kts").apply { writeText("", UTF_8) }

    // Create build directory and file
    val serviceBuildDir = serverDir.resolve("build").apply { mkdirs() }
    val buildExample = serviceBuildDir.resolve("BuildExample.kt").apply { writeText("", UTF_8) }

    // Note that we are staging a build file, it should be included in the formattables
    TestUtils.withWorkingDir(serverDir) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", serverExample1.toString(), serverExample2.toString(), buildExample.toString())

      val configs = FormattingConfigs.forPreCommit(listOf())
      assertThat(configs.formattables.filterIsInstance<FormattableBlob>())
        .containsExactlyInAnyOrder(
          TestFixtures.blob(serverExample1.relativeTo(serverDir).path),
          TestFixtures.blob(serverExample2.relativeTo(serverDir).path),
          TestFixtures.blob(buildExample.relativeTo(serverDir).path),
        )
    }
  }

  @Test
  fun `filter staged files by directory when running in precommit mode`(@TempDir tempDir: File) {
    // Create server directory and files
    val serverDir = tempDir.resolve("server").apply { mkdirs() }
    val subdir = serverDir.resolve("subdir").apply { mkdirs() }
    val serverExample1 = serverDir.resolve("Example1.kt").apply { writeText("", UTF_8) }
    val serverExample2 = subdir.resolve("Example2.kt").apply { writeText("", UTF_8) }
    serverDir.resolve("Example3.kt").apply { writeText("", UTF_8) }
    serverDir.resolve("build.gradle.kts").apply { writeText("", UTF_8) }

    // Create build directory and file
    val serviceBuildDir = serverDir.resolve("build").apply { mkdirs() }
    val buildExample = serviceBuildDir.resolve("BuildExample.kt").apply { writeText("", UTF_8) }

    // Note that we are staging a build file, it should be included in the formattables
    TestUtils.withWorkingDir(serverDir) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", serverExample1.toString(), serverExample2.toString(), buildExample.toString())

      val configs = FormattingConfigs.forPreCommit(listOf("subdir"))
      assertThat(configs.formattables.filterIsInstance<FormattableBlob>())
        .containsExactlyInAnyOrder(TestFixtures.blob(serverExample2.relativeTo(serverDir).path))
    }
  }

  @Test
  fun `filter staged files from a different directory in precommit mode`(@TempDir tempDir: File) {
    // Create server directory and files
    val serverDir = tempDir.resolve("server").apply { mkdirs() }
    val subdir = serverDir.resolve("some/subdir").apply { mkdirs() }
    val serverExample1 = serverDir.resolve("Example1.kt").apply { writeText("", UTF_8) }
    val serverExample2 = subdir.resolve("Example2.kt").apply { writeText("", UTF_8) }

    TestUtils.withWorkingDir(serverDir) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", serverExample1.toString(), serverExample2.toString())

      TestUtils.withWorkingDir(serverDir.resolve("some")) {
        val configs = FormattingConfigs.forPreCommit(listOf())
        assertThat(configs.formattables.filterIsInstance<FormattableBlob>())
          .containsExactlyInAnyOrder(
            TestFixtures.blob(serverExample1.relativeTo(serverDir).path),
            TestFixtures.blob(serverExample2.relativeTo(serverDir).path),
          )
      }
    }
  }

  @Test
  fun `filter staged files relative to current directory in precommit mode`(@TempDir tempDir: File) {
    // Create server directory and files
    val serverDir = tempDir.resolve("server").apply { mkdirs() }
    val subdir = serverDir.resolve("some/subdir").apply { mkdirs() }
    val serverExample1 = serverDir.resolve("Example1.kt").apply { writeText("", UTF_8) }
    val serverExample2 = subdir.resolve("Example2.kt").apply { writeText("", UTF_8) }

    TestUtils.withWorkingDir(serverDir) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", serverExample1.toString(), serverExample2.toString())

      TestUtils.withWorkingDir(serverDir.resolve("some")) {
        // This simulates running `format --pre-commit .` inside the `server/some` directory,
        // so only the Example2.kt file should be included in the formattables.
        val configs = FormattingConfigs.forPreCommit(listOf("."))
        assertThat(configs.formattables.filterIsInstance<FormattableBlob>())
          .containsExactlyInAnyOrder(TestFixtures.blob(serverExample2.relativeTo(serverDir).path))
      }
      TestUtils.withWorkingDir(serverDir.resolve("some")) {
        // This simulates running `format --pre-commit ..` inside the `server/some` directory,
        // so both files should be included in the formattables.
        val configs = FormattingConfigs.forPreCommit(listOf(".."))
        assertThat(configs.formattables.filterIsInstance<FormattableBlob>())
          .containsExactlyInAnyOrder(
            TestFixtures.blob(serverExample1.relativeTo(serverDir).path),
            TestFixtures.blob(serverExample2.relativeTo(serverDir).path),
          )
      }
    }
  }

  @Test
  fun `filter committed files relative to current directory in pre-push mode`(@TempDir tempDir: File) {
    // Create server directory and files
    val serverDir = tempDir.resolve("server").apply { mkdirs() }
    val subdir = serverDir.resolve("some/subdir").apply { mkdirs() }
    val serverExample1 = serverDir.resolve("Example1.kt").apply { writeText("", UTF_8) }
    val serverExample2 = subdir.resolve("Example2.kt").apply { writeText("", UTF_8) }
    val serverExample3 = subdir.resolve("Example3.kt").apply { writeText("", UTF_8) }
    subdir.resolve("Example4.kt").apply { writeText("", UTF_8) }

    TestUtils.withWorkingDir(serverDir) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", serverExample1.toString(), serverExample2.toString())
      TestUtils.setupGitUser() // needed for commit to work on CI
      GitProcessRunner.run("commit", "-m", "Initial commit")
      // Neither Example3.kt nor Example4.kt are committed, so they should not show up in the list
      // of pre-push formattables.
      GitProcessRunner.run("add", serverExample3.toString())

      TestUtils.withWorkingDir(serverDir.resolve("some")) {
        // This simulates running `format --pre-push .` inside the `server/some` directory,
        // so only the Example2.kt file should be included in the formattables.
        val configs = FormattingConfigs.forPrePush(listOf("."), dryRun = true, commitRef = "HEAD")
        assertThat(configs.formattables.filterIsInstance<FormattableBlob>())
          .containsExactlyInAnyOrder(TestFixtures.blob(serverExample2.relativeTo(serverDir).path))
      }
      TestUtils.withWorkingDir(serverDir.resolve("some")) {
        // This simulates running `format --pre-push ..` inside the `server/some` directory,
        // so both files should be included in the formattables.
        val configs = FormattingConfigs.forPrePush(listOf(".."), dryRun = true, commitRef = "HEAD")
        assertThat(configs.formattables.filterIsInstance<FormattableBlob>())
          .containsExactlyInAnyOrder(
            TestFixtures.blob(serverExample1.relativeTo(serverDir).path),
            TestFixtures.blob(serverExample2.relativeTo(serverDir).path),
          )
      }
    }
  }
}
