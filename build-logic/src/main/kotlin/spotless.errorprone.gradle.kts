import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorProneOptions

plugins {
	id("net.ltgt.errorprone")
}

tasks.withType<JavaCompile>().configureEach {
	(options as ExtensionAware).extensions.configure<ErrorProneOptions>("errorprone") {
		if (System.getenv("error-prone")?.toBoolean() == true) {
			enable()
		} else {
			disable()
		}
		disableAllWarnings = true
		check("AnnotateFormatMethod", CheckSeverity.OFF)
		check("FunctionalInterfaceMethodChanged", CheckSeverity.OFF)
		check("ImmutableEnumChecker", CheckSeverity.OFF)
		check("InlineMeSuggester", CheckSeverity.OFF)
		check("JavaxInjectOnAbstractMethod", CheckSeverity.OFF)
		check("OverridesJavaxInjectableMethod", CheckSeverity.OFF)
		check("ReturnValueIgnored", CheckSeverity.ERROR)
		check("SelfAssignment", CheckSeverity.ERROR)
		check("StringJoin", CheckSeverity.ERROR)
		check("UnnecessarilyFullyQualified", CheckSeverity.ERROR)
		check("UnnecessaryLambda", CheckSeverity.ERROR)
		excludedPaths = ".*/GradleIntegrationHarness.java"
	}
}

dependencies {
	errorprone(libs.errorprone.core)
}
