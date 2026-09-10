plugins {
  id("org.openrewrite.rewrite")
}

rewrite {
  activeRecipe("com.diffplug.spotless.openrewrite.SanityCheck")
  exclusion(
      listOf(
          "**.dirty.java",
          "**FormatterProperties.java",
          "**_gradle_node_plugin_example_**",
          "**idea/full.clean.java",
          "**package-info.java",
          "**testlib/src/main/resources**",
      )
  )
  isExportDatatables = true
  failOnDryRunResults = true
}

dependencies {
  rewrite(libs.rewrite.recipe.migrate.java)
  rewrite(libs.rewrite.recipe.static.analysis)
  rewrite(libs.rewrite.recipe.third.party)
}
