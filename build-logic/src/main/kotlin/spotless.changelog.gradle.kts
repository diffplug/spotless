plugins {
	id("com.diffplug.spotless-changelog")
}

val kind = when (project.name) {
	"plugin-gradle" -> "gradle"
	"plugin-maven" -> "maven"
	else -> {
		check(project == rootProject)
		"lib"
	}
}

val releaseTitle = when (project.name) {
	"plugin-gradle" -> "Gradle Plugin"
	"plugin-maven" -> "Maven Plugin"
	else -> "Lib"
}

spotlessChangelog {
	changelogFile(file("CHANGES.md"))
	setAppendDashSnapshotUnless_dashPrelease(true)
	branch("release")
	tagPrefix("${kind}/")
	commitMessage("Published ${kind}/{{version}}")
	tagMessage("{{changes}}")
	runAfterPush("gh release create ${kind}/{{version}} --title '${releaseTitle} v{{version}}' --notes-from-tag")
	rootProject.extra["versionLast"] = versionLast
	rootProject.extra["versionNext"] = versionNext
}

if (project == rootProject) {
	gradle.taskGraph.whenReady {
		val changelogPushTasks = allTasks.filter { it.name == "changelogPush" }.map { it.path }
		if (changelogPushTasks.size > 1) {
			error("Run changelogPush one at a time:\n" + changelogPushTasks.joinToString("\n"))
		} else if (changelogPushTasks.size == 1) {
			val isPlugin =
				changelogPushTasks[0] == ":plugin-gradle:changelogPush" || changelogPushTasks[0] == ":plugin-maven:changelogPush"
			if (isPlugin) {
				val extension = extensions.getByName("spotlessChangelog")
				val parsedChangelog = extension.javaClass.getMethod("getParsedChangelog").invoke(extension)
				val noUnreleasedChanges =
					parsedChangelog.javaClass.getMethod("noUnreleasedChanges").invoke(parsedChangelog) as Boolean

				if (!noUnreleasedChanges) {
					if (rootProject.findProperty("ignoreUnreleasedLib") != "true") {
						val unreleased =
							parsedChangelog.javaClass.getMethod("unreleasedChanges").invoke(parsedChangelog)
						error(
							"You're going to publish ${changelogPushTasks[0]}, but there are unreleased features in lib!\n" +
								"You should run :changelogPush first!  Else you'll be missing out on:\n" +
								"${unreleased}\n" +
								"If it's okay to miss those and link against the old ${rootProject.extra["versionLast"]} then " +
								"add -PignoreUnreleasedLib=true"
						)
					}
				}
			}
		}
	}
}
