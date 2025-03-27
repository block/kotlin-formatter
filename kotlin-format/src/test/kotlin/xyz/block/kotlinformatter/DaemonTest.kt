package xyz.block.kotlinformatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.Socket
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.concurrent.thread

class TestSystemExiter : SystemExiter {
  var exitCalled = false
  var lastExitStatus = -1

  override fun exit(status: Int) {
    exitCalled = true
    lastExitStatus = status
  }
}

class DaemonTest {
  private lateinit var daemon: Daemon
  private lateinit var daemonThread: Thread
  private lateinit var testExiter: TestSystemExiter

  @TempDir
  lateinit var tempDir: Path

  @BeforeEach
  fun setup() {
    testExiter = TestSystemExiter()
    daemon = Daemon(
      systemExiter = testExiter,
      idleTimeout = IDLE_TIMEOUT,
      maxRuntime = MAX_RUNTIME,
      timeoutCheckerInterval = TIMEOUT_CHECKER_INTERVAL,
      workingDir = tempDir
    )

    // Start daemon in a separate thread
    daemonThread = thread(start = true) {
      try {
        daemon.start()
      } catch (e: Exception) {
        // Ignore exceptions from normal shutdown
        if (!e.message?.contains("Socket closed")!!) {
          throw e
        }
      }
    }

    // Wait for daemon to start (max 1 second)
    var attempts = 0
    while (!daemon.isRunning() && attempts < 20) {
      Thread.sleep(50)
      attempts++
    }

    assertThat(daemon.isRunning()).isTrue()
  }

  @AfterEach
  fun tearDown() {
    daemon.stop()
    daemonThread.join(1000) // Wait up to 1 second for thread to finish
  }

  @Test
  fun `starts and accepts connections`() {
    val lockFile = tempDir.resolve(Daemon.LOCK_FILE_PATH).toFile()
    assertThat(lockFile.exists()).isTrue()

    val daemonData = daemon.readLockFile()
    assertThat(daemonData).isNotNull

    // Try to connect to the daemon
    Socket("localhost", daemonData!!.port).use { socket ->
      socket.getOutputStream().write("status\n".toByteArray())
      val response = socket.getInputStream().bufferedReader().readText()
      assertThat(response).contains("Daemon Status")
      assertThat(response).contains("Time until idle timeout: 2s")
      assertThat(response).contains("Time until max runtime timeout: 5s")
    }
  }

  @Test
  fun `only one daemon runs at a time`() {
    // Try to start another daemon
    val secondTestExiter = TestSystemExiter()
    val secondDaemon = Daemon(
      systemExiter = secondTestExiter,
      workingDir = tempDir
    )
    var secondDaemonStarted = false

    thread {
      try {
        secondDaemon.start()
        secondDaemonStarted = true
      } catch (e: Exception) {
        // Expected
      }
    }

    // Wait a bit and verify the second daemon didn't start
    Thread.sleep(1000)
    assertThat(secondDaemonStarted).isFalse()
  }

  @Test
  fun `format command with file arguments`() {
    val testFile = File(tempDir.toFile(), "FormatTest.kt")
    val unformatted = "fun  foo()=println(\"Hello\")"
    val expectedFormatted = "fun foo() = println(\"Hello\")\n"
    testFile.writeText(unformatted)

    val daemonData = daemon.readLockFile()
    assertThat(daemonData).isNotNull

    // Send format command
    Socket("localhost", daemonData!!.port).use { socket ->
      socket.getOutputStream().write("format ${testFile.absolutePath}\n".toByteArray())
      val reader = socket.getInputStream().bufferedReader()
      val exitCode = reader.readLine()
      assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.toString())
      val output = reader.readText()
      assertThat(output).contains("✅ Formatted")
    }

    // Verify file content
    val formattedContent = testFile.readText()
    assertThat(formattedContent).isEqualTo(expectedFormatted)
  }

  @Test
  fun `format command with 'stdin' mode`() {
    val unformatted = "fun  foo()=println(\"Hello\")"
    val expectedFormatted = "fun foo() = println(\"Hello\")\n"

    val daemonData = daemon.readLockFile()
    assertThat(daemonData).isNotNull

    // Send format -
    Socket("localhost", daemonData!!.port).use { socket ->
      val outputStream = socket.getOutputStream()
      outputStream.write("format -\n".toByteArray())
      outputStream.write(unformatted.toByteArray())
      outputStream.write("\n\u0004\n".toByteArray())

      val reader = socket.getInputStream().bufferedReader()
      val exitCode = reader.readLine()
      assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.toString())
      val output = reader.readText()
      assertThat(output).isEqualTo(expectedFormatted)
    }
  }

  @Test
  fun `handles pre-commit command`() {
    val testFile = File(tempDir.toFile(), "Test.kt")
    testFile.writeText("""
        fun main() {
        
        
              println("Hello")
        }
    """.trimIndent())
    TestUtils.withWorkingDir(tempDir.toFile()) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", ".")
    }

    val daemonData = daemon.readLockFile()
    assertThat(daemonData).isNotNull

    // Send pre-commit command
    Socket("localhost", daemonData!!.port).use { socket ->
      socket.getOutputStream().write("pre-commit ${testFile.absolutePath}\n".toByteArray())
      val reader = socket.getInputStream().bufferedReader()
      val exitCode = reader.readLine()
      assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.toString())
      val output = reader.readText()
      assertThat(output).isEqualTo("✅ Formatted Test.kt\n")
    }
  }

  @Test
  fun `handles pre-push command`() {
    // Create a test Kotlin file
    val testFile = File(tempDir.toFile(), "Test.kt")
    testFile.writeText("""
        fun main() {
        
        
              println("Hello")
        }
    """.trimIndent())
    var pushHash = ""
    TestUtils.withWorkingDir(tempDir.toFile()) {
      GitProcessRunner.run("init")
      GitProcessRunner.run("add", ".")
      TestUtils.setupGitUser() // needed for commit to work on CI
      GitProcessRunner.run("commit", "-m", "Initial commit")
      pushHash =
        GitProcessRunner.run("log", "-1", "--format=%H").trim()
    }

    val daemonData = daemon.readLockFile()
    assertThat(daemonData).isNotNull

    // Send pre-push command
    Socket("localhost", daemonData!!.port).use { socket ->
      socket.getOutputStream().write("pre-push $pushHash ${testFile.absolutePath}\n".toByteArray())
      val reader = socket.getInputStream().bufferedReader()
      val exitCode = reader.readLine()
      assertThat(exitCode).isEqualTo(ExitCode.FILE_CHANGED.toString())
      val output = reader.readText()
      assertThat(output).isEqualTo(
        """
        🛠️ Would format Test.kt
        ⚠️ The committed files have formatting errors. Please format the files and commit the formatting changes.
        
        """.trimIndent())
    }
  }

  @Test
  fun `daemon shuts down after idle timeout`() {
    var timeoutOccurred = false
    val startTime = Instant.now()

    while (Duration.between(startTime, Instant.now()).seconds < IDLE_TIMEOUT.seconds + 1) {
      if (testExiter.exitCalled) {
        timeoutOccurred = true
        break
      }
      Thread.sleep(100)
    }

    assertThat(timeoutOccurred).isTrue()
    assertThat(testExiter.lastExitStatus).isEqualTo(0)
    // daemon exited because of idle timeout, not max runtime timeout
    assertThat(Duration.between(startTime, Instant.now()).seconds).isLessThan(MAX_RUNTIME.seconds)
  }

  @Test
  fun `daemon shuts down after max runtime timeout`() {
    val daemonData = daemon.readLockFile()
    assertThat(daemonData).isNotNull

    var timeoutOccurred = false
    val startTime = Instant.now()

    while (Duration.between(startTime, Instant.now()).seconds < MAX_RUNTIME.seconds + 1) {
      if (testExiter.exitCalled) {
        timeoutOccurred = true
        break
      }

      // Only send a command if we're not close to the max runtime timeout, otherwise we risk
      // sending the message to a closed socket and causing an exception
      if (Duration.between(startTime, Instant.now()).seconds < MAX_RUNTIME.seconds - IDLE_TIMEOUT.seconds) {
        // Postpone idle timeout so we hit the max runtime timeout instead
        Socket("localhost", daemonData!!.port).use { socket ->
          socket.getOutputStream().write("pre-commit foo.kt\n".toByteArray())
          socket.getInputStream().bufferedReader().readText()
        }
      }
      Thread.sleep(100)
    }

    assertThat(timeoutOccurred).isTrue()
    assertThat(testExiter.lastExitStatus).isEqualTo(0)
    // daemon exited because of max runtime timeout, not idle timeout
    assertThat(Duration.between(startTime, Instant.now()).seconds).isGreaterThan(IDLE_TIMEOUT.seconds)
  }

  @Test
  fun `cleans up on stop`() {
    val lockFile = tempDir.resolve(Daemon.LOCK_FILE_PATH).toFile()
    assertThat(lockFile.exists()).isTrue()

    daemon.stop()

    // Wait a bit for cleanup
    Thread.sleep(100)

    assertThat(lockFile.exists()).isFalse()
    assertThat(daemon.isRunning()).isFalse()
    assertThat(testExiter.exitCalled).isTrue()
    assertThat(testExiter.lastExitStatus).isEqualTo(0)
  }

  @Test
  fun `handles exit command`() {
    val daemonData = daemon.readLockFile()
    assertThat(daemonData).isNotNull

    // Send exit command
    Socket("localhost", daemonData!!.port).use { socket ->
      socket.getOutputStream().write("exit\n".toByteArray())
    }

    // Wait a bit for cleanup
    Thread.sleep(100)

    assertThat(testExiter.exitCalled).isTrue()
    assertThat(testExiter.lastExitStatus).isEqualTo(0)
    assertThat(daemon.isRunning()).isFalse()
  }

  @Test
  fun `handles unknown command`() {
    val daemonData = daemon.readLockFile()
    assertThat(daemonData).isNotNull

    // Send unknown command
    Socket("localhost", daemonData!!.port).use { socket ->
      socket.getOutputStream().write("unknown\n".toByteArray())
      val reader = socket.getInputStream().bufferedReader()
      val exitCode = reader.readLine()
      val output = reader.readText()
      assertThat(exitCode).isEqualTo(ExitCode.FAILURE.toString())
      assertThat(output).isEqualTo("Unknown command: unknown")
    }
  }

  @Test
  fun `stops older daemon version when starting new daemon`() {
    val daemonData = daemon.readLockFile()
    assertThat(daemonData).isNotNull

    // Start a new daemon with a newer version
    val newDaemon = Daemon(
      systemExiter = testExiter,
      idleTimeout = IDLE_TIMEOUT,
      maxRuntime = MAX_RUNTIME,
      timeoutCheckerInterval = TIMEOUT_CHECKER_INTERVAL,
      workingDir = tempDir,
      version = 2
    )

    // Start daemon in a separate thread
    thread(start = true) {
      try {
        newDaemon.start()
      } catch (e: Exception) {
        // Ignore exceptions from normal shutdown
        if (!e.message?.contains("Socket closed")!!) {
          throw e
        }
      }
    }

    // Wait for new daemon to start (max 1 second)
    var attempts = 0
    while (!newDaemon.isRunning() && attempts < 20) {
      Thread.sleep(50)
      attempts++
    }

    assertThat(newDaemon.isRunning()).isTrue()

    // Check that the old daemon has stopped
    assertThat(daemon.isRunning()).isFalse()

    // Stop the new daemon
    newDaemon.stop()
  }

  companion object {
    val IDLE_TIMEOUT = Duration.ofSeconds(2)
    val MAX_RUNTIME = Duration.ofSeconds(5)
    val TIMEOUT_CHECKER_INTERVAL = Duration.ofSeconds(1)
  }
}
