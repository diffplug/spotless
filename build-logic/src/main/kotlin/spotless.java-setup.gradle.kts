import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.SpotBugsTask

plugins {
	java
	id("com.github.spotbugs")
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = Charsets.UTF_8.toString()
	options.release = libs.versions.jdk.release.get().toInt()
}

spotbugs {
	ignoreFailures = false
	reportLevel = Confidence.MEDIUM
	omitVisitors = listOf("ConstructorThrow", "FindReturnRef")
}

tasks.spotbugsTest {
	enabled = false
}

tasks.withType<SpotBugsTask>().configureEach {
	outputs.file(layout.buildDirectory.file("reports/spotbugs/${name}.html"))
	outputs.file(layout.buildDirectory.file("spotbugs/auxclasspath/${name}"))
	reports {
		create("html") {
			required = true
		}
	}
}

dependencies {
	compileOnly(libs.jcip.annotations)
	compileOnly("com.github.spotbugs:spotbugs-annotations:${spotbugs.toolVersion.get()}")
	compileOnly(libs.jsr305)
}
