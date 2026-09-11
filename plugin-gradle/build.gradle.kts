import org.gradle.plugin.compatibility.compatibility

plugins {
  `java-library`
  `java-gradle-plugin`
  alias(libs.plugins.plugin.publish)
  id("spotless.changelog")
  id("spotless.java-setup")
  id("spotless.spotless-freshmark")
  id("spotless.special-tests")
  // must come after java-gradle-plugin, which is what creates the 'pluginMaven' publication that
  // spotless.java-publish then configures
  id("spotless.java-publish")
}

extra["artifactId"] = property("artifactIdGradle")

version = spotlessChangelog.versionNext

dependencies {
  if (
      version.toString().endsWith("-SNAPSHOT") ||
          (rootSpotlessChangelog.versionNext == rootSpotlessChangelog.versionLast)
  ) {
    api(projects.lib)
    api(projects.libExtra)
  } else {
    api("com.diffplug.spotless:spotless-lib:${rootSpotlessChangelog.versionLast}")
    api("com.diffplug.spotless:spotless-lib-extra:${rootSpotlessChangelog.versionLast}")
  }
  implementation(libs.durian.core)
  implementation(libs.durian.io)
  implementation(libs.durian.collect)
  implementation(libs.jgit)

  testImplementation(projects.testlib)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testImplementation(libs.durian.testlib)
  testImplementation(libs.owasp.encoder)
  testImplementation(libs.solstice)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach { testLogging.showStandardStreams = true }

tasks.validatePlugins {
  // TODO: https://github.com/gradle/gradle/issues/22600
  enableStricterValidation = true
}

//////////////////////////
// GRADLE PLUGIN PORTAL //
//////////////////////////
gradlePlugin {
  website = "https://github.com/diffplug/spotless"
  vcsUrl = "https://github.com/diffplug/spotless"
  plugins {
    create("spotlessPlugin") {
      id = "com.diffplug.spotless"
      implementationClass = "com.diffplug.gradle.spotless.SpotlessPlugin"
      displayName = "Spotless formatting plugin"
      description = project.description
      tags =
          listOf(
              "format",
              "style",
              "license",
              "header",
              "google-java-format",
              "eclipse",
              "ktlint",
              "ktfmt",
              "diktat",
              "tsfmt",
              "prettier",
              "scalafmt",
              "scalafix",
              "black",
              "clang-format",
          )
      compatibility { features { configurationCache = true } }
    }
    create("spotlessPluginLegacy") {
      id = "com.diffplug.gradle.spotless"
      implementationClass = "com.diffplug.gradle.spotless.SpotlessPluginRedirect"
      displayName = "Spotless formatting plugin (legacy)"
      description = project.description
      tags = listOf("format")
    }
  }
}

tasks.publishPlugins { enabled = !version.toString().endsWith("-SNAPSHOT") }
