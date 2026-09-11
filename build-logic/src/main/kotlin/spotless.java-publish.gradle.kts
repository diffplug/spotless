plugins {
  `java-library`
  `maven-publish`
  signing
}

java {
  withJavadocJar()
  withSourcesJar()
}

tasks.named("sourcesJar") {
  dependsOn(tasks.jar)
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
      val artifactId = requiredProperty("artifactId")
      val org = requiredProperty("org")
      val version = project.version.toString()
      val group = project.group.toString()
      val javadocInfo =
          "<h2><a href=\"https://github.com/$org/${rootProject.name}\" style=\"text-transform: none;\">$group:$artifactId:$version</a> by <a href=\"https://www.diffplug.com\" style=\"text-transform: none;\">DiffPlug</a></h2>"
      header = javadocInfo

      val dotdotGradle = if (project.name.startsWith("eclipse-")) "../../gradle" else "../gradle"
      linksOffline("https://docs.gradle.org/6.1.1/javadoc/", "$dotdotGradle/javadoc/gradle")
      val versionLast = rootSpotlessChangelog.versionLast
      linksOffline(
          "https://javadoc.io/static/com.diffplug.spotless/spotless-lib/$versionLast",
          "$dotdotGradle/javadoc/spotless-lib",
      )
      linksOffline(
          "https://javadoc.io/static/com.diffplug.spotless/spotless-lib-extra/$versionLast",
          "$dotdotGradle/javadoc/spotless-lib-extra",
      )
    }
  }
}

// make sure bad javadoc breaks the build
tasks.check {
  dependsOn(tasks.javadoc)
}

afterEvaluate {
  val isExt = project.name.startsWith("eclipse-")
  val artifactId = requiredProperty("artifactId")
  val org = requiredProperty("org")
  val isPluginMaven = artifactId == "spotless-maven-plugin"
  val isPluginGradle = pluginManager.hasPlugin("java-gradle-plugin")

  publishing {
    publications {
      // java-gradle-plugin creates 'pluginMaven' from its own afterEvaluate, with the java
      // component already attached. Ours has to run after that one, which it only does because
      // java-gradle-plugin is applied earlier in the consumer's plugins block. If that ever
      // inverts we would create an empty publication here and ship a POM with no jar, so check
      // rather than silently degrade.
      val existing = findByName("pluginMaven") as? MavenPublication
      check(!isPluginGradle || existing != null) {
        "${project.path} applies java-gradle-plugin, which must be applied before " +
            "spotless.java-publish so that it can create the 'pluginMaven' publication"
      }
      val pluginMaven =
          existing ?: create<MavenPublication>("pluginMaven") { from(components["java"]) }

      pluginMaven.apply {
        groupId = project.group.toString()
        this.artifactId = artifactId
        version = project.version.toString()

        pom {
          name = artifactId
          description = project.description
          url = "https://github.com/$org/${rootProject.name}"
          scm {
            url = "https://github.com/$org/${rootProject.name}"
            connection = "scm:git:https://github.com/$org/${rootProject.name}.git"
            developerConnection = "scm:git:ssh:git@github.com/$org/${rootProject.name}.git"
          }
          licenses {
            license {
              if (isExt) {
                name = "Eclipse Public License - v 1.0"
                url = "https://www.eclipse.org/legal/epl-v10.html"
              } else {
                name = "The Apache Software License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
              }
              distribution = "repo"
            }
          }
          developers {
            if (isExt) {
              @Suppress("UNCHECKED_CAST")
              val devs = project.findProperty("developers") as? Map<String, Map<String, String>>
              devs?.forEach { (extId, extValues) ->
                developer {
                  id = extId
                  name = extValues["name"]
                  email = extValues["email"]
                }
              }
            } else {
              if (isPluginMaven) {
                developer {
                  id = "lutovich"
                  name = "Konstantin Lutovich"
                  email = "konstantin.lutovich@neotechnology.com"
                }
              }
              developer {
                id = "nedtwigg"
                name = "Ned Twigg"
                email = "ned.twigg@diffplug.com"
              }
            }
          }
          if (isPluginMaven) {
            // Maven plugin requires Maven 3.1.0+ to run
            withXml {
              val rootNode = asNode()
              val prerequisites = rootNode.appendNode("prerequisites")
              prerequisites.appendNode("maven", "3.1.0")
            }
          }
        }
      }
    }
    if (System.getenv("JITPACK") == "true" || project.version.toString().endsWith("-SNAPSHOT")) {
      signing {
        isRequired = false
      }
    } else {
      signing {
        val gpgKey = decode64("ORG_GRADLE_PROJECT_gpg_key64")
        useInMemoryPgpKeys(
            "0x4272C851",
            gpgKey,
            System.getenv("ORG_GRADLE_PROJECT_gpg_passphrase"),
        )
        sign(publishing.publications)
      }

      // find the project with the changelog (this project for plugins, root project for libs)
      val changelogProject = if (tasks.names.contains("changelogBump")) project else rootProject
      val changelogTasks = changelogProject.tasks

      // ensures that nothing will be built if changelogPush will end up failing
      tasks.jar {
        dependsOn(changelogTasks.named("changelogCheck"))
      }

      // ensures that changelog bump and push only happens if the publish was successful
      val thisProj = project
      changelogTasks.named("changelogBump") {
        dependsOn(
            ":${thisProj.path.removePrefix(":")}:publishPluginMavenPublicationToSonatypeRepository"
        )
        dependsOn(":closeAndReleaseSonatypeStagingRepository")
        // if we have a Gradle plugin, we need to push it up to the plugin portal too
        if (thisProj.tasks.names.contains("publishPlugins")) {
          dependsOn(thisProj.tasks.named("publishPlugins"))
        }
      }
    }
  }
}

tasks.withType<AbstractArchiveTask>().configureEach {
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}
