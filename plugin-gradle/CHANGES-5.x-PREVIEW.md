We will soon be releasing `com.diffplug.spotless`, which is a drop-in replacement for `com.diffplug.gradle.spotless`, except that it drops all deprecated functionality and raises the minimum required Gradle to `5.4`.

For now, you can access this plugin by using `com.diffplug.gradle.spotless` and adding `-PspotlessModern=true` to the CLI.

* We now calculate incremental builds using the new `InputChanges` rather than the deprecated `IncrementalTaskInputs`. ([#607](https://github.com/diffplug/spotless/pull/607))
* We now use Gradle's config avoidance APIs. ([#617](https://github.com/diffplug/spotless/pull/617))
* Spotless no longer creates any tasks eagerly. ([#622](https://github.com/diffplug/spotless/pull/622))
* **BREAKING** `-PspotlessFiles` (which was deprecated) has been removed. ([#624](https://github.com/diffplug/spotless/pull/624))
* **BREAKING** The closures inside each format specification are now executed lazily on task configuration. ([#618](https://github.com/diffplug/spotless/pull/618))

```groovy
String isEager = 'nope'
spotless {
    java {
        isEager = 'yup'
    }
}
println 'isEager ' isEager
// 'com.diffplug.gradle.spotless' -> isEager yup
// 'com.diffplug.spotless'        -> isEager nope
```
