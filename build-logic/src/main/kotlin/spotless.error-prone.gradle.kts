import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("net.ltgt.errorprone")
}

// Groovy's String.toBoolean() -- what this used before the Kotlin DSL migration -- accepts "true",
// "y" and "1", ignoring case. Kotlin's toBoolean() only accepts "true", so the short forms would
// silently become no-ops.
fun String.toBooleanGroovy(): Boolean = trim().lowercase() in setOf("true", "y", "1")

tasks.withType<JavaCompile>().configureEach {
  options.errorprone {
    if (System.getenv("error-prone")?.toBooleanGroovy() == true) {
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
