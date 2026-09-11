import com.gradle.develocity.agent.gradle.test.DevelocityTestConfiguration

plugins {
  // this script configures the `test` task and registers Test tasks, so it needs java itself
  // rather than relying on every consumer to declare it earlier in their plugins block
  id("java")
  id("com.adarshr.test-logger")
}

// See com.diffplug.spotless.tag package for available JUnit 5 @Tag annotations
val special = listOf("black", "buf", "clang", "gofmt", "idea", "npm", "shfmt")

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
  // optional, see "Overwrite everything" there
  project.findProperty("selfie")?.let { environment("selfie", it) }
  // optional, improves up-to-date checking
  inputs.files(fileTree("src/test") { include("**/*.ss") })
  // https://docs.gradle.org/8.8/userguide/performance.html#execute_tests_in_parallel
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
