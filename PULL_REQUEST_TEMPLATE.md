## DO NOT FORCE PUSH

Please **DO NOT FORCE PUSH**. Don't worry about messy history, it's easier to do code review if we can tell what happened after the review, and force pushing breaks that.

Please make sure that your [PR allows edits from maintainers](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/allowing-changes-to-a-pull-request-branch-created-from-a-fork).  Sometimes it's faster for us to just fix something than it is to describe how to fix it.

<img src="https://docs.github.com/assets/cb-87454/images/help/pull_requests/allow-edits-and-access-by-maintainers.png" height="297" alt="Allow edits from maintainers">

After creating the PR, please add a commit that adds a bullet-point under the `[Unreleased]` section of [CHANGES.md](https://github.com/diffplug/spotless/blob/main/CHANGES.md), [plugin-gradle/CHANGES.md](https://github.com/diffplug/spotless/blob/main/plugin-gradle/CHANGES.md), and [plugin-maven/CHANGES.md](https://github.com/diffplug/spotless/blob/main/plugin-maven/CHANGES.md) which includes:

- [ ] a summary of the change
- either
    - [ ] a link to the issue you are resolving (for small changes)
    - [ ] a link to the PR you just created (for big changes likely to have discussion)

If your change only affects a build plugin, and not the lib, then you only need to update the `plugin-foo/CHANGES.md` for that plugin.

If your change affects lib in an end-user-visible way (fixing a bug, updating a version) then you need to update `CHANGES.md` for both the lib and all build plugins.  Users of a build plugin shouldn't have to refer to lib to see changes that affect them.

This makes it easier for the maintainers to quickly release your changes :)

## Check and format code

Before creating a pull request, you might want to format (yes, spotless is  formatted by spotless)
the code and check for possible bugs

* Error Prone 🚧
  * `./gradlew assemble -Derror-prone=true`
* OpenRewrite ☑️
  * `./gradlew rewriteRun`
* Spotless ✨
  * `./gradlew spotlessApply`
* Spotbugs 🐞
  * `./gradlew spotbugsMain`
