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

// the root project and plugins have their own changelogs
spotlessChangelog {
  changelogFile("CHANGES.md")
  // need -Prelease=true in order to do a publish
  setAppendDashSnapshotUnless_dashPrelease(true)
  branch("release")
  tagPrefix("$kind/")
  commitMessage("Published $kind/{{version}}") // {{version}} will be replaced
  tagMessage("{{changes}}")
  runAfterPush(
      "gh release create $kind/{{version}} --title '$releaseTitle v{{version}}' --notes-from-tag"
  )
}

if (project == rootProject) {
  gradle.taskGraph.whenReady {
    val changelogPushTasks = allTasks.filter { it.name == "changelogPush" }.map { it.path }
    if (changelogPushTasks.size > 1) {
      // make sure only one changelog gets published per tag/commit
      throw IllegalArgumentException(
          "Run changelogPush one at a time:\n" + changelogPushTasks.joinToString("\n")
      )
    }
    if (changelogPushTasks.size == 1) {
      // if the one thing being published is a plugin, make sure there aren't any unreleased
      // changes in lib
      val isPlugin =
          changelogPushTasks[0] == ":plugin-gradle:changelogPush" ||
              changelogPushTasks[0] == ":plugin-maven:changelogPush"
      if (
          isPlugin &&
              !rootSpotlessChangelog.parsedChangelog.noUnreleasedChanges() &&
              rootProject.findProperty("ignoreUnreleasedLib")?.toString() != "true"
      ) {
        throw IllegalArgumentException(
            "You're going to publish ${changelogPushTasks[0]}, but there are unreleased features in lib!\n" +
                "You should run :changelogPush first!  Else you'll be missing out on:\n" +
                "${rootSpotlessChangelog.parsedChangelog.unreleasedChanges()}\n" +
                "If it's okay to miss those and link against the old ${rootSpotlessChangelog.versionLast} then " +
                "add -PignoreUnreleasedLib=true"
        )
      }
    }
  }
}
