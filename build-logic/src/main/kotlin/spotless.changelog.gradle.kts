plugins {
  id("com.diffplug.spotless-changelog")
}

val (kind, releaseTitle) =
    when (project.name) {
      "plugin-gradle" -> "gradle" to "Gradle Plugin"
      "plugin-maven" -> "maven" to "Maven Plugin"
      else -> {
        check(project == rootProject)
        "lib" to "Lib"
      }
    }

spotlessChangelog {
  changelogFile("CHANGES.md")
  setAppendDashSnapshotUnless_dashPrelease(true)
  branch("release")
  tagPrefix("$kind/")
  commitMessage("Published $kind/{{version}}")
  tagMessage("{{changes}}")
  runAfterPush(
      "gh release create $kind/{{version}} --title '$releaseTitle v{{version}}' --notes-from-tag"
  )
}

if (project == rootProject) {
  gradle.taskGraph.whenReady {
    val changelogPushTasks = allTasks.filter { it.name == "changelogPush" }.map { it.path }
    if (changelogPushTasks.size > 1) {
      throw IllegalArgumentException(
          "Run changelogPush one at a time:\n" + changelogPushTasks.joinToString("\n")
      )
    }
    if (changelogPushTasks.size == 1) {
      val isPlugin =
          changelogPushTasks[0] == ":plugin-gradle:changelogPush" ||
              changelogPushTasks[0] == ":plugin-maven:changelogPush"
      if (
          isPlugin &&
              !rootProject.spotlessChangelog.parsedChangelog.noUnreleasedChanges() &&
              rootProject.findProperty("ignoreUnreleasedLib")?.toString() != "true"
      ) {
        throw IllegalArgumentException(
            "You're going to publish ${changelogPushTasks[0]}, but there are unreleased features in lib!\n" +
                "You should run :changelogPush first!  Else you'll be missing out on:\n" +
                "${rootProject.spotlessChangelog.parsedChangelog.unreleasedChanges()}\n" +
                "If it's okay to miss those and link against the old ${rootProject.spotlessChangelog.versionLast} then " +
                "add -PignoreUnreleasedLib=true"
        )
      }
    }
  }
}
