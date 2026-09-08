import com.diffplug.spotless.changelog.gradle.ChangelogExtension

plugins {
  alias(libs.plugins.maven.plugin.development)
  id("spotless.changelog")
  id("spotless.java-setup")
  id("spotless.spotless-freshmark")
  id("spotless.special-tests")
  id("spotless.java-publish")
}

extra["artifactId"] = property("artifactIdMaven")

version = the<ChangelogExtension>().versionNext

mavenPlugin {
  name = "Spotless Maven Plugin"
  artifactId = property("artifactIdMaven").toString()
  description = project.description
}

dependencies {
  implementation(projects.lib)
  implementation(projects.libExtra)

  compileOnly(libs.maven.plugin.api)
  compileOnly(libs.maven.plugin.annotations)
  compileOnly(libs.maven.core)
  compileOnly(libs.aether.api)

  compileOnly(libs.jakarta.annotation.api)

  implementation(libs.durian.core)
  implementation(libs.durian.io)
  implementation(libs.durian.collect)
  implementation(libs.plexus.resources)
  implementation(libs.plexus.utils)
  implementation(libs.jgit)
  implementation(libs.plexus.build.api)

  testImplementation(projects.testlib)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testImplementation(libs.mockito.core)
  testImplementation(libs.durian.io)
  testImplementation(libs.mustache.compiler)
  testImplementation(libs.owasp.encoder)
  testImplementation(libs.maven.plugin.api)
  testImplementation(libs.aether.api)
  testImplementation(libs.plexus.resources)
  testImplementation(libs.maven.core)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
  systemProperty("spotlessMavenPluginVersion", project.version)
  systemProperty("spotlessProjectDir", "${project.rootProject.projectDir}")
  dependsOn(tasks.publishToMavenLocal)
  dependsOn(":lib:publishToMavenLocal")
  dependsOn(":lib-extra:publishToMavenLocal")
}
