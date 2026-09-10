import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("net.ltgt.errorprone")
}

tasks.withType<JavaCompile>().configureEach {
  options.errorprone {
    enabled = System.getenv("error-prone")?.toBooleanGroovy() == true
    // https://github.com/diffplug/spotless/issues/2745
    // https://github.com/google/error-prone/issues/5365
    disableAllWarnings = true
    disable(
        "AnnotateFormatMethod", // We don't want to use ErrorProne's annotations.
        "FunctionalInterfaceMethodChanged",
        "ImmutableEnumChecker", // We don't want to use ErrorProne's annotations.
        "InlineMeSuggester", // We don't want to use ErrorProne's annotations.
        "JavaxInjectOnAbstractMethod",
        "OverridesJavaxInjectableMethod",
        "ReturnValueIgnored", // We don't want to use ErrorProne's annotations.
    )
    error(
        "ReturnValueIgnored",
        "SelfAssignment",
        "StringJoin",
        "UnnecessarilyFullyQualified",
        "UnnecessaryLambda",
    )
    // bug: this only happens when the file is dirty.
    // might be an up2date (caching) issue, as file is currently in corrupt state.
    // ForbidGradleInternal(import
    excludedPaths = ".*/GradleIntegrationHarness.java"
  }
}

dependencies {
  errorprone(libs.errorprone.core)
}
