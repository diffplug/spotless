# spotless-eclipse-base

Eclipse formatters are part of the the Eclipse User Interface implementations. Hence their public interfaces are depending on various Eclipse modules.

Spotless provides its own plugin framework with `com.diffplug.spotless.JarState`. This allows Spotless the usage of different Eclipse versions in parallel.


The `com.diffplug.gradle.spotless:spotless-eclipse-base` artifact mocks the redundant Eclipse OSGI/plugin framework for Spotless. Furthermore it provides Eclipse services adapted for Spotless, which avoids for example the creation of a permanent workspace and reduces the dependencies on Eclipse modules unused by the Eclipse code formatters.

## Usage

Include the artifact to your Spotless Eclipse formatter project, whereas the major version must match the Eclipse core version your formatter supports. The exact default version should be selected by the **lib-extra**.
Minor versions indicate a change in the minimum Eclipse (core/resource) dependencies.
Patch versions are always backward compatible.


```Gradle
dependencies {
  compile "com.diffplug.spotless:spotless-eclipse-base:3.+"
}
```

In the constructor of your formatter, the Spotless Eclipse Framework can be configured depending on your formatters requirements. For example the JDT formatter can be configured like:

```Java
public EclipseFormatterStepImpl(Properties settings) throws Exception {
    SpotlessEclipseFramework.setup(plugins -> {
        plugins.addAll(SpotlessEclipseFramework.DefaultPlugins.createAll());
        plugins.add(new org.eclipse.jdt.core.JavaCore());
  });
  ...
```

The framework also supports fat JARs, providing multiple plugins.
In this cases the resources required by plugins, especially the `META-INF` and plugin information, must be located in locations unique
to the plugin.
For this purpose the framework expects that these resources are stored in a sub-directory
which has the name of the package containing the plugin. For example in case the JDT plugin
is included in your formatter fat JAR, the directory structure should be:

```
+ resources
|
+--+ org.eclipse.jdt.core
|  |
|  +--+ META-INF
|  |  |
|  |  +-- MANIFEST.MF
|  |
|  +--- plugin.properties
|  |
|  +--- plugin.xml

```


## Build

```
gradlew -b _ext/eclipse-base/build.gradle publish
```


## License

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.
