# Contributing to Spotless

Pull requests are welcome, preferably against `master`.

## Build instructions

It's a bog-standard gradle build.

`gradlew eclipse`
* creates an Eclipse project file for you.

`gradlew build`
* builds the jar
* runs the tests

Weirdly, we can't run Spotless on Spotless at the moment. Not sure why, but I think it has something to do with the weird ivy setup - it seems like maybe the POM from the deployed Spotless artifact gets overwritten by the local gradle project definition, and then the ivy stuff screws it up.

It's easy to work around though!  Just run `SelfTest.java`, which is the equivalent to `gradlew spotlessApply`.

## License

By contributing your code, you agree to license your contribution under the terms of the APLv2: https://github.com/diffplug/spotless/blob/master/LICENSE

All files are released with the Apache 2.0 license as such:

```
Copyright 2015 DiffPlug

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
