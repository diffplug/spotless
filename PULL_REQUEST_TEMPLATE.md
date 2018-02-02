After creating the PR, please add a commit that adds a bullet-point under the `-SNAPSHOT` section of [CHANGES.md](https://github.com/diffplug/spotless/blob/master/CHANGES.md) and [plugin-gradle/CHANGES.md](https://github.com/diffplug/spotless/blob/master/plugin-gradle/CHANGES.md) which includes:

- [ ] a summary of the change
- [ ] a link to the newly created PR

If your change only affects a build plugin, and not the lib, then you only need to update the `CHANGES.md` for that plugin.

If your change affects lib in an end-user-visible way (fixing a bug, updating a version) then you need to update `CHANGES.md` for both the lib and the build plugins.  Users of a build plugin shouldn't have to refer to lib to see changes that affect them.

This makes it easier for the maintainers to quickly release your changes :)
