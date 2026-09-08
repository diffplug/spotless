import com.diffplug.gradle.spotless.FreshMarkExtension
import com.diffplug.spotless.changelog.gradle.ChangelogExtension

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

pluginManager.withPlugin("com.diffplug.spotless-changelog") {
  val changelog = the<ChangelogExtension>()
  val versionLast = changelog.versionLast
  val versionNext = changelog.versionNext

  spotless {
    freshmark {
      properties {
        put("versionLast", versionLast)
      }
    }
  }

  val freshmark = FreshMarkExtension(the())
  freshmarkSetup(freshmark)
  freshmark.properties {
    put("versionLast", versionNext)
  }

  val changelogBumpFreshmark = freshmark.createIndependentApplyTask("changelogBumpFreshmark")
  changelogBumpFreshmark.dependsOn(tasks.named("changelogBump"))

  val changelogBumpFreshmarkGitAdd =
      tasks.register<Exec>("changelogBumpFreshmarkGitAdd") {
        dependsOn(changelogBumpFreshmark)
        commandLine("git", "add", "*.md")
      }

  tasks.named("changelogPush") {
    dependsOn(changelogBumpFreshmarkGitAdd)
  }
}
