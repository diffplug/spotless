plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spotless.changelog.lib)
    implementation(libs.errorprone.gradle.lib)
    implementation(libs.errorprone.plugin)
    implementation(libs.nexus.publish.lib)
    implementation(libs.spotbugs.gradle.lib)
    implementation(libs.rewrite.gradle.lib)
    implementation(libs.test.logger.gradle.lib)
    implementation(libs.groovy.xml)
    implementation(libs.develocity.gradle.lib)
    implementation(libs.spotless.gradle.lib)

	// TODO: https://github.com/gradle/gradle/issues/15383
	implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
