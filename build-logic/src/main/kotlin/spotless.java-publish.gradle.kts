plugins {
  `java-library`
  id("com.vanniktech.maven.publish")
  signing
}

tasks.withType<Javadoc>().configureEach {
  options {
    (this as? StandardJavadocDocletOptions)?.apply {
      encoding = Charsets.UTF_8.name()
      // Where it's possible to name parameters and methods clearly enough that javadoc is not
      // necessary, why make the code bigger?  Thus, no javadoc warnings.
      addStringOption("Xdoclint:none", "-quiet")
      addStringOption("Xwerror", "-quiet")
      addStringOption("source", "17")
      val version = project.version.toString()
      val group = project.group.toString()
      val artifactId = project.findProperty("POM_ARTIFACT_ID")?.toString() ?: project.name
      val javadocInfo =
          "<h2><a href=\"https://github.com/diffplug/spotless\" style=\"text-transform: none;\">$group:$artifactId:$version</a> by <a href=\"https://www.diffplug.com\" style=\"text-transform: none;\">DiffPlug</a></h2>"
      header = javadocInfo

      linksOffline("https://docs.gradle.org/6.1.1/javadoc/", "../gradle/javadoc/gradle")
      val versionLast = rootSpotlessChangelog.versionLast
      linksOffline(
          "https://javadoc.io/static/com.diffplug.spotless/spotless-lib/$versionLast",
          "../gradle/javadoc/spotless-lib",
      )
      linksOffline(
          "https://javadoc.io/static/com.diffplug.spotless/spotless-lib-extra/$versionLast",
          "../gradle/javadoc/spotless-lib-extra",
      )
    }
  }
}

if (System.getenv("JITPACK") == "true") {
  signing {
    isRequired = false
  }
} else {
  signing {
    if (
        !project.providers.gradleProperty("signingInMemoryKey").isPresent &&
            System.getenv("ORG_GRADLE_PROJECT_gpg_key64") != null
    ) {
      val gpgKey = decode64("ORG_GRADLE_PROJECT_gpg_key64")
      useInMemoryPgpKeys(
          "0x4272C851",
          gpgKey,
          System.getenv("ORG_GRADLE_PROJECT_gpg_passphrase"),
      )
    }
  }

  // find the project with the changelog (this project for plugins, root project for libs)
  val changelogProject = if (pluginManager.hasPlugin("spotless.changelog")) project else rootProject
  val changelogTasks = changelogProject.tasks

  // ensures that nothing will be built if changelogPush will end up failing
  tasks.jar {
    dependsOn(changelogTasks.named("changelogCheck"))
  }

  // ensures that changelog bump and push only happens if the publish was successful
  changelogTasks.named("changelogBump") {
    dependsOn(tasks.named("publishToMavenCentral"))
    // if we have a Gradle plugin, we need to push it up to the plugin portal too
    plugins.withId("com.gradle.plugin-publish") {
      dependsOn(tasks.named("publishPlugins"))
    }
  }
}
