import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.SpotBugsTask

plugins {
  id("java")
  id("com.github.spotbugs")
  id("spotless.error-prone")
  id("spotless.spotless-conventions")
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = Charsets.UTF_8.name()
  options.release = libs.versions.jdk.release.map { it.toInt() }
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
  outputs.file(project.layout.buildDirectory.file("reports/spotbugs/${name}.html"))
  outputs.file(project.layout.buildDirectory.file("spotbugs/auxclasspath/${name}"))
  reports.create("html") {
    required = true
  }
}

dependencies {
  compileOnly(libs.jcip.annotations)
  compileOnly(spotbugs.toolVersion.map { "com.github.spotbugs:spotbugs-annotations:$it" })
  compileOnly(libs.jsr305)
}
