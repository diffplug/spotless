import java.util.Base64
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

fun decode64(varName: String): String {
	val envValue = System.getenv(varName)
	return if (envValue == null) {
		""
	} else {
		String(Base64.getDecoder().decode(envValue), Charsets.UTF_8)
	}
}

if (project.parent == null) {
	group = "com.diffplug.spotless"

	apply(plugin = "io.github.gradle-nexus.publish-plugin")

	extensions.configure<io.github.gradlenexus.publishplugin.NexusPublishExtension> {
		repositories {
			sonatype {
				nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
				snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
				username = System.getenv("ORG_GRADLE_PROJECT_nexus_user")
				password = decode64("ORG_GRADLE_PROJECT_nexus_pass64")
			}
		}
	}

	val initTask = tasks.named("initializeSonatypeStagingRepository")
	allprojects {
		initTask.configure {
			shouldRunAfter(tasks.withType<Sign>())
		}
	}
} else {
	// Subproject publishing logic
	apply(plugin = "maven-publish")
	apply(plugin = "signing")

	extensions.configure<JavaPluginExtension> {
		withJavadocJar()
		withSourcesJar()
	}

	tasks.named("sourcesJar") {
		dependsOn("jar")
	}

	tasks.withType<Javadoc>().configureEach {
		options {
			this as StandardJavadocDocletOptions
			addStringOption("Xdoclint:none", "-quiet")
			addStringOption("Xwerror", "-quiet")
			encoding = "UTF-8"
			addStringOption("source", "17")
		}
	}

	tasks.named("check") {
		dependsOn(tasks.named("javadoc"))
	}

	afterEvaluate {
		val artifactId = extra.properties["artifactId"] as? String ?: project.name
		val org = extra.properties["org"] as? String ?: "diffplug"

		val javadocInfo = """
        <h2><a href="https://github.com/${org}/${rootProject.name}" style="text-transform: none;">${project.group}:${artifactId}:${project.version}</a>
        by <a href="https://www.diffplug.com" style="text-transform: none;">DiffPlug</a></h2>
    """.trimIndent()

		val dotdotGradle = if (project.name.startsWith("eclipse-")) "../../gradle" else "../gradle"

		tasks.withType<Javadoc>().configureEach {
			options {
				this as StandardJavadocDocletOptions
				header = javadocInfo
				linksOffline("https://docs.gradle.org/6.1.1/javadoc/", "${dotdotGradle}/javadoc/gradle")
				val versionLast = rootProject.extra["versionLast"]?.toString() ?: "unknown"
				linksOffline(
					"https://javadoc.io/static/com.diffplug.spotless/spotless-lib/${versionLast}",
					"${dotdotGradle}/javadoc/spotless-lib"
				)
				linksOffline(
					"https://javadoc.io/static/com.diffplug.spotless/spotless-lib-extra/${versionLast}",
					"${dotdotGradle}/javadoc/spotless-lib-extra"
				)
			}
		}

		val isExt = project.name.startsWith("eclipse-")
		val isPluginMaven = artifactId == "spotless-maven-plugin"

		val rootProjectName = rootProject.name
		val projectDescription = project.description
		val devMap = if (isExt) extra["developers"] as? Map<String, Map<String, String>> else null

		extensions.configure<PublishingExtension> {
			publications {
				maybeCreate<MavenPublication>("pluginMaven").also { pub ->
					if (artifactId != "spotless-plugin-gradle") {
						pub.from(project.components["java"])
					}
					pub.groupId = project.group.toString()

					pub.artifactId = artifactId
					pub.version = project.version.toString()

					pub.pom {
						name = artifactId
						description = projectDescription
						url = "https://github.com/${org}/${rootProjectName}"
						scm {
							url = "https://github.com/${org}/${rootProjectName}"
							connection = "scm:git:https://github.com/${org}/${rootProjectName}.git"
							developerConnection = "scm:git:ssh:git@github.com/${org}/${rootProjectName}.git"
						}
						withXml {
							val node = asNode()

							// Prerequisites
							if (isPluginMaven) {
								val prerequisites = node.appendNode("prerequisites")
								prerequisites.appendNode("maven", "3.1.0")
							}

							// Licenses
							val licenses = node.appendNode("licenses")
							val license = licenses.appendNode("license")
							if (isExt) {
								license.appendNode("name", "Eclipse Public License - v 1.0")
								license.appendNode("url", "https://www.eclipse.org/legal/epl-v10.html")
								license.appendNode("distribution", "repo")
							} else {
								license.appendNode("name", "The Apache Software License, Version 2.0")
								license.appendNode("url", "https://www.apache.org/licenses/LICENSE-2.0.txt")
								license.appendNode("distribution", "repo")
							}

							// Developers
							val developers = node.appendNode("developers")
							if (isExt && devMap != null) {
								devMap.forEach { (devId, devValues) ->
									val dev = developers.appendNode("developer")
									dev.appendNode("id", devId)
									dev.appendNode("name", devValues["name"])
									dev.appendNode("email", devValues["email"])
								}
							} else {
								if (isPluginMaven) {
									val dev = developers.appendNode("developer")
									dev.appendNode("id", "lutovich")
									dev.appendNode("name", "Konstantin Lutovich")
									dev.appendNode("email", "konstantin.lutovich@neotechnology.com")
								}
								val dev = developers.appendNode("developer")
								dev.appendNode("id", "nedtwigg")
								dev.appendNode("name", "Ned Twigg")
								dev.appendNode("email", "ned.twigg@diffplug.com")
							}
						}
					}
				}
			}
		}
	}

	val gpgKey = decode64("ORG_GRADLE_PROJECT_gpg_key64")
	val gpgPassphrase = System.getenv("ORG_GRADLE_PROJECT_gpg_passphrase")
	val canSign = gpgKey.isNotEmpty() && !gpgPassphrase.isNullOrEmpty()

	extensions.configure<SigningExtension> {
		isRequired = false
		if (canSign && System.getenv("JITPACK") != "true" && !project.version.toString().endsWith("-SNAPSHOT")) {
			useInMemoryPgpKeys("0x4272C851", gpgKey, gpgPassphrase)
			sign(the<PublishingExtension>().publications)
		}
	}

	if (System.getenv("JITPACK") != "true" && !project.version.toString().endsWith("-SNAPSHOT")) {
		val changelogTasks = if (tasks.names.contains("changelogBump")) tasks else rootProject.tasks

		tasks.named("jar") {
			dependsOn(changelogTasks.named("changelogCheck"))
		}

		changelogTasks.named("changelogBump") {
			dependsOn(":${project.path}:publishPluginMavenPublicationToSonatypeRepository")
			dependsOn(":closeAndReleaseSonatypeStagingRepository")
			if (project.tasks.names.contains("publishPlugins")) {
				dependsOn(project.tasks.named("publishPlugins"))
			}
		}
	}

	tasks.withType<AbstractArchiveTask>().configureEach {
		isPreserveFileTimestamps = false
		isReproducibleFileOrder = true
	}
}
