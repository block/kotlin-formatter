# Change Log

## [Unreleased]

### New:
- Nothing yet!

### Changed:
- Nothing yet!

### Fixed:
- Nothing yet!

## [1.5.0] - 2025-11-19

### New:
- Added a gradle plugin (id: `xyz.block.kotlinformatter`)

### Changed:
- Updated various internal dependencies.

## [1.4.1] - 2025-10-14

### Changed:
- Updated ktfmt to `v0.59`.
- Updated various other internal dependencies.

## [1.4.0] - 2025-09-24

### Changed:
- Updated ktfmt to `v0.58`.
- Updated various other internal dependencies.

## [1.3.0] - 2025-08-15

### Changed:
- Add support for IntelliJ version 2025.2
- Various dependency updates

## [1.2.2] - 2025-05-20

### Changed:
- Update all non-major dependencies
- Update publication to Sonatype Central Portal 

## [1.2.1] - 2025-05-07

### Fixed:
- Fixed bug with formatting moved files in pre-commit mode

## [1.2.0] - 2025-04-30

### Changed:
- Upgrade the supported IntelliJ versions to the 2024.1 to 2025.1 range

## [1.1.0] - 2025-03-27

### New:
- **Daemon mode** Added new long-running mode for better performance. [#67](https://github.com/block/kotlin-formatter/pull/67) by @yissachar

### Changed:
- Some performance optimizations [#71](https://github.com/block/kotlin-formatter/pull/71) and [#75](https://github.com/block/kotlin-formatter/pull/75) by @yissachar
- Various dependency updates

## [1.0.3] - 2025-02-21

### New:

- **IntelliJ Plugin**: Added IntelliJ plugin for formatting support. [#33](https://github.com/block/kotlin-formatter/pull/33) by @wsutina
  - Added configuration for using a formatting script in the IntelliJ plugin. [#42](https://github.com/block/kotlin-formatter/pull/42) by @mmollaverdi
  - Enabled IntelliJ plugin configuration per project. [#41](https://github.com/block/kotlin-formatter/pull/41) by @mmollaverdi
  - Enabled automatically upgrading the supported IntelliJ IDEA version. [#39](https://github.com/block/kotlin-formatter/pull/39) by @mmollaverdi

### Changed
 
- **Formatting Ignore File Location**: Updated the location of the formatting ignore file. [#38](https://github.com/block/kotlin-formatter/pull/38) by @wsutina
- **Dependency Updates**:
    - Updated `com.facebook:ktfmt` to `v0.54`. [#15](https://github.com/block/kotlin-formatter/pull/15) by @renovate
    - Updated `com.gradleup.shadow` to `v8.3.6`. [#34](https://github.com/block/kotlin-formatter/pull/34) by @renovate
    - Updated `org.jetbrains.intellij.platform.settings` to `v2.2.1`. [#35](https://github.com/block/kotlin-formatter/pull/35) by @renovate
    - Updated `clikt` to `v5.0.3`. [#44](https://github.com/block/kotlin-formatter/pull/44) by @renovate
    - Updated `dependencyAnalysis` to `v2.8.2`. [#45](https://github.com/block/kotlin-formatter/pull/45) by @renovate

## [1.0.2] - 2025-01-30

### Changed:
- Updated to gradle 8.12.1, kotlin 2.1.10.
- Update other dependencies (except ktfmt) to newest available versions.

### Fixed:
- Incorrect property in CONTRIBUTING.md

## [1.0.1] - 2025-01-14

### Changed: 
- Update release pipeline and releasing instruction

### Fixed:
- Fix incorrect local build scan property name

## [1.0.0] - 2024-12-12

First OSS release.
