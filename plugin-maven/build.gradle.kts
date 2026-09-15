plugins {
  alias(libs.plugins.maven.plugin.development)
  id("spotless.changelog")
  id("spotless.java-setup")
  id("spotless.spotless-freshmark")
  id("spotless.special-tests")
  id("spotless.java-publish")
}

version = spotlessChangelog.versionNext

mavenPlugin {
  name = property("POM_NAME").toString()
  artifactId = property("POM_ARTIFACT_ID").toString()
  description = property("POM_DESCRIPTION").toString()
}

mavenPublishing {
  pom {
    developers {
      developer {
        id = "lutovich"
        name = "Konstantin Lutovich"
        email = "konstantin.lutovich@neotechnology.com"
      }
    }
    withXml {
      val rootNode = asNode()
      val prerequisites = rootNode.appendNode("prerequisites")
      prerequisites.appendNode("maven", "3.1.0")
    }
  }
}

dependencies {
  implementation(projects.lib)
  implementation(projects.libExtra)

  compileOnly(pinnedLibs.maven.plugin.api)
  compileOnly(pinnedLibs.maven.plugin.annotations)
  compileOnly(pinnedLibs.maven.core)
  compileOnly(pinnedLibs.aether.api)

  compileOnly(libs.jakarta.annotation.api)

  implementation(libs.durian.core)
  implementation(libs.durian.io)
  implementation(libs.durian.collect)
  implementation(libs.plexus.resources)
  implementation(libs.plexus.utils)
  implementation(libs.jgit)
  implementation(pinnedLibs.plexus.build.api)

  testImplementation(projects.testlib)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testImplementation(libs.mockito.core)
  testImplementation(libs.durian.io)
  testImplementation(libs.mustache.compiler)
  testImplementation(libs.owasp.encoder)
  testImplementation(pinnedLibs.maven.plugin.api)
  testImplementation(pinnedLibs.aether.api)
  testImplementation(libs.plexus.resources)
  testImplementation(pinnedLibs.maven.core)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
  systemProperty("spotlessMavenPluginVersion", project.version)
  systemProperty("spotlessProjectDir", "${project.rootProject.projectDir}")
  dependsOn(tasks.publishToMavenLocal)
  dependsOn(":lib:publishToMavenLocal")
  dependsOn(":lib-extra:publishToMavenLocal")
}
