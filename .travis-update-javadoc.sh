#!/bin/bash


echo -e "Publishing javadoc...\n"
source ./update-javadoc.sh

cd $HOME
git config --global user.email "travis@travis-ci.org"
git config --global user.name "travis-ci"
git add -f doc
git commit -m "Latest javadoc on successful travis build $TRAVIS_BUILD_NUMBER"
git push -fq origin  > /dev/null

echo -e "Published Javadoc.\n"
