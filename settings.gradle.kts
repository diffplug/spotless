pluginManagement {
  includeBuild("build-logic")
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("com.diffplug.spotless") version "8.10.0" apply false
  id("com.github.spotbugs") version "6.5.11" apply false
  id("com.gradle.develocity") version "4.5.0"
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

if (System.getenv("CI") != null) {
  // use the remote buildcache on all CI builds
  buildCache {
    fun cred(name: String): String? = System.getenv(name) ?: System.getProperty(name)
    remote<HttpBuildCache> {
      url = uri("https://buildcache.diffplug.com/cache/")
      // but we only push if it's a trusted build (not PRs)
      val user = cred("buildcacheuser")
      val pass = cred("buildcachepass")
      if (user != null && pass != null) {
        isPush = true
        credentials {
          username = user
          password = pass
        }
      } else {
        credentials {
          username = "anonymous"
        }
      }
    }
  }
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
    publishing {
      onlyIf { System.getenv("CI") != null }
    }
  }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "spotless"

include("lib") // reusable library with no dependencies

include("lib-extra") // reusable library with lots of dependencies

include("plugin-gradle") // gradle-specific glue code

include("testlib") // library for sharing test infrastructure between the projects below

fun getStartProperty(name: String): String? =
    startParameter.projectProperties[name]
        ?: File(startParameter.gradleUserHomeDir, "gradle.properties")
            .takeIf { it.exists() }
            ?.reader()
            ?.use { java.util.Properties().apply { load(it) }.getProperty(name) }

// Groovy's String.toBoolean() -- what this used before the Kotlin DSL migration -- accepts "true",
// "y" and "1", ignoring case. Kotlin's toBoolean() only accepts "true", so the short forms would
// silently become no-ops.
fun String.toBooleanGroovy(): Boolean = trim().lowercase() in setOf("true", "y", "1")

val excludeMaven =
    System.getenv("SPOTLESS_EXCLUDE_MAVEN")?.toBooleanGroovy() == true ||
        getStartProperty("SPOTLESS_EXCLUDE_MAVEN")?.toBooleanGroovy() == true

if (!excludeMaven) {
  include("plugin-maven") // maven-specific glue code
}
