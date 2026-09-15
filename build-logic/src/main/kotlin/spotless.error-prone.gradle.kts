import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("net.ltgt.errorprone")
}

// error_prone_core 2.43.0+ requires Java 21+ to run (compiled with classfile version 65.0).
val isJava21Compatible = JavaVersion.current() >= JavaVersion.VERSION_21

tasks.withType<JavaCompile>().configureEach {
  options.errorprone {
    enabled =
        isJava21Compatible &&
            (System.getProperty("error-prone")?.toBooleanGroovy() == true ||
                System.getenv("error-prone")?.toBooleanGroovy() == true)
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
  if (isJava21Compatible) {
    errorprone(libs.errorprone.core)
  }
}
