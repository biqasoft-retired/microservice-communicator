#!/usr/bin/env bash

# fail on error
set -e

if [[ $TRAVIS_PULL_REQUEST == "false" ]]; then
    cd $BASE_DIR

    # semver format. for example 1.2.2-RELEASE+build.15
#    mvn build-helper:parse-version versions:set -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-RELEASE+build.$TRAVIS_JOB_ID

    # java 9 test
    if [[ $JAVA_VERSION == "openjdk9" ]]; then

    # only ubuntu 16.04 have in repo early build jdk9
    # sudo apt-get update & sudo apt install openjdk-9-jdk
    wget http://download.java.net/java/jdk9/archive/131/binaries/jdk-9-ea+131_linux-x64_bin.tar.gz
    tar -xvf jdk-9-ea+131_linux-x64_bin.tar.gz
    sudo mv jdk-9 java-9-openjdk-amd64
    sudo mv java-9-openjdk-amd64 /usr/lib/jvm
    env MAVEN_SKIP_RC="true" JAVA_HOME="/usr/lib/jvm/java-9-openjdk-amd64" JRE_HOME="/usr/lib/jvm/java-9-openjdk-amd64" mvn package -Pjdk9 -Djacoco.skip=true
    mvn clean
    fi

    if [[ $JAVA_VERSION == "openjdk8" ]]; then
    # deploy to maven binary repo
    mvn package --settings $BASE_DIR/deploy/settings.xml -DperformRelease=true -Dmaven.javadoc.skip=true -Dgpg.skip=true
    fi

#    PROJECT_VERSION="`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version 2> /dev/null |grep -Ev '(^\[|Download\w+:)'`"
#    echo "PROJECT_VERSION is $PROJECT_VERSION"

# end build
    exit $?
fi