plugins {
  id("com.diffplug.spotless")
}

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
    ktfmt(libs.ktfmt.get().version)
  }
  kotlinGradle {
    target("*.gradle.kts", "build-logic/*.gradle.kts", "build-logic/src/**/*.gradle.kts")
    ktfmt(libs.ktfmt.get().version)
  }
  groovyGradle {
    target("*.gradle", "gradle/*.gradle")
    greclipse()
        .configFile(
            rootProject.files(
                "gradle/spotless.eclipseformat.xml",
                "gradle/spotless.groovyformat.prefs",
            )
        )
  }
  format("dotfiles") {
    target(".gitignore", ".gitattributes", ".editorconfig")
    leadingTabsToSpaces(2)
    trimTrailingWhitespace()
    endWithNewline()
  }
}
