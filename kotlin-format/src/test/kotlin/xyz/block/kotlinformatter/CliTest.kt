package xyz.block.kotlinformatter

import com.github.ajalt.clikt.testing.test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Paths
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CliTest {
  @Test
  fun `format by kt filenames`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    val result =
      Cli()
        .test(
          "${testDir.mainExample1} ${testDir.libExample3} ${testDir.libRandomFile} ${testDir.gradleBuildFile} ${testDir.generatedWireSource} non-existing-file.kt"
        )

    // libRandomFile should not be part of formatted files
    val formattedFiles = listOf(testDir.mainExample1, testDir.libExample3)
    assertThat(result.stdout.trimEnd().lines())
      .containsExactlyInAnyOrderElementsOf(formattedFiles.map { "✅ Formatted $it" })

    // Check files are formatted
    assertThat(testDir.mainExample1.readText()).isEqualTo(TestFixtures.formattedMainExample1Content)
    assertThat(testDir.libExample3.readText()).isEqualTo(TestFixtures.formattedLibExample3Content)

    // Check other files are the same
    assertThat(testDir.generatedWireSource.readText()).isEqualTo(TestFixtures.generatedWireSourceContent)
    assertThat(testDir.gradleBuildFile.readText()).isEqualTo(TestFixtures.gradleBuildFileContent)
    assertThat(testDir.mainAlreadyFormatted.readText()).isEqualTo(TestFixtures.mainAlreadyFormattedContent)
    assertThat(testDir.testExample1Test.readText()).isEqualTo(TestFixtures.testExample1TestContent)
    assertThat(testDir.libRandomFile.readText()).isEqualTo(TestFixtures.libRandomFileContent)
    assertThat(testDir.libExample2.readText()).isEqualTo(TestFixtures.libExample2Content)

    assertThat(result.statusCode).isEqualTo(0)
  }

  @Test
  fun `format all kt files in directories`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    val mainSrcDirectory = Paths.get(tempDir.path, "project", "src")
    val libDirectory = Paths.get(tempDir.path, "project", "lib")
    val result = Cli().test("${mainSrcDirectory.toAbsolutePath()} ${libDirectory.toAbsolutePath()}")

    // generatedWireSource, mainAlreadyFormattedFile and libRandomFile should not be part of
    // formatted files
    val formattedFiles =
      listOf(testDir.mainExample1, testDir.testExample1Test, testDir.libExample2, testDir.libExample3)
    assertThat(result.stdout.trimEnd().lines())
      .containsExactlyInAnyOrderElementsOf(formattedFiles.map { "✅ Formatted $it" })

    // Check files are formatted
    assertThat(testDir.mainExample1.readText()).isEqualTo(TestFixtures.formattedMainExample1Content)
    assertThat(testDir.testExample1Test.readText()).isEqualTo(TestFixtures.formattedTestExample1TestContent)
    assertThat(testDir.libExample2.readText()).isEqualTo(TestFixtures.formattedLibExample2Content)
    assertThat(testDir.libExample3.readText()).isEqualTo(TestFixtures.formattedLibExample3Content)

    // Check other files are the same
    assertThat(testDir.generatedWireSource.readText()).isEqualTo(TestFixtures.generatedWireSourceContent)
    assertThat(testDir.mainAlreadyFormatted.readText()).isEqualTo(TestFixtures.mainAlreadyFormattedContent)
    assertThat(testDir.libRandomFile.readText()).isEqualTo(TestFixtures.libRandomFileContent)

    assertThat(result.statusCode).isEqualTo(0)
  }

  @Test
  fun `format by kt filenames and directories`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    val libDirectory = Paths.get(tempDir.path, "project", "lib")
    val result = Cli().test("${testDir.mainExample1} ${libDirectory.toAbsolutePath()}")

    // generatedWireSource, mainAlreadyFormattedFile, testExample1Test and libRandomFile should not
    // be part of formatted files
    val formattedFiles = listOf(testDir.mainExample1, testDir.libExample2, testDir.libExample3)
    assertThat(result.stdout.trimEnd().lines())
      .containsExactlyInAnyOrderElementsOf(formattedFiles.map { "✅ Formatted $it" })

    // Check files are formatted
    assertThat(testDir.mainExample1.readText()).isEqualTo(TestFixtures.formattedMainExample1Content)
    assertThat(testDir.libExample2.readText()).isEqualTo(TestFixtures.formattedLibExample2Content)
    assertThat(testDir.libExample3.readText()).isEqualTo(TestFixtures.formattedLibExample3Content)

    // Check other files are the same
    assertThat(testDir.generatedWireSource.readText()).isEqualTo(TestFixtures.generatedWireSourceContent)
    assertThat(testDir.gradleBuildFile.readText()).isEqualTo(TestFixtures.gradleBuildFileContent)
    assertThat(testDir.testExample1Test.readText()).isEqualTo(TestFixtures.testExample1TestContent)
    assertThat(testDir.mainAlreadyFormatted.readText()).isEqualTo(TestFixtures.mainAlreadyFormattedContent)
    assertThat(testDir.libRandomFile.readText()).isEqualTo(TestFixtures.libRandomFileContent)

    assertThat(result.statusCode).isEqualTo(0)
  }

  @Test
  fun `format input from stdin and write to stdout`() {
    val inputContent = TestFixtures.testExample1TestContent
    val inputStream = ByteArrayInputStream(inputContent.toByteArray(Charsets.UTF_8))

    val result = Cli(inputStream).test("--set-exit-if-changed -")

    assertThat(result.stdout).contains(TestFixtures.formattedTestExample1TestContent)
    assertThat(result.statusCode).isEqualTo(3)
  }

  @Test
  fun `return nothing if input from stdin is already formatted`() {
    val inputContent = TestFixtures.formattedTestExample1TestContent
    val inputStream = ByteArrayInputStream(inputContent.toByteArray(Charsets.UTF_8))

    val result = Cli(inputStream).test("--set-exit-if-changed -")

    assertThat(result.stdout).isEmpty()
    assertThat(result.statusCode).isEqualTo(0)
  }

  @Test
  fun `with --dry-run - doesn't format files`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    val result = Cli().test("--dry-run ${testDir.mainExample1}")

    assertThat(result.stdout.trimEnd().lines())
      .containsExactlyInAnyOrderElementsOf(listOf("🛠️ Would format ${testDir.mainExample1}"))

    // Check all files are not formatted
    assertThat(testDir.generatedWireSource.readText()).isEqualTo(TestFixtures.generatedWireSourceContent)
    assertThat(testDir.gradleBuildFile.readText()).isEqualTo(TestFixtures.gradleBuildFileContent)
    assertThat(testDir.mainExample1.readText()).isEqualTo(TestFixtures.mainExample1Content)
    assertThat(testDir.mainAlreadyFormatted.readText()).isEqualTo(TestFixtures.mainAlreadyFormattedContent)
    assertThat(testDir.testExample1Test.readText()).isEqualTo(TestFixtures.testExample1TestContent)
    assertThat(testDir.libRandomFile.readText()).isEqualTo(TestFixtures.libRandomFileContent)
    assertThat(testDir.libExample2.readText()).isEqualTo(TestFixtures.libExample2Content)
    assertThat(testDir.libExample3.readText()).isEqualTo(TestFixtures.libExample3Content)

    assertThat(result.statusCode).isEqualTo(0)
  }

  @Test
  fun `with --pre-commit - format partially staged file and fully staged file`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    TestUtils.withWorkingDir(testDir.rootDir) {
      // 1. Set up Git
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", testDir.mainExample1.path)
      GitProcessRunner.run("add", testDir.libExample2.path)
      GitProcessRunner.run("add", testDir.libExample3.path)

      // And simulate libExample2 (partially staged) and libExample3 (fully staged)
      val libExample2UpdatedContent = TestFixtures.libExample2Content + "\nfun baz()  =  println(\"Example 2 Baz\")"
      testDir.libExample2.writeText(libExample2UpdatedContent)

      // 2. Format lib with --pre-commit flag
      val result = Cli().test("--pre-commit lib")

      val libExample2Relative = testDir.libExample2.relativeTo(testDir.rootDir).path
      val libExample3Relative = testDir.libExample3.relativeTo(testDir.rootDir).path
      val mainExample1Relative = testDir.mainExample1.relativeTo(testDir.rootDir).path
      val formattedFiles = listOf(libExample2Relative, libExample3Relative)

      // 3. Verify that we only format libExample2 and libExample3
      assertThat(result.stdout.trimEnd().lines())
        .containsExactlyInAnyOrderElementsOf(formattedFiles.map { "✅ Formatted $it" })

      assertThat(TestFixtures.getStagedContent(testDir.rootDir))
        .hasSize(3)
        .containsEntry(mainExample1Relative, TestFixtures.mainExample1Content) // Not changed
        .containsEntry(libExample2Relative, TestFixtures.formattedLibExample2Content) // Change staged
        .containsEntry(libExample3Relative, TestFixtures.formattedLibExample3Content) // Change staged

      assertThat(testDir.libExample3.readText()).isEqualTo(TestFixtures.formattedLibExample3Content) // Change written
      assertThat(testDir.libExample2.readText()).isEqualTo(libExample2UpdatedContent) // Change not written

      // Other files should be formatted
      assertThat(testDir.mainExample1.readText()).isEqualTo(TestFixtures.mainExample1Content)
      assertThat(testDir.generatedWireSource.readText()).isEqualTo(TestFixtures.generatedWireSourceContent)
      assertThat(testDir.gradleBuildFile.readText()).isEqualTo(TestFixtures.gradleBuildFileContent)
      assertThat(testDir.mainAlreadyFormatted.readText()).isEqualTo(TestFixtures.mainAlreadyFormattedContent)
      assertThat(testDir.testExample1Test.readText()).isEqualTo(TestFixtures.testExample1TestContent)
      assertThat(testDir.libRandomFile.readText()).isEqualTo(TestFixtures.libRandomFileContent)

      assertThat(result.statusCode).isEqualTo(0)
    }
  }

  @Test
  fun `exits with a non-zero exit code if any files needed to be changed`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    val result = Cli().test("--set-exit-if-changed ${testDir.mainExample1}")

    assertThat(result.stdout.trimEnd().lines())
      .containsExactlyInAnyOrderElementsOf(listOf("✅ Formatted ${testDir.mainExample1}"))

    // Check files are formatted
    assertThat(testDir.mainExample1.readText()).isEqualTo(TestFixtures.formattedMainExample1Content)

    // Check other files are not formatted
    assertThat(testDir.generatedWireSource.readText()).isEqualTo(TestFixtures.generatedWireSourceContent)
    assertThat(testDir.gradleBuildFile.readText()).isEqualTo(TestFixtures.gradleBuildFileContent)
    assertThat(testDir.mainAlreadyFormatted.readText()).isEqualTo(TestFixtures.mainAlreadyFormattedContent)
    assertThat(testDir.testExample1Test.readText()).isEqualTo(TestFixtures.testExample1TestContent)
    assertThat(testDir.libRandomFile.readText()).isEqualTo(TestFixtures.libRandomFileContent)
    assertThat(testDir.libExample2.readText()).isEqualTo(TestFixtures.libExample2Content)
    assertThat(testDir.libExample3.readText()).isEqualTo(TestFixtures.libExample3Content)
    assertThat(result.statusCode).isEqualTo(3)
  }

  @Test
  fun `successfully exits if no files to format`(@TempDir tempDir: File) {
    TestFixtures.setupTestDirectory(tempDir)
    val result = Cli().test("non-existing-file.kt")

    assertThat(result.stdout.trimEnd()).isEqualTo("Nothing to format")
    assertThat(result.statusCode).isEqualTo(0)
  }

  @Test
  fun `returns help message if no filenames are specified`(@TempDir tempDir: File) {
    TestFixtures.setupTestDirectory(tempDir)
    val result = Cli().test("")

    assertThat(result.stdout.trimEnd()).contains(Cli.HELP_MESSAGE)
    assertThat(result.statusCode).isEqualTo(0)
  }

  @Test
  fun `exits with a non-zero exit code on pre-push for unformatted committed files`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    TestUtils.withWorkingDir(testDir.rootDir) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", ".")
      TestUtils.setupGitUser() // needed for commit to work on CI
      GitProcessRunner.run("commit", "-m", "Initial commit")

      val result = Cli().test("--pre-push --dry-run --set-exit-if-changed src")

      assertThat(result.statusCode).isNotEqualTo(0)
      assertThat(result.stdout.trimEnd().lines())
        .containsExactlyInAnyOrderElementsOf(
          listOf(
            "🛠️ Would format ${testDir.mainExample1.relativeTo(testDir.rootDir)}",
            "🛠️ Would format ${testDir.testExample1Test.relativeTo(testDir.rootDir)}",
            "⚠️ The committed files have formatting errors. Please format the files and commit the formatting changes.",
          )
        )

      // Check other files are not formatted
      assertThat(testDir.generatedWireSource.readText()).isEqualTo(TestFixtures.generatedWireSourceContent)
      assertThat(testDir.gradleBuildFile.readText()).isEqualTo(TestFixtures.gradleBuildFileContent)
      assertThat(testDir.mainAlreadyFormatted.readText()).isEqualTo(TestFixtures.mainAlreadyFormattedContent)
      assertThat(testDir.testExample1Test.readText()).isEqualTo(TestFixtures.testExample1TestContent)
      assertThat(testDir.libRandomFile.readText()).isEqualTo(TestFixtures.libRandomFileContent)
      assertThat(testDir.libExample2.readText()).isEqualTo(TestFixtures.libExample2Content)
      assertThat(testDir.libExample3.readText()).isEqualTo(TestFixtures.libExample3Content)
    }
  }

  @Test
  fun `exercise scenario with file renaming`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    TestUtils.withWorkingDir(testDir.rootDir) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", ".")
      TestUtils.setupGitUser() // needed for commit to work on CI
      GitProcessRunner.run("commit", "-m", "Initial commit")

      TestUtils.addPrecommitHook(testDir.rootDir)
      GitProcessRunner.run("mv", "lib/Example2.kt", "lib/Example2Renamed.kt")
      val proc = GitProcessRunner.start("commit", "-m", "File move")
      // The pre-commit hook output goes to git's stderr
      val stderr = proc.errorStream.bufferedReader().use { it.readText() }
      assertThat(proc.waitFor()).isEqualTo(0)
      assertThat(stderr.trimEnd().lines()).containsExactly("✅ Formatted lib/Example2Renamed.kt")
    }
  }

  @Test
  fun `exercise scenario with file deletion and addition`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    TestUtils.withWorkingDir(testDir.rootDir) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", ".")
      TestUtils.setupGitUser() // needed for commit to work on CI
      GitProcessRunner.run("commit", "-m", "Initial commit")

      TestUtils.addPrecommitHook(testDir.rootDir)
      testDir.rootDir.resolve("lib/Example4.kt").writeText("fun foo()  = println(\"Example 4\")")
      GitProcessRunner.run("rm", "lib/Example2.kt")
      GitProcessRunner.run("add", "lib/Example4.kt")
      val proc = GitProcessRunner.start("commit", "-m", "File swap")
      // The pre-commit hook output goes to git's stderr
      val stderr = proc.errorStream.bufferedReader().use { it.readText() }

      assertThat(proc.waitFor()).isEqualTo(0)
      assertThat(stderr.trimEnd().lines()).containsExactly("✅ Formatted lib/Example4.kt")
    }
  }

  @Test
  fun `with --pre-commit - handle edge cases`(@TempDir tempDir: File) {
    val testDir = TestFixtures.setupTestDirectory(tempDir)
    TestUtils.withWorkingDir(testDir.rootDir) {
      // 1. Set up Git
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", testDir.edgecaseSpaces.path)

      // 2. Format lib with --pre-commit flag
      val result = Cli().test("--pre-commit .")

      val edgecaseSpacesRelative = testDir.edgecaseSpaces.relativeTo(testDir.rootDir).path
      val formattedFiles = listOf(edgecaseSpacesRelative)

      // 3. Verify that we only format libExample2 and libExample3
      assertThat(result.stdout.trimEnd().lines())
        .containsExactlyInAnyOrderElementsOf(formattedFiles.map { "✅ Formatted $it" })

      assertThat(TestFixtures.getStagedContent(testDir.rootDir))
        .hasSize(1)
        .containsEntry(edgecaseSpacesRelative, TestFixtures.formattedEdgecaseSpacesContent) // Change staged

      assertThat(testDir.edgecaseSpaces.readText())
        .isEqualTo(TestFixtures.formattedEdgecaseSpacesContent) // Change written

      assertThat(result.statusCode).isEqualTo(0)
    }
  }
}
