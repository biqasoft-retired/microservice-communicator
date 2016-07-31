#!/usr/bin/env bash

# fail on error
set -e

if [[ $TRAVIS_PULL_REQUEST == "false" ]]; then
    cd $BASE_DIR

    # semver format. for example 1.2.2-RELEASE+build.15
#    mvn build-helper:parse-version versions:set -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-RELEASE+build.$TRAVIS_JOB_ID

    # deploy to maven binary repo
    mvn package --settings $BASE_DIR/deploy/settings.xml -DperformRelease=true -Dmaven.javadoc.skip=true -Dgpg.skip=true
#    PROJECT_VERSION="`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version 2> /dev/null |grep -Ev '(^\[|Download\w+:)'`"

#    echo "PROJECT_VERSION is $PROJECT_VERSION"

# end build
    exit $?
fi