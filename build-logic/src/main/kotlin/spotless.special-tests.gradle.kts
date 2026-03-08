plugins {
	id("com.adarshr.test-logger")
}

val special = listOf(
	"black",
	"buf",
	"clang",
	"gofmt",
	"idea",
	"npm",
	"shfmt"
)

val isCI = System.getenv().containsKey("CI")

tasks.withType<Test>().configureEach {
	if (isCI) {
		extensions.configure<com.gradle.develocity.agent.gradle.test.DevelocityTestConfiguration>("develocity") {
			testRetry {
				maxRetries = 2
				maxFailures = 10
			}
		}
	}

	// selfie https://selfie.dev/jvm/get-started#gradle
	environment(project.properties.filterKeys { it == "selfie" })

	inputs.files(fileTree("src/test") {
		include("**/*.ss")
	}).withPropertyName("selfieSnapshots")

	maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

tasks.named<Test>("test") {
	useJUnitPlatform {
		excludeTags(*special.toTypedArray())
	}
}

special.forEach { tag ->
	tasks.register<Test>("test${tag.replaceFirstChar { it.uppercase() }}") {
		useJUnitPlatform {
			includeTags(tag)
		}
		val testEnvFile = rootProject.file("testenv.properties")
		if (testEnvFile.exists()) {
			systemProperty("testenv.properties.path", testEnvFile.canonicalPath)
		}
	}
}
