import com.diffplug.spotless.changelog.gradle.ChangelogExtension
import com.github.spotbugs.snom.Confidence

plugins {
  `java-library`
  alias(libs.plugins.p2deps)
  id("spotless.java-setup")
  id("spotless.java-publish")
  id("spotless.special-tests")
}

extra["artifactId"] = property("artifactIdLibExtra")

version = rootProject.the<ChangelogExtension>().versionNext

dependencies {
  api(projects.lib)
  // misc useful utilities
  implementation(libs.durian.core)
  implementation(libs.durian.collect)
  // needed by GitAttributesLineEndings
  implementation(libs.jgit)
  implementation(libs.concurrent.trees)
  // for eclipse
  implementation(libs.solstice)
  // the osgi dep is included in solstice, but it has some CVE's against it.
  // 3.18.500 is the oldest, most-compatible version with no CVE's
  // https://central.sonatype.com/artifact/org.eclipse.platform/org.eclipse.osgi/versions
  implementation(libs.osgi)

  // testing
  testImplementation(projects.testlib)
  testImplementation(libs.durian.io)
  testImplementation(libs.jsr305)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testImplementation(libs.durian.testlib)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spotless {
  java {
    replaceRegex(
        "enforceSolsticeVersion",
        "\"dev.equo.ide:solstice:(.*)\"",
        "\"${libs.solstice.get()}\"",
    )
  }
}

val needsP2Deps =
    listOf(
        // (alphabetic order please)
        "cdt",
        "groovy",
        "jdt",
    )

val mainSourceSet = sourceSets.named("main")

for (needsP2 in needsP2Deps) {
  sourceSets.register(needsP2) {
    compileClasspath += mainSourceSet.get().output
    runtimeClasspath += mainSourceSet.get().output
    java {}
  }
  dependencies { add("${needsP2}CompileOnly", libs.solstice) }
}

tasks.jar {
  for (needsP2 in needsP2Deps) {
    from(sourceSets.named(needsP2).map { it.output.classesDirs })
  }
}

tasks.withType<Test>().configureEach {
  // needed for EclipseCdtFormatterStepTest
  jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
  dependsOn(tasks.jar)
  classpath += tasks.jar.get().outputs.files
  // P2 tests use a process-level file lock; parallel forks race for it.
  // In-JVM synchronization in TestP2Provisioner is enough, so one fork is sufficient.
  maxParallelForks = 1
}

p2deps {
  into("cdtCompileOnly") {
    p2repo("https://download.eclipse.org/eclipse/updates/4.26/")
    p2repo("https://download.eclipse.org/tools/cdt/releases/11.0/")
    install("org.eclipse.cdt.core")
  }
  into("groovyCompileOnly") {
    p2repo("https://download.eclipse.org/eclipse/updates/4.26/")
    p2repo(
        "https://groovy.jfrog.io/artifactory/plugins-release/org/codehaus/groovy/groovy-eclipse-integration/4.8.0/e4.26/"
    )
    install("org.codehaus.groovy.eclipse.refactoring")
    install("org.codehaus.groovy.eclipse.core")
    install("org.eclipse.jdt.groovy.core")
    install("org.codehaus.groovy")
  }
  into("jdtCompileOnly") {
    p2repo("https://download.eclipse.org/eclipse/updates/4.26/")
    install("org.eclipse.jdt.core")
  }
}

// Embedded Eclipse JDT lockfiles, see EclipseJdtLockfileMetadataTool. `verify` runs in CI so the
// lockfiles cannot silently drift from Eclipse's P2 metadata; `update` is run by hand whenever the
// default version moves. Both hit the Eclipse update sites, so neither is wired into `build`.
fun eclipseJdtLockfileTool(entrypoint: String): Action<JavaExec> = Action {
  group = "verification"
  classpath = sourceSets.named("test").get().runtimeClasspath
  mainClass = "com.diffplug.spotless.extra.java.EclipseJdtLockfileMetadataTool\$$entrypoint"
  workingDir = rootProject.projectDir
  outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("verifyEclipseJdtLockfiles") {
  eclipseJdtLockfileTool("Verify").execute(this)
  description = "Checks the embedded Eclipse JDT lockfiles against Eclipse P2 metadata."
}

tasks.register<JavaExec>("updateEclipseJdtLockfiles") {
  eclipseJdtLockfileTool("Update").execute(this)
  description = "Regenerates the embedded Eclipse JDT lockfiles from Eclipse P2 metadata."
}

// we'll hold the core lib to a high standard
spotbugs {
  // LOW|MEDIUM|DEFAULT|HIGH (low = sensitive to even minor mistakes).
  reportLevel = Confidence.LOW
  excludeFilter = file("spotbugs-exclude.xml")
}
