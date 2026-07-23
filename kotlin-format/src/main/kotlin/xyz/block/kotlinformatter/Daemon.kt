package xyz.block.kotlinformatter

import java.io.ByteArrayInputStream
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileLock
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import kotlin.io.path.deleteExisting
import kotlin.concurrent.thread

class Daemon(
  private val systemExiter: SystemExiter = RealSystemExiter(),
  private val idleTimeout: Duration = Duration.ofDays(1),
  private val maxRuntime: Duration = Duration.ofDays(7),
  private val timeoutCheckerInterval: Duration = Duration.ofMinutes(1),
  private val workingDir: Path = rootGitPath,
  private val version: Int = DAEMON_VERSION
) {
  private var lockFile: RandomAccessFile? = null
  private var lock: FileLock? = null
  private var lastCommandTime: Instant = Instant.now()
  private val startTime: Instant = Instant.now()
  private var timeoutChecker: Thread? = null
  private var serverSocket: ServerSocket? = null
  private var isRunning = false
  private val lockFilePath = workingDir.resolve(LOCK_FILE_PATH)

  fun isRunning(): Boolean = isRunning

  private fun shouldShutdown(): Boolean {
    val now = Instant.now()
    val idleTime = Duration.between(lastCommandTime, now)
    val totalRuntime = Duration.between(startTime, now)

    if (idleTime >= idleTimeout) {
      System.err.println("Shutting down due to inactivity (${idleTime.seconds}s idle)")
      return true
    }

    if (totalRuntime >= maxRuntime) {
      System.err.println("Shutting down due to maximum runtime exceeded (${totalRuntime.seconds}s)")
      return true
    }

    return false
  }

  private fun startTimeoutChecker() {
    timeoutChecker = thread(start = true) {
      while (!Thread.currentThread().isInterrupted) {
        if (shouldShutdown()) {
          serverSocket?.close()
          cleanup()
          systemExiter.exit(0)
        }
        Thread.sleep(timeoutCheckerInterval.toMillis())
      }
    }
  }

  fun start() {
    try {
      // Change working directory so git commands work as expected
      workingDir.let { System.setProperty("user.dir", it.toString()) }

      lockFilePath.parent.toFile().mkdirs()

      // If there's a running daemon with an older version, stop it first
      val existingDaemonData = readLockFile()
      if (existingDaemonData != null && existingDaemonData.version < version) {
        stop()
        Thread.sleep(100) // Wait for the old daemon to stop
      }

      // Attempt to lock the file to ensure only one instance is running
      lockFile = RandomAccessFile(lockFilePath.toFile().path, "rw")
      lock = lockFile?.channel?.tryLock()

      if (lock == null) {
        System.err.println("Daemon is already running")
        cleanup()
        return
      }

      // Add shutdown hook to ensure cleanup on unexpected termination
      Runtime.getRuntime().addShutdownHook(Thread { cleanup() })

      serverSocket = ServerSocket(0)
      val assignedPort = serverSocket!!.localPort
      
      // Start the timeout checker thread
      startTimeoutChecker()
      
      // Clear and write the lock file atomically
      lockFile?.let { file ->
        file.setLength(0)
        file.seek(0)
        writeLockFile(DaemonData(DAEMON_VERSION, assignedPort))
      }

      isRunning = true

      while (true) {
        try {
          val clientSocket: Socket = serverSocket!!.accept()

          clientSocket.getInputStream().bufferedReader().use { reader ->
            val message = reader.readLine()
            if (message != null) {
              val messageParts = message.split(" ")
              val command = messageParts[0]

              when (command) {
                "exit" -> {
                  clientSocket.close()
                  serverSocket?.close()
                  cleanup()
                  systemExiter.exit(0)
                }

                "status" -> {
                  val idleTime = Duration.between(lastCommandTime, Instant.now())
                  val totalRuntime = Duration.between(startTime, Instant.now())
                  val response = "Daemon Status:\n" +
                      "Idle time: ${idleTime.seconds}s\n" +
                      "Total runtime: ${totalRuntime.seconds}s\n" +
                      "Time until idle timeout: ${idleTimeout.seconds - idleTime.seconds}s\n" +
                      "Time until max runtime timeout: ${maxRuntime.seconds - totalRuntime.seconds}s"
                  clientSocket.write(ExitCode.SUCCESS, response)
                }
                
                "format" -> {
                  lastCommandTime = Instant.now()
                  val fileArgs = messageParts.drop(1)
                  if (fileArgs.isEmpty()) {
                    clientSocket.write(ExitCode.FAILURE, "No files provided for format command")
                  } else if (fileArgs.size == 1 && fileArgs[0] == "-") {
                    // Process the code starting from a newline and ending at ASCII EOT (04)
                    // Usage: (echo "format -"; echo <CODE>; echo -ne '\04\n') | nc localhost <PORT>
                    // Or replace "echo <CODE>" to "cat" for standard input 
                    val inputLines = mutableListOf<String>()
                    while (true) {
                      val line = reader.readLine() ?: break
                      if (line.trim() == "\u0004") break
                      inputLines.add(line)
                    }
                    val inputContent = inputLines.joinToString("\n")
                    val inputStream = ByteArrayInputStream(inputContent.toByteArray())
                    lateinit var formatted: String 
                    val result = KotlinFormatter(
                      files = listOf("-"),
                      inputStream = inputStream,
                      outputCallback = { formatted = it }
                    ).format()
                    val exitCode = if (result.hasFailure) ExitCode.FAILURE else ExitCode.SUCCESS
                    clientSocket.write(exitCode, if (result.hasFailure) result.output else formatted)
                  } else {
                    val result = KotlinFormatter(files = fileArgs).format()
                    val exitCode = if (result.hasFailure) ExitCode.FAILURE else ExitCode.SUCCESS
                    clientSocket.write(exitCode, result.output)
                  }
                }

                "pre-commit" -> {
                  lastCommandTime = Instant.now()
                  val files = messageParts.drop(1)
                  val result = KotlinFormatter(files = files, preCommit = true).format()
                  val exitCode = if (result.hasFailure) ExitCode.FAILURE else ExitCode.SUCCESS
                  clientSocket.write(exitCode, result.output)
                }

                "pre-push" -> {
                  lastCommandTime = Instant.now()
                  val pushCommit = messageParts[1]
                  val files = messageParts.drop(2)
                  val result =
                    KotlinFormatter(files = files, prePush = true, pushCommit = pushCommit, dryRun = true).format()
                  val exitCode = when {
                    result.hasFailure -> ExitCode.FAILURE
                    result.hasFileChanged -> ExitCode.FILE_CHANGED
                    else -> ExitCode.SUCCESS
                  }
                  clientSocket.write(exitCode, result.output)
                }

                else -> {
                  clientSocket.write(ExitCode.FAILURE, "Unknown command: $command")
                }
              }
            }
          }

          clientSocket.close()
        } catch (e: Exception) {
          if (!serverSocket?.isClosed!!) {
            System.err.println("Error handling client connection: ${e.message}")
            e.printStackTrace()
          }
          // If the socket is closed (due to timeout), break the loop
          if (serverSocket?.isClosed == true) {
            break
          }
        }
      }
    } catch (e: Exception) {
      System.err.println("Error in daemon: ${e.message}")
      cleanup()
      throw e
    }
  }

  private fun cleanup() {
    try {
      timeoutChecker?.interrupt()
      lock?.release()
      lockFile?.close()
      serverSocket?.close()
      if (lockFilePath.toFile().exists()) {
        lockFilePath.deleteExisting()
      }
    } catch (e: Exception) {
      System.err.println("Error during cleanup: ${e.message}")
    } finally {
      lock = null
      lockFile = null
      timeoutChecker = null
      serverSocket = null
      isRunning = false
    }
  }

  fun stop() {
    val daemonData = readLockFile()
    if (daemonData == null) {
      System.err.println("Daemon is not running")
      return
    }

    try {
      Socket("localhost", daemonData.port).use { socket ->
        socket.getOutputStream().write("exit\n".toByteArray())
      }
    } catch (e: Exception) {
      System.err.println("Error stopping daemon: ${e.message}")
      // If we can't connect to the daemon, it might have crashed
      // Try to clean up the lock file
      if (lockFilePath.toFile().exists()) {
        lockFilePath.deleteExisting()
      }
    }
  }

  private fun writeLockFile(daemonData: DaemonData) {
    lockFile?.writeBytes("${daemonData.version} ${daemonData.port}")
  }

  fun readLockFile(): DaemonData? {
    if (!lockFilePath.toFile().exists()) return null
    return try {
      val parts = lockFilePath.toFile().readText().split(" ")
      DaemonData(parts[0].toInt(), parts[1].toInt())
    } catch (e: Exception) {
      System.err.println("Error reading lock file: ${e.message}")
      null
    }
  }

  private fun Socket.write(exitCode: Int, message: String) {
    getOutputStream().write("${exitCode}\n$message".toByteArray())
  }

  companion object {
    const val DAEMON_VERSION = 1
    val LOCK_FILE_PATH = Paths.get(".kotlinformatter/kf.lock")
    val rootGitPath by lazy { Paths.get(GitProcessRunner.run("rev-parse", "--show-toplevel").trim()) }
  }
}
