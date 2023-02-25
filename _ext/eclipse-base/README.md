# spotless-eclipse-base

Eclipse formatters are embedded in plugins serving multiple purposes and not necessarily supporting headless builds. Hence the plugin interfaces are depending on various Eclipse plugins and services not required for the formatting purpose.

Spotless provides its own plugin framework with `com.diffplug.spotless.JarState`. This allows Spotless to use different Eclipse versions in parallel.


The `com.diffplug.gradle.spotless:spotless-eclipse-base` artifact mocks the redundant Eclipse OSGI/plugin framework for Spotless. Furthermore it provides Eclipse services adapted for Spotless's own use, which avoids for example the creation of a permanent Eclipse workspace and reduces dependencies on Eclipse modules unused by the Eclipse code formatters.

## Usage

Include the artifact in your Spotless Eclipse formatter project, where the major version must match the Eclipse core version your formatter supports. The exact default version should be selected by the **lib-extra**.
Minor versions indicate a change in the minimum Eclipse (core/resource) dependencies.
Patch versions are always backward compatible.


```Gradle
dependencies {
  compile "com.diffplug.spotless:spotless-eclipse-base:3.+"
}
```

In the constructor of your formatter, the Spotless Eclipse Framework can be configured depending on your formatter's requirements. For example the JDT formatter can be configured like:

```Java
public EclipseFormatterStepImpl(Properties settings) throws Exception {
    SpotlessEclipseFramework.setup(plugins -> {
        plugins.applyDefault();
        plugins.add(new org.eclipse.jdt.core.JavaCore());
  });
  ...
```

The framework also supports fat JARs, providing multiple plugins.
In this cases the resources required by plugins, especially the `META-INF` and plugin information, must be located in locations unique to the plugin.
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

To publish a new version, update the `_ext/eclipse-base/gradle.properties` appropriately and see [CONTRIBUTING.md](../../CONTRIBUTING.md) how to enable
`_ext` projects.

## License

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.
