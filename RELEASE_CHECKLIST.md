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
  ./gradlew publishGhPages
```

- [ ] Test latest spotless on:
    - Gradle: https://github.com/junit-team/junit-lambda/blob/151d52ffab07881de71a8396a9620f18072c65ec/build.gradle#L86-L101
        - `../beam/buildSrc/build.gradle`
        - `./gradlew spotlessApply`
    - Maven: https://github.com/jrtom/jung/blob/b3a2461b97bb3ab40acc631e21feef74976489e4/pom.xml#L187-L208
        - `../jung/pom.xml`
        - `mvn spotless:apply`
        - (might take a while for mavencentral to update)
- [ ] Tag the releases
- [ ] Bump `gradle.properties` to next snapshot, run `spotlessApply`, commit any changees
- [ ] Comment on all released PRs / issues
