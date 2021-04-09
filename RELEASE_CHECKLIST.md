# Manual Release checklist

We now do this automatically in CI.

- [ ] Revise [`CHANGES.md`](CHANGES.md), [`plugin-gradle/CHANGES.md`](plugin-gradle/CHANGES.md), and [`plugin-maven/CHANGES.md`](plugin-maven/CHANGES.md)
- [ ] If necessary, release lib `./gradlew :changelogPush`
- [ ] Release the plugins one at a time `./gradlew :plugin-XXX:changelogPush`
- [ ] Test latest spotless on:
    - Gradle via Apache beam at `v2.13.0`: https://github.com/apache/beam
        - bump the spotless version in [`buildSrc/build.gradle`](https://github.com/apache/beam/blob/28fad69d43de08e8419d421bd8bfd823a327abb7/buildSrc/build.gradle#L23)
        - `./gradlew spotlessApply -PdisableSpotlessCheck=true`
    - Maven via JUNG at `bf7e5b9`: https://github.com/jrtom/jung
        - bump the spotless version in [`pom.xml`](https://github.com/jrtom/jung/blob/bf7e5b91340e3f703ad1bc5ffe4abc922bd712a4/pom.xml#L82)
        - `mvn spotless:apply -U`
        - might take a while for mavencentral to update, the `-U` flag above ensures that it tries again rather than caching a failure
- [ ] Comment on all released PRs / issues
