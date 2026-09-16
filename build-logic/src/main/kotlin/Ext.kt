import com.diffplug.spotless.changelog.gradle.ChangelogExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: LibrariesForLibs
  get() = extensions.findByType() ?: rootProject.extensions.getByType()

val Project.spotlessChangelog: ChangelogExtension
  get() = extensions.findByType() ?: rootProject.extensions.getByType()

val Project.rootSpotlessChangelog: ChangelogExtension
  get() = rootProject.extensions.getByType()

fun Project.requiredProperty(name: String): String =
    findProperty(name)?.toString()
        ?: error("$path is missing the '$name' property, which is required")

fun String.toBooleanGroovy(): Boolean = trim().lowercase() in setOf("true", "y", "1")

val isCiServer: Boolean
  get() = System.getenv().containsKey("CI")
