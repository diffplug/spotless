import org.gradle.api.plugins.JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.JAVADOC_ELEMENTS_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.SOURCES_ELEMENTS_CONFIGURATION_NAME
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

version = spotlessChangelog.versionNext

val publishedElements =
    listOf(
        API_ELEMENTS_CONFIGURATION_NAME,
        RUNTIME_ELEMENTS_CONFIGURATION_NAME,
        JAVADOC_ELEMENTS_CONFIGURATION_NAME,
        SOURCES_ELEMENTS_CONFIGURATION_NAME,
    )

configurations.configureEach {
  when (name) {
    in publishedElements ->
        outgoing {
          // Main/current capability.
          capability("com.diffplug.spotless:spotless-plugin-gradle:$version")
          // Historical capabilities.
          capability("com.diffplug.gradle.spotless:spotless-plugin-gradle:$version")
        }
  }
}

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
      description = property("POM_DESCRIPTION").toString()
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
  }
}

tasks.publishPlugins { enabled = !version.toString().endsWith("-SNAPSHOT") }
