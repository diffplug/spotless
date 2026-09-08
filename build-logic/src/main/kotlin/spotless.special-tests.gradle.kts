import com.gradle.develocity.agent.gradle.test.DevelocityTestConfiguration

plugins {
  id("com.adarshr.test-logger")
}

val special = listOf("black", "buf", "clang", "gofmt", "idea", "npm", "shfmt")

val isCiServer = System.getenv().containsKey("CI")

tasks.withType<Test>().configureEach {
  if (isCiServer) {
    configure<DevelocityTestConfiguration> {
      testRetry {
        maxRetries = 2
        maxFailures = 10
      }
    }
  }
  // selfie https://selfie.dev/jvm/get-started#gradle
  project.findProperty("selfie")?.let { environment("selfie", it) }
  inputs.files(fileTree("src/test") { include("**/*.ss") })
  maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

tasks.named<Test>("test") { useJUnitPlatform { excludeTags(*special.toTypedArray()) } }

special.forEach { tag ->
  tasks.register<Test>("test${tag.replaceFirstChar { it.uppercase() }}") {
    useJUnitPlatform { includeTags(tag) }
    val testEnv = rootProject.file("testenv.properties")
    if (testEnv.exists()) {
      systemProperty("testenv.properties.path", testEnv.canonicalPath)
    }
  }
}
