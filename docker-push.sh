#!/usr/bin/env bash
if test $TRAVIS_BRANCH = "master"
then
    ./gradlew dockerPushImage -Dbuild.version=latest --daemon || travis_terminate 1
fi
if test -z $TRAVIS_TAG
then
    ./gradlew dockerPushImage -Dbuild.version=$TRAVIS_TAG --daemon || travis_terminate 1
fi