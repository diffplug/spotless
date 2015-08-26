#!/bin/bash

# Do the Gradle build
./gradlew build || exit 1

if [ "$TRAVIS_REPO_SLUG" == "diffplug/spotless" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
	# Publish the artifacts
	./gradlew publish publishPlugins -Dgradle.publish.key=$gradle.publish.key -Dgradle.publish.secret=$gradle.publish.secret || exit 1
fi
