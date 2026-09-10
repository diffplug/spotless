import com.diffplug.gradle.spotless.FreshMarkExtension

plugins {
  id("com.diffplug.spotless")
}

val freshmarkSetup: (FreshMarkExtension) -> Unit = { freshmark ->
  freshmark.target("*.md")
  freshmark.propertiesFile(rootProject.file("gradle.properties"))
  freshmark.properties {
    put("yes", ":+1:")
    put("no", ":white_large_square:")
  }
  freshmark.leadingTabsToSpaces(2)
  freshmark.endWithNewline()
}

spotless {
  freshmark {
    freshmarkSetup(this)
    // disable ratchetFrom for freshmark, because we always want to update the readme links
    ratchetFrom(null as String?)
  }
}

// if this freshmark has a changelog file, then it has version-sensitive content
pluginManager.withPlugin("com.diffplug.spotless-changelog") {
  val changelog = spotlessChangelog
  val versionLast = changelog.versionLast
  val versionNext = changelog.versionNext

  // normally we use versionLast for our freshmark
  spotless {
    freshmark {
      properties {
        put("versionLast", versionLast)
      }
    }
  }

  // create a freshmark apply task manually
  val freshmark = FreshMarkExtension(the())
  freshmarkSetup(freshmark)
  freshmark.properties {
    // that uses versionNext as versionLast
    put("versionLast", versionNext)
  }

  val changelogBumpFreshmark = freshmark.createIndependentApplyTask("changelogBumpFreshmark")
  // freshmark should run after the changelog bump
  changelogBumpFreshmark.dependsOn(tasks.named("changelogBump"))

  val changelogBumpFreshmarkGitAdd =
      tasks.register<Exec>("changelogBumpFreshmarkGitAdd") {
        // this git add should run after the freshmark
        dependsOn(changelogBumpFreshmark)
        commandLine("git", "add", "*.md")
      }

  tasks.named("changelogPush") {
    dependsOn(changelogBumpFreshmarkGitAdd)
  }
}
