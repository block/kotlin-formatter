package xyz.block.kotlinformatter

import xyz.block.kotlinformatter.GitStagingService.getStagedFiles
import xyz.block.kotlinformatter.GitStagingService.getStagedFormattableBlobs
import java.io.File
import java.nio.file.Path

internal object TestFixtures {
  internal data class TestDirectoryFiles(
    val rootDir: File,
    val generatedWireSource: File,
    val gradleBuildFile: File,
    val mainAlreadyFormatted: File,
    val mainExample1: File,
    val testExample1Test: File,
    val libRandomFile: File,
    val libExample2: File,
    val libExample3: File,
    val edgecaseSpaces: File,
  )

  // Original File Content
  internal val gradleBuildFileContent =
    """
        plugins {
            kotlin("jvm") version "1.4.32"
        }

        repositories {
            mavenCentral()
        }

        dependencies {
            implementation(kotlin("stdlib"))
        }
    """
      .trimIndent()
  internal val generatedWireSourceContent = "fun buildExample()    =   println(\"Build Example\")\n"
  internal val mainAlreadyFormattedContent = "fun foo() = println(\"Hello World\")\n"
  internal val mainExample1Content = "fun  foo()=println(\"Example 1\")"
  internal val testExample1TestContent = "fun test() {     println(\"Example 1 Test\")}"
  internal val libRandomFileContent = "This is a text file."
  internal val libExample2Content = "fun bar()  =  println(\"Example 2\")"
  internal val libExample3Content = "fun bar()  =  println(\"Example 3\")"
  internal val edgecaseSpacesContent = "fun bar()  =  println(\"File with spaces\")"

  // Formatted File Content
  internal val formattedMainExample1Content = "fun foo() = println(\"Example 1\")\n"
  internal val formattedTestExample1TestContent = "fun test() {\n  println(\"Example 1 Test\")\n}\n"
  internal val formattedLibExample2Content = "fun bar() = println(\"Example 2\")\n"
  internal val formattedLibExample3Content = "fun bar() = println(\"Example 3\")\n"
  internal val formattedEdgecaseSpacesContent = "fun bar() = println(\"File with spaces\")\n"

  /**
   * Directory and File Structure:
   * ```
   * └── project/
   *     ├── .gitignore
   *     ├── build.gradle.kts
   *     ├── build/
   *     │   └── generated/
   *     │       └── source/
   *     │           └── wire/
   *     │               └── BuildExample.kt
   *     ├── src/
   *     │   ├── main/
   *     │   │   └── kotlin/
   *     │   │       ├── AlreadyFormatted.kt
   *     │   │       └── Example1.kt
   *     │   └── test/
   *     │       └── kotlin/
   *     │           └── Example1Test.kt
   *     ├── lib/
   *     │   ├── randomFile.txt
   *     │   ├── Example2.kt
   *     │   └── Example3.kt
   *     └── edgecases/
   *         └─── file with spaces.kt
   * ```
   */
  fun setupTestDirectory(root: File): TestDirectoryFiles {
    val projectDir = File(root, "project").apply { mkdirs() }

    // Add gitignore file
    File(projectDir, ".gitignore").apply {
      writeText(
        """
            # Ignore build directories
            /build/

            # Ignore IntelliJ IDEA project files
            .idea/
            *.iml

            # Ignore log files
            *.log
        """
          .trimIndent()
      )
    }

    val gradleBuildFile = File(projectDir, "build.gradle.kts").apply { writeText(gradleBuildFileContent) }

    // Create build/generated/source/wire directory and add Kotlin file
    val buildWireDir = File(projectDir, "build/generated/source/wire").apply { mkdirs() }
    val generatedWireSource = File(buildWireDir, "BuildExample.kt").apply { writeText(generatedWireSourceContent) }

    val srcMainKotlin = File(root, "project/src/main/kotlin").apply { mkdirs() }
    val srcTestKotlin = File(root, "project/src/test/kotlin").apply { mkdirs() }
    val lib = File(root, "project/lib").apply { mkdirs() }
    val edgecases = File(root, "project/edgecases").apply { mkdirs() }

    val mainAlreadyFormattedFile =
      File(srcMainKotlin, "AlreadyFormatted.kt").apply { writeText(mainAlreadyFormattedContent) }
    val mainExample1 = File(srcMainKotlin, "Example1.kt").apply { writeText(mainExample1Content) }
    val testExample1Test = File(srcTestKotlin, "Example1Test.kt").apply { writeText(testExample1TestContent) }
    val libRandomFile = File(lib, "RandomFile.txt").apply { writeText(libRandomFileContent) }
    val libExample2 = File(lib, "Example2.kt").apply { writeText(libExample2Content) }
    val libExample3 = File(lib, "Example3.kt").apply { writeText(libExample3Content) }
    val edgecaseSpaces = File(edgecases, "file with spaces.kt").apply { writeText(edgecaseSpacesContent) }

    return TestDirectoryFiles(
      rootDir = projectDir,
      generatedWireSource = generatedWireSource,
      gradleBuildFile = gradleBuildFile,
      mainAlreadyFormatted = mainAlreadyFormattedFile,
      mainExample1 = mainExample1,
      testExample1Test = testExample1Test,
      libRandomFile = libRandomFile,
      libExample2 = libExample2,
      libExample3 = libExample3,
      edgecaseSpaces = edgecaseSpaces,
    )
  }

  /**
   * Compiles a map of file paths to their content for all staged files.
   *
   * @return A map of file paths as strings to their content.
   */
  fun getStagedContent(gitRoot: File): Map<String, String> {
    return getStagedFormattableBlobs(gitRoot, getStagedFiles()).associate { it.name() to it.read() }
  }

  fun blob(path: String) = FormattableBlob(Path.of(path), "100644", "somehash")
}
