# Releasing Guide

Release procedure for Kotlin Formatter.

## 1. Prepare the Release

1. Create a new branch for the release:
   ```sh
   git checkout -b release-x.y.z
   ```

2. Bump version number in `gradle.properties` to next stable version.
3. Update `CHANGELOG.md`:
   1. Change the `Unreleased` header to the release version e.g. `[x.y.z] - YYYY-MM-DD`.
   2. Add a new `Unreleased` section to the top.
      ```md
      ## [Unreleased]

      ### New:
      - Nothing yet!

      ### Changed:
      - Nothing yet!

      ### Fixed:
      - Nothing yet!
      ```

4. Update `README.md` if needed.
5. Commit:
   ```sh
   $ git commit -am "chore: prepare for release x.y.z."
   ```

6. Push the branch and create a pull request.

## 2. Tag and Push the Release

1. Once the PR is approved and merged, check out `main` and pull the latest changes.
2. Tag the release:
   ```sh
   git tag -am "Version x.y.z" x.y.z
   ```
3. Push the tag
   ```sh
   git push origin x.y.z
   ```

The tag will trigger a GitHub Action workflow which will upload the artifacts to Maven Central and create a GitHub release.

### JetBrains Marketplace Approval
Releases of the IntelliJ IDEA plugin are subject to JetBrains' manual approval for every version. While the official documentation states that approval may take **2–3 business days**, in practice, the plugin may appear on the JetBrains Marketplace within approximately **3 hours**.
