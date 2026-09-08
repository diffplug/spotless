import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("net.ltgt.errorprone")
}

tasks.withType<JavaCompile>().configureEach {
  options.errorprone {
    if (System.getenv("error-prone")?.toBoolean() == true) {
      enable()
    } else {
      disable()
    }
    disableAllWarnings = true
    disable(
        "AnnotateFormatMethod",
        "FunctionalInterfaceMethodChanged",
        "ImmutableEnumChecker",
        "InlineMeSuggester",
        "JavaxInjectOnAbstractMethod",
        "OverridesJavaxInjectableMethod",
        "ReturnValueIgnored",
    )
    error(
        "ReturnValueIgnored",
        "SelfAssignment",
        "StringJoin",
        "UnnecessarilyFullyQualified",
        "UnnecessaryLambda",
    )
    excludedPaths = ".*/GradleIntegrationHarness.java"
  }
}

dependencies {
  errorprone(libs.errorprone.core)
}
