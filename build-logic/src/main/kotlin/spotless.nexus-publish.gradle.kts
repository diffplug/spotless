import java.util.Base64

plugins {
  id("io.github.gradle-nexus.publish-plugin")
}

// getMimeDecoder, not getDecoder: the basic decoder rejects the newlines that `base64`/`openssl
// base64` leave in a wrapped secret, whereas Groovy's String.decodeBase64 (used before the Kotlin
// DSL migration) skipped whitespace. See the matching note in spotless.java-publish.gradle.kts.
fun decode64(varName: String): String {
  val envValue = System.getenv(varName) ?: return ""
  return String(Base64.getMimeDecoder().decode(envValue), Charsets.UTF_8)
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
