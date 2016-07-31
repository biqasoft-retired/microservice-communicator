#!/usr/bin/env bash

# fail on error
set -e

if [[ $TRAVIS_PULL_REQUEST == "false" ]]; then
    cd $BASE_DIR

    # run consul for tests
    docker run -d -p 8400:8400 -p 8500:8500 -p 8600:53/udp -h node1 progrium/consul -server -bootstrap -ui-dir /ui
    docker run -d -e "spring.cloud.consul.host=localhost" --net=host biqasoft/microservice-communicator-demo-server:latest

# end build
    exit $?
fi