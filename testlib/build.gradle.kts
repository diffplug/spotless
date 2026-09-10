import com.github.spotbugs.snom.Confidence

plugins {
  `java-library`
  id("spotless.java-setup")
  id("spotless.special-tests")
}

extra["artifactId"] = property("artifactIdTestLib")

version = spotlessChangelog.versionNext

dependencies {
  api(projects.lib)
  api(libs.durian.core)
  api(libs.durian.testlib)
  api(libs.junit.jupiter)
  api(libs.assertj.core)
  api(libs.mockito.core)
  api(libs.selfie.lib)
  api(libs.selfie.runner.junit5)
  runtimeOnly("org.junit.platform:junit-platform-launcher")

  implementation(libs.durian.io)
  implementation(libs.durian.collect)
  implementation(libs.jgit)
  implementation(gradleTestKit())
}

// we'll hold the testlib to a low standard (prize brevity)
spotbugs {
  // LOW|MEDIUM|DEFAULT|HIGH (low = sensitive to even minor mistakes).
  reportLevel = Confidence.HIGH
}

tasks.withType<Test>().configureEach {
  // for Antlr4FormatterStepTest, KtfmtStepTest, and KtLintStepTest
  jvmArgs(
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
  )
}

tasks.withType<Javadoc>().configureEach {
  options {
    (this as? StandardJavadocDocletOptions)?.apply {
      encoding = Charsets.UTF_8.name()
      addStringOption("Xdoclint:none", "-quiet")
      addStringOption("Xwerror", "-quiet")
    }
  }
}
