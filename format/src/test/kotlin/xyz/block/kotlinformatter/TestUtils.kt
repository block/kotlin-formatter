package xyz.block.kotlinformatter

import java.io.File

object TestUtils {
  internal fun <T> withWorkingDir(dir: File, block: () -> T): T {
    val oldWorkingDir = System.getProperty("user.dir")
    try {
      System.setProperty("user.dir", dir.absolutePath)
      return block()
    } finally {
      System.setProperty("user.dir", oldWorkingDir)
    }
  }

  internal fun setupGitUser() {
    GitProcessRunner.run("config", "user.name", "Testy McTestface")
    GitProcessRunner.run("config", "user.email", "test@squareup.com")
  }

  internal fun addPrecommitHook(rootDir: File) {
    rootDir.resolve(".git/hooks/pre-commit").apply {
      writeText(
        """
          |#!/usr/bin/env bash
          |set -euo pipefail
          |java -jar ${System.getenv("JAR_UNDER_TEST")} --pre-commit .
          |
          """
          .trimMargin()
      )
      setExecutable(true, false)
    }
    GitProcessRunner.run("config", "--local", "core.hooksPath", ".git/hooks")
  }
}
