# spotless-eclipse-base

Eclipse formatters are part of the the Eclipse User Interface implementations. Hence their public interfaces are depending on various Eclipse modules.

Spotless provides its own plugin framework with `com.diffplug.spotless.JarState`. This allows Spotless the usage of different Eclipse versions in parallel.


The `com.diffplug.gradle.spotless:spotless-eclipse-base` artifact mocks the redundant Eclipse OSGI/plugin framework for Spotless. Furthermore it provides Eclipse services adapted for Spotless, which avoids for example the creation of a permanent workspace and reduces the dependencies on Eclipse modules unused by the Eclipse code formatters.

## Usage

Include the artifact to your Spotless Eclipse formatter project, whereas the minor/major version must match the Eclipse version your formatter supports. The exact default version should be selected by the **lib-extra**. Patch versions are backward compatible.

```Gradle
dependencies {
  compile "com.diffplug.spotless:spotless-ext-eclipse-base:4.7.+"
}
```

In the constructor of your formatter, the Spotless Eclipse Framework can be configured depending on your formatters requirements. For example the JDT formatter can be configured like:

```Java
  //Eclipse plugins can be statically created, since their constructors
  //do not require a running Eclipse environment.
  private final static Plugin[] PLUGINS = {
      SpotlessEclipseFramework.DefaultPlugins.RESOURCES.create(),
      new org.eclipse.jdt.core.JavaCore()
  };

  public EclipseFormatterStepImpl(Properties settings) throws Exception {
    SpotlessEclipseFramework.setup(PLUGINS, config -> {
      config.disableDebugging();
      config.hideEnvironment();
      config.ignoreContentType();
      config.ignoreUnsupportedPreferences();
      config.useTemporaryLocations();
    });
  ...
```

## Build

```
gradlew -b _ext/eclipse-base/build.gradle publish
```


## License

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.
