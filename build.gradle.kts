plugins {
  alias(libs.plugins.equo.ide)
  alias(libs.plugins.plugin.publish) apply false
  alias(libs.plugins.version.compatibility) apply false
  alias(libs.plugins.maven.plugin.development) apply false
  alias(libs.plugins.p2deps) apply false
  id("spotless.nexus-publish")
  id("spotless.changelog")
  id("spotless.rewrite")
  id("spotless.spotless-freshmark")
  id("spotless.spotless-conventions")
}

equoIde {
  branding().title("Spotless").icon(file("_images/spotless_logo.png"))
  welcome().openUrl("https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md")
  gradleBuildship().autoImport(".")
}
