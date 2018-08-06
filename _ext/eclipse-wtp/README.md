# spotless-eclipse-wtp

Eclipse WTP is not available in a form which can be easily consumed by maven or gradle. To fix this, we publish Eclipse's WTP formatters, along with a small amount of glue code, into the `com.diffplug.spotless.extra:spotless-eclipse-wtp` artifact.

To publish a new version, update the `_ext/eclipse-wtp/gradle.properties` appropriately and run this from the root directory:

```
gradlew -b _ext/eclipse-wtp/build.gradle publish
```

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.
