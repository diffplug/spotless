# Release checklist

- [ ] Update [`CHANGES.md`](CHANGES.md), [`plugin-gradle/CHANGES.md`](plugin-gradle/CHANGES.md), and [`plugin-maven/CHANGES.md`](plugin-maven/CHANGES.md)
- [ ] Upgrade [`gradle.properties`](gradle.properties).
- [ ] Run `./gradlew spotlessApply`
- [ ] Make sure all files are committed
- [ ] Run `./gradlew check`
- [ ] Make sure all tests pass and no files are changes
- [ ] Run :

```
  ./gradlew generatePomFileForPluginMavenPublication
  ./gradlew publish publishPlugins
  ./gradlew gitPublishPush
```

- [ ] Test latest spotless on:
    - Gradle via Apache beam at `v2.13.0`: https://github.com/apache/beam
        - bump the spotless version in [`buildSrc/build.gradle`](https://github.com/apache/beam/blob/28fad69d43de08e8419d421bd8bfd823a327abb7/buildSrc/build.gradle#L23)
        - `./gradlew spotlessApply -PdisableSpotlessCheck=true`
    - Maven via JUNG at `bf7e5b9`: https://github.com/jrtom/jung
        - bump the spotless version in [`pom.xml`](https://github.com/jrtom/jung/blob/bf7e5b91340e3f703ad1bc5ffe4abc922bd712a4/pom.xml#L82)
        - `mvn spotless:apply`
        - (might take a while for mavencentral to update)
- [ ] Tag the releases
- [ ] Bump `gradle.properties` to next snapshot, run `spotlessApply`, commit any changees
- [ ] Comment on all released PRs / issues
