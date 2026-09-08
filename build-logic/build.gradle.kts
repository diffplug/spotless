plugins {
  `kotlin-dsl`
}

val versionCatalog = versionCatalogs.named("libs")

fun plugin(alias: String): String {
  val p = versionCatalog.findPlugin(alias).get().get()
  return "${p.pluginId}:${p.pluginId}.gradle.plugin:${p.version}"
}

dependencies {
  implementation(plugin("spotless"))
  implementation(plugin("spotless-changelog"))
  implementation("com.diffplug.spotless-changelog:spotless-changelog-lib:3.1.2")
  implementation(plugin("spotbugs"))
  implementation(plugin("errorprone"))
  implementation(plugin("rewrite"))
  implementation(plugin("test-logger"))
  implementation(plugin("nexus-publish"))
  implementation(plugin("develocity"))

  // TODO: https://github.com/gradle/gradle/issues/15383
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
