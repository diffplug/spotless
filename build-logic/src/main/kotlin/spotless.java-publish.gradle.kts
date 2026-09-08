import com.diffplug.spotless.changelog.gradle.ChangelogExtension
import java.util.Base64

plugins {
  `java-library`
  `maven-publish`
  signing
}

// getMimeDecoder, not getDecoder: the secrets are line-wrapped. GPG_KEY64 is produced with
// `openssl base64`, which wraps at 64 chars unless given -A, and the basic decoder rejects those
// newlines. Groovy's String.decodeBase64 (used before the Kotlin DSL migration) skipped whitespace.
// This only runs on a real `-Prelease=true` publish, so CI never exercises it.
fun decode64(varName: String): String {
  val envValue = System.getenv(varName) ?: return ""
  return String(Base64.getMimeDecoder().decode(envValue), Charsets.UTF_8)
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
      addStringOption("Xdoclint:none", "-quiet")
      addStringOption("Xwerror", "-quiet")
      addStringOption("source", "17")
      val artifactId = project.findProperty("artifactId")?.toString() ?: project.name
      val org = project.findProperty("org")?.toString() ?: "diffplug"
      val name = project.findProperty("name")?.toString() ?: "spotless"
      val version = project.version.toString()
      val group = project.group.toString()
      val javadocInfo =
          "<h2><a href=\"https://github.com/$org/$name\" style=\"text-transform: none;\">$group:$artifactId:$version</a> by <a href=\"https://www.diffplug.com\" style=\"text-transform: none;\">DiffPlug</a></h2>"
      header = javadocInfo

      val dotdotGradle = if (project.name.startsWith("eclipse-")) "../../gradle" else "../gradle"
      linksOffline("https://docs.gradle.org/6.1.1/javadoc/", "$dotdotGradle/javadoc/gradle")
      val versionLast = rootProject.the<ChangelogExtension>().versionLast
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

tasks.check {
  dependsOn(tasks.javadoc)
}

afterEvaluate {
  val isExt = project.name.startsWith("eclipse-")
  val artifactId = project.findProperty("artifactId")?.toString() ?: project.name
  val isPluginMaven = artifactId == "spotless-maven-plugin"
  val isPluginGradle = pluginManager.hasPlugin("java-gradle-plugin")

  publishing {
    publications {
      val pluginMaven =
          findByName("pluginMaven") as? MavenPublication
              ?: create<MavenPublication>("pluginMaven") {
                if (!isPluginGradle) {
                  from(components["java"])
                }
              }

      pluginMaven.apply {
        groupId = project.group.toString()
        this.artifactId = artifactId
        version = project.version.toString()

        pom {
          name = artifactId
          description = project.description
          url = "https://github.com/${project.findProperty("org")}/${rootProject.name}"
          scm {
            url = "https://github.com/${project.findProperty("org")}/${rootProject.name}"
            connection =
                "scm:git:https://github.com/${project.findProperty("org")}/${rootProject.name}.git"
            developerConnection =
                "scm:git:ssh:git@github.com/${project.findProperty("org")}/${rootProject.name}.git"
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

      val changelogProject = if (tasks.names.contains("changelogBump")) project else rootProject
      val changelogTasks = changelogProject.tasks

      tasks.jar {
        dependsOn(changelogTasks.named("changelogCheck"))
      }

      val thisProj = project
      changelogTasks.named("changelogBump") {
        dependsOn(
            ":${thisProj.path.removePrefix(":")}:publishPluginMavenPublicationToSonatypeRepository"
        )
        dependsOn(":closeAndReleaseSonatypeStagingRepository")
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
