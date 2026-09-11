import com.github.spotbugs.snom.Confidence

plugins {
  `java-library`
  alias(libs.plugins.buildconfig)
  alias(libs.plugins.version.compatibility)
  id("spotless.java-setup")
  id("spotless.java-publish")
  id("spotless.special-tests")
}

extra["artifactId"] = property("artifactIdLib")

version = spotlessChangelog.versionNext

val Provider<MinimalExternalModuleDependency>.version: Provider<String>
  get() = map { it.version }

buildConfig {
  packageName("com.diffplug.spotless.java")
  useJavaOutput {
    defaultVisibility = true
  }
  buildConfigField("VERSION_CLEANTHAT", libs.cleanthat.java.version)
  buildConfigField("VERSION_GJF", libs.google.java.format.version)
  buildConfigField("VERSION_JAVAPARSER", libs.javaparser.symbol.solver.core.version)
  buildConfigField("VERSION_PALANTIR_JAVA_FORMAT", libs.palantir.java.format.default.version)
  buildConfigField("VERSION_PRINCE_OF_SPACE", libs.prince.of.space.core.version)
  buildConfigField("VERSION_TABLETEST_FORMATTER", libs.tabletest.formatter.core.version)

  forClass("com.diffplug.spotless.antlr4", "Antlr4BuildConfig") {
    buildConfigField("VERSION_ANTLR4", libs.antlr4.formatter.version)
  }
  forClass("com.diffplug.spotless.asciidoc", "AsciidocBuildConfig") {
    buildConfigField("VERSION_ADOCFMT", libs.adocfmt.version)
  }
  forClass("com.diffplug.spotless.gherkin", "GherkinBuildConfig") {
    buildConfigField("VERSION_GHERKIN_UTILS", libs.gherkin.utils.version)
  }
  forClass("com.diffplug.spotless.json", "JsonBuildConfig") {
    buildConfigField("VERSION_JACKSON", libs.jackson.databind.version)
    buildConfigField("VERSION_JSON_SIMPLE", libs.json.simple.version)
    buildConfigField("VERSION_ZJSONPATCH", libs.zjsonpatch.version)
  }
  forClass("com.diffplug.spotless.json.gson", "GsonBuildConfig") {
    buildConfigField("VERSION_GSON", libs.gson.version)
  }
  forClass("com.diffplug.spotless.kotlin", "KotlinBuildConfig") {
    buildConfigField("VERSION_DIKTAT", libs.diktat.runner.version)
    buildConfigField("VERSION_KTFMT", libs.ktfmt.version)
    buildConfigField("VERSION_KTLINT", libs.ktlint.rule.engine.version)
  }
  forClass("com.diffplug.spotless.markdown", "MarkdownBuildConfig") {
    buildConfigField("VERSION_FLEXMARK", libs.flexmark.all.version)
    buildConfigField("VERSION_FRESHMARK", libs.freshmark.version)
  }
  forClass("com.diffplug.spotless.pom", "PomBuildConfig") {
    buildConfigField("VERSION_SORTPOM", libs.sortpom.sorter.version)
  }
  forClass("com.diffplug.spotless.rdf", "RdfBuildConfig") {
    buildConfigField("VERSION_COOL_RDF_FORMATTER", libs.cool.rdf.formatter.version)
  }
  forClass("com.diffplug.spotless.scala", "ScalaBuildConfig") {
    buildConfigField("VERSION_SCALAFMT", libs.scalafmt.core.version)
  }
  forClass("com.diffplug.spotless.yaml", "YamlBuildConfig") {
    buildConfigField("VERSION_JACKSON", libs.jackson.dataformat.yaml.version)
  }
}

val needsGlue =
    listOf(
        // (alphabetic order please)
        "cleanthat",
        "diktat",
        "flexmark",
        "gherkin",
        "googleJavaFormat",
        "gson",
        "jackson",
        "javaParser",
        "ktfmt",
        "ktlint",
        "lombokStubs",
        "palantirJavaFormat",
        "princeOfSpace",
        "scalafmt",
        "sortPom",
        "tableTestFormatter",
        "zjsonPatch",
    )

val mainSourceSet = sourceSets.named("main")

for (glue in needsGlue) {
  sourceSets.register(glue) {
    compileClasspath += mainSourceSet.get().output
    runtimeClasspath += mainSourceSet.get().output
    java {}
  }
}

versionCompatibility {
  adapters {
    // (alphabetic order please)
    namespaces.register("Cleanthat") {
      versions = listOf("2.1")
      targetSourceSetName = "cleanthat"
    }
    namespaces.register("KtLint") {
      // as discussed at https://github.com/diffplug/spotless/pull/1475
      // we will support no more than 2 breaking changes at a time = 3 incompatible versions
      // we will try to drop down to only one version if a stable API can be maintained for a full
      // year
      versions = listOf("1.0.0")
      targetSourceSetName = "ktlint"
    }
    namespaces.register("Diktat") {
      versions = listOf("1.2.5", "2.0.0")
      targetSourceSetName = "diktat"
    }
  }
}

tasks.check {
  dependsOn(tasks.named("testCompatibilityAdapters"))
  dependsOn(tasks.named("testCompatibility"))
}

dependencies {
  compileOnly(libs.slf4j.api)
  "testCommonImplementation"(libs.slf4j.api)

  // zero runtime reqs is a hard requirements for spotless-lib
  // if you need a dep, put it in lib-extra
  "testCommonImplementation"(libs.junit.jupiter)
  "testCommonImplementation"(libs.assertj.core)
  "testCommonImplementation"(libs.durian.testlib)
  "testCommonImplementation"(projects.testlib)
  "testCommonRuntimeOnly"("org.junit.platform:junit-platform-launcher")

  // GLUE CODE (alphabetic order please)
  // cleanthat
  "cleanthatCompileOnly"(libs.cleanthat.java)
  "compatCleanthat2Dot1CompileAndTestOnly"(libs.cleanthat.java)
  // diktat old supported version 1.x
  "compatDiktat1Dot2Dot5CompileOnly"(libs.diktat.rules)
  // diktat latest supported version 2.x
  "compatDiktat2Dot0Dot0CompileOnly"(libs.diktat.runner)
  // flexmark
  "flexmarkCompileOnly"(libs.flexmark.all)
  // gherkin
  "gherkinCompileOnly"(libs.gherkin.utils)
  "gherkinCompileOnly"(libs.slf4j.api)
  // googleJavaFormat
  "googleJavaFormatCompileOnly"(libs.google.java.format)
  // gson
  "gsonCompileOnly"(libs.gson)
  // jackson
  "jacksonCompileOnly"(libs.jackson.databind)
  "jacksonCompileOnly"(libs.jackson.dataformat.yaml)
  // javaParser
  "javaParserCompileOnly"(libs.javaparser.symbol.solver.core)
  // ktfmt
  "ktfmtCompileOnly"(libs.ktfmt)
  "ktfmtCompileOnly"("com.google.googlejavaformat:google-java-format") {
    version {
      strictly("1.7") // for JDK 8 compatibility
    }
  }
  "ktfmtCompileOnly"(libs.jsr305)
  // ktlint latest supported version
  "compatKtLint1Dot0Dot0CompileAndTestOnly"(libs.ktlint.rule.engine)
  "compatKtLint1Dot0Dot0CompileAndTestOnly"(libs.ktlint.ruleset.standard)
  "compatKtLint1Dot0Dot0CompileAndTestOnly"(libs.slf4j.api)
  // palantirJavaFormat
  "palantirJavaFormatCompileOnly"(
      libs.palantir.java.format
  ) // this version needs to stay compilable against Java 8 for CI Job testNpm
  // princeOfSpace
  "princeOfSpaceCompileOnly"(libs.prince.of.space.core)
  // scalafmt
  "scalafmtCompileOnly"(libs.scalafmt.core)
  // sortPom
  "sortPomCompileOnly"(libs.sortpom.sorter)
  "sortPomCompileOnly"(libs.slf4j.api)
  // tableTestFormatter
  "tableTestFormatterCompileOnly"(libs.tabletest.formatter.core)
  // zjsonPatch
  "zjsonPatchCompileOnly"(libs.zjsonpatch)
}

// we'll hold the core lib to a high standard
spotbugs {
  // LOW|MEDIUM|DEFAULT|HIGH (low = sensitive to even minor mistakes).
  reportLevel = Confidence.LOW
  excludeFilter = file("spotbugs-exclude.xml")
}

tasks.withType<Test>().configureEach {
  jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
  failOnNoDiscoveredTests = false
}

tasks.jar {
  for (glue in needsGlue) {
    from(sourceSets.getByName(glue).output.classesDirs)
  }
}
