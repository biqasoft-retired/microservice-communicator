#!/usr/bin/env bash

# fail on error
set -e

if [[ $TRAVIS_PULL_REQUEST == "false" ]]; then
    cd $BASE_DIR
    export JAVA_TOOL_OPTIONS="-Dgpg.skip=true"
    bash <(curl -s https://codecov.io/bash)

# end build
    exit $?
fi