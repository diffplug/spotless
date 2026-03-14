import com.diffplug.gradle.spotless.FreshMarkExtension

plugins {
	id("com.diffplug.spotless")
}

val freshMarkSetup: (FreshMarkExtension) -> Unit = {
	it.target("*.md")
	it.propertiesFile(rootProject.file("gradle.properties"))
	it.properties {
		put("yes", ":+1:")
		put("no", ":white_large_square:")
	}
	it.leadingTabsToSpaces(2)
	it.endWithNewline()
}

spotless {
	if (project == rootProject) {
		freshmark {
			freshMarkSetup(this)
			ratchetFrom(null)
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
				"import org\\.gradle\\.api\\.internal\\.(.*)",
				"Don't use Gradle's internal API"
			)
		}
	}

	groovyGradle {
		target("*.gradle", "gradle/*.gradle")
		greclipse().configFile(
			rootProject.files(
				"gradle/spotless.eclipseformat.xml",
				"gradle/spotless.groovyformat.prefs"
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

// Extra freshmark logic from spotless-freshmark.gradle
if (project == rootProject) {
	val versionLast = rootProject.extra["versionLast"]?.toString()
	val versionNext = rootProject.extra["versionNext"]?.toString()

	if (versionLast != null && versionNext != null) {
		if (tasks.names.contains("changelogCheck")) {
			spotless {
				freshmark {
					properties {
						put("versionLast", versionLast)
					}
				}
			}

			// create a freshmark apply task manually
			val freshMarkApply = FreshMarkExtension(spotless)
			freshMarkSetup(freshMarkApply)
			freshMarkApply.properties {
				put("versionLast", versionNext)
			}

			val changelogBumpFreshMark = freshMarkApply.createIndependentApplyTask("changelogBumpFreshmark")
			changelogBumpFreshMark.dependsOn(tasks.named("changelogBump"))

			val changelogBumpFreshMarkGitAdd by tasks.registering(Exec::class) {
				dependsOn(changelogBumpFreshMark)
				commandLine("git", "add", "*.md")
			}

			tasks.named("changelogPush") {
				dependsOn(changelogBumpFreshMarkGitAdd)
			}
		}
	}
}
