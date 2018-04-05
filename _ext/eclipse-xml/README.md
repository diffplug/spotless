# spotless-eclipse-xml

Eclipse formatter are part of the the Eclipse User Interface implementations. Hence their public interfaces are depending on various Eclipse modules. The `com.diffplug.gradle.spotless:spotless-eclipse-xml` artifact provides a wrapper interface for the Eclipse XmlFormatter, only using Java native classes. This allows an encapsulation and lazy loading of the Eclipse dependencies.

## Build

To publish a new version, update the `_ext/eclipse-xml/gradle.properties` appropriately and run this from the root directory:

```
gradlew -b _ext/eclipse-xml/build.gradle publish
```

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.
