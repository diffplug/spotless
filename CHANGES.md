# Spotless releases

### Version 1.4.0-SNAPSHOT - TBD ([oss snapshots](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/gradle/spotless/spotless/))

* Added a warning when the Eclipse formatter xml has more than one profile.

### Version 1.3.3 - March 10th 2016 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.3.3/view))

* Upgraded Eclipse formatter to 4.5.2, which fixes [37 bugs](https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&resolution=FIXED&short_desc=%5Bformatter%5D&short_desc_type=allwordssubstr&target_milestone=4.5.1&target_milestone=4.5.2) compared to the previous 4.5.0.
* If you have been using `custom 'Lambda fix', { it.replace('} )', '})').replace('} ,', '},') }`, you don't need it anymore.

### Version 1.3.2 - December 17th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.3.2/view))

* Spotless no longer clobbers package-info.java, fixes [#1](https://github.com/diffplug/spotless/issues/1).
* Added some infrastructure which allows `FormatterStep`s to peek at the filename if they really need to.

### Version 1.3.1 - September 22nd 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.3.1/view))

* Bumped the FreshMark dependency to 1.3.0, because it offers improved error reporting.

### Version 1.3.0 - September 22nd 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.3.0/view))

* Added native support for FreshMark.

### Version 1.2.0 - August 25th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.2.0/view))

* Updated from Eclipse 4.5 M6 to the official Eclipse 4.5 release, which fixes several bugs in the formatter.
* Fixed a bug in the import sorter which made it impossible to deal with "all unmatched type imports".
* Formatting is now relative to the project directory rather than the root directory.
* Improved the logging levels.

### Version 1.1 - May 14th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.1/view))

* No functional changes, probably not worth the time for an upgrade.
* First version which is available on plugins.gradle.org as well as jcenter.
* Removed some code that was copy-pasted from Durian, and added a Durian dependency.

### Version 1.0 - April 29th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.0/view))

* Initial release.

### Version 0.1 - April 28th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/0.1/view))

* First release, to test out that we can release to jcenter and whatnot.
