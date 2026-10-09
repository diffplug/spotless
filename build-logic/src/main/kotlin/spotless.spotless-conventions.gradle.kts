plugins {
  id("com.diffplug.spotless")
}

// The released Spotless plugin used here only knows the legacy `com.facebook:ktfmt` coordinate.
// TODO: switch back to `libs.ktfmt` once a Spotless release supporting ktfmt 0.65+ is used.
val KTFMT_VERSION = "0.64"

spotless {
  if (project != rootProject) {
    java {
      ratchetFrom("origin/main")
      bumpThisNumberIfACustomStepChanges(1)
      licenseHeaderFile(rootProject.file("gradle/spotless.license"))
      importOrderFile(rootProject.file("gradle/spotless.importorder"))
      eclipse().configFile(rootProject.file("gradle/spotless.eclipseformat.xml"))
      trimTrailingWhitespace()
      removeUnusedImports()
      formatAnnotations()
      forbidWildcardImports()
      forbidRegex(
          "ForbidGradleInternal",
          """import org\.gradle\.api\.internal\.(.*)""",
          "Don't use Gradle's internal API",
      )
    }
  }
  kotlin {
    target("build-logic/src/**/*.kt")
    ktfmt(KTFMT_VERSION)
  }
  kotlinGradle {
    target("*.gradle.kts", "build-logic/*.gradle.kts", "build-logic/src/**/*.gradle.kts")
    ktfmt(KTFMT_VERSION)
  }
  format("dotfiles") {
    target(".gitignore", ".gitattributes", ".editorconfig")
    leadingTabsToSpaces(2)
    trimTrailingWhitespace()
    endWithNewline()
  }
}
