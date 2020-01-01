#!/bin/bash

# Do the Gradle build
./gradlew build --build-cache || exit 1
./gradlew npmTest --build-cache || exit 1

if [ "$TRAVIS_REPO_SLUG" == "diffplug/spotless" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
	# Make sure that all pom are up-to-date
	./gradlew generatePomFileForPluginMavenPublication --build-cache
	# Publish the artifacts
	./gradlew publish publishPlugins -Dgradle.publish.key=$gradle_key -Dgradle.publish.secret=$gradle_secret --build-cache || exit 1
	# Push the javadoc
	./gradlew gitPublishPush --build-cache || exit 1
fi
