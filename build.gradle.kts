plugins {
  alias(libs.plugins.kotlin) apply false
  alias(libs.plugins.dependencyAnalysis)
}

// https://github.com/autonomousapps/dependency-analysis-gradle-plugin/wiki
dependencyAnalysis {
  issues {
    all {
      onAny {
        severity("fail")
      }
    }
  }
}