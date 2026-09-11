import com.diffplug.spotless.changelog.gradle.ChangelogExtension
import java.util.Base64
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

// getMimeDecoder, not getDecoder: the basic decoder rejects the newlines that `base64`/`openssl
// base64` leave in a wrapped secret, whereas Groovy's String.decodeBase64 (used before the Kotlin
// DSL migration) skipped whitespace.
fun decode64(varName: String): String {
  val envValue = System.getenv(varName) ?: return ""
  return String(Base64.getMimeDecoder().decode(envValue), Charsets.UTF_8)
}

// Groovy's String.toBoolean() -- what this used before the Kotlin DSL migration -- accepts "true",
// "y" and "1", ignoring case. Kotlin's toBoolean() only accepts "true", so the short forms would
// silently become no-ops.
fun String.toBooleanGroovy(): Boolean = trim().lowercase() in setOf("true", "y", "1")

val isCiServer: Boolean
  get() = System.getenv().containsKey("CI")
