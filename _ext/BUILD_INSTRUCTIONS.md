
The `_ext` builds are currently tested only with gradle `4.4.1`.  They are known not to work for the current wrapper version.

To install the correct version using [sdkman](https://sdkman.io/install):

- `sdk install gradle 4.4.1`
- `sdk default gradle 4.4.1`

Then you can do `gradle -b _ext/{DIR}/build.gradle publish`.  `gradlew` will not work.
