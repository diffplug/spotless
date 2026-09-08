import java.util.Base64

plugins {
  id("io.github.gradle-nexus.publish-plugin")
}

fun decode64(varName: String): String {
  val envValue = System.getenv(varName) ?: return ""
  return String(Base64.getDecoder().decode(envValue), Charsets.UTF_8)
}

group = "com.diffplug.spotless"

nexusPublishing {
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
