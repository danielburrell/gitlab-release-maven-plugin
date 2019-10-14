#!/usr/bin/env bash

echo "Welcome to the GitLab Release tool"
echo "Handing over to mvn release:clean release:prepare"
mvn release:clean release:prepare release:perform
