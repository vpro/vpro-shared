#!/bin/bash

set -x
echo -e "Publishing javadoc...\n"
source ./update-javadoc.sh

git config --global user.email "travis@travis-ci.org"
git config --global user.name "travis-ci"

git add -f docs
git commit -m "Latest javadoc on successful travis build $TRAVIS_BUILD_NUMBER"
git push origin HEAD:master 

echo -e "Published Javadoc.\n"
