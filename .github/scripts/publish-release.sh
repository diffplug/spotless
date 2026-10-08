#!/usr/bin/env bash
set -euo pipefail

case "${1:-}" in
  lib)
    changelog_prefix=:
    publish_tasks=(:lib:publishToMavenCentral :lib-extra:publishToMavenCentral)
    ;;
  plugin-gradle)
    changelog_prefix=:plugin-gradle:
    publish_tasks=(:plugin-gradle:publishPlugins :plugin-gradle:publishToMavenCentral)
    ;;
  plugin-maven)
    changelog_prefix=:plugin-maven:
    publish_tasks=(:plugin-maven:publishToMavenCentral)
    ;;
  *)
    echo 'Usage: publish-release.sh {lib|plugin-gradle|plugin-maven}' >&2
    exit 2
    ;;
esac

gradle_args=(-Prelease=true -PmavenCentralDeploymentValidation=PUBLISHED
  --stacktrace --warning-mode all --no-configuration-cache --no-scan)
if [[ "$1" == plugin-gradle ]]; then
  gradle_args+=("-Pgradle.publish.key=${GRADLE_KEY:?}" "-Pgradle.publish.secret=${GRADLE_SECRET:?}")
fi

# Central uploads happen at the end of a successful Gradle build. Finish that
# build, including waiting for PUBLISHED, before mutating the changelog or Git.
./gradlew "${changelog_prefix}changelogInternalPushWillRun" "${changelog_prefix}changelogCheck" \
  "${publish_tasks[@]}" "${gradle_args[@]}"

# The artifacts are already published. Exclude the publishing dependencies from
# the changelog build so it cannot upload them again or defer publication to its end.
exclude_tasks=()
for task in "${publish_tasks[@]}"; do
  exclude_tasks+=(-x "$task")
done
./gradlew "${changelog_prefix}changelogPush" -PpublicationConfirmed=true \
  "${exclude_tasks[@]}" "${gradle_args[@]}"
