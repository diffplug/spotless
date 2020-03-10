Please make sure that your [PR allows edits from maintainers](https://help.github.com/articles/allowing-changes-to-a-pull-request-branch-created-from-a-fork/).  Sometimes its faster for us to just fix something than it is to describe how to fix it.

![Allow edits from maintainers](https://help.github.com/assets/images/help/pull_requests/allow-maintainers-to-make-edits-sidebar-checkbox.png)

After creating the PR, please add a commit that adds a bullet-point under the `-SNAPSHOT` section of [CHANGES.md](https://github.com/diffplug/spotless/blob/master/CHANGES.md), [plugin-gradle/CHANGES.md](https://github.com/diffplug/spotless/blob/master/plugin-gradle/CHANGES.md), and [plugin-maven/CHANGES.md](https://github.com/diffplug/spotless/blob/master/plugin-maven/CHANGES.md) which includes:

- [ ] a summary of the change
- either
    - [ ] a link to the issue you are resolving (for small changes)
    - [ ] a link to the PR you just created (for big changes likely to have discussion)

If your change only affects a build plugin, and not the lib, then you only need to update the `CHANGES.md` for that plugin.

If your change affects lib in an end-user-visible way (fixing a bug, updating a version) then you need to update `CHANGES.md` for both the lib and the build plugins.  Users of a build plugin shouldn't have to refer to lib to see changes that affect them.

This makes it easier for the maintainers to quickly release your changes :)
