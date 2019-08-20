#!/bin/bash

# First parse the gradle version from its gradle-wrapper.properties file
GRADLE_WRAPPER_PROPERTIES_FILE=gradle/wrapper/gradle-wrapper.properties
GRADLE_URL_PREFIX="https\://services.gradle.org/distributions/gradle-"
GRADLE_URL_SUFFIX="-all.zip"

function prop {
    grep "${1}" ${GRADLE_WRAPPER_PROPERTIES_FILE}|cut -d'=' -f2
}

GRADLE_VERSION_URL=$(prop "distributionUrl")
GRADLE_VERSION_STRIPPED_PREFIX=${GRADLE_VERSION_URL#"$GRADLE_URL_PREFIX"}
GRADLE_VERSION=${GRADLE_VERSION_STRIPPED_PREFIX%"$GRADLE_URL_SUFFIX"}

# Now compare against gradle's distribution upstream with sha256sum
echo "Checking Gradle wrapper jar for version: ${GRADLE_VERSION}"
cd gradle/wrapper
curl --location --output gradle-wrapper.jar.sha256 \
       https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-wrapper.jar.sha256
echo "  gradle-wrapper.jar" >> gradle-wrapper.jar.sha256
sha256sum --check gradle-wrapper.jar.sha256
if [[ $? != 0 ]]; then
    echo "Gradle wrapper failed checksum verification. Please investigate." >&2
    exit $?
fi
rm gradle-wrapper.jar.sha256
cd ../..
