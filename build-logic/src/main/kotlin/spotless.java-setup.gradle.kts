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
  ignoreFailures = false // bug free or it doesn't ship!
  // LOW|MEDIUM|DEFAULT|HIGH (low = sensitive to even minor mistakes).
  reportLevel = Confidence.MEDIUM
  omitVisitors =
      listOf(
          // https://spotbugs.readthedocs.io/en/latest/detectors.html#constructorthrow
          "ConstructorThrow",
          // https://spotbugs.readthedocs.io/en/latest/detectors.html#findreturnref
          "FindReturnRef",
      )
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
