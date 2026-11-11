plugins {
  id("com.diffplug.spotless")
}

spotless {
  if (project == rootProject) {
    kotlin {
      target("build-logic/src/**/*.kt", "*.kts", "build-logic/*.kts")
      ktfmt(libs.ktfmt.get().version)
    }
    format("dotfiles") {
      target(".gitignore", ".gitattributes", ".editorconfig")
      leadingTabsToSpaces(2)
      trimTrailingWhitespace()
      endWithNewline()
    }
  } else {
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
}
