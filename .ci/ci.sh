#!/bin/bash

# Do the Gradle build
./gradlew build --build-cache || exit 1
./gradlew npmTest --build-cache || exit 1
