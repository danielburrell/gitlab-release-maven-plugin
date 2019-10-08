#!/usr/bin/env bash

echo "Welcome to the Mestimate Release tool"
echo "Handing over to mvn release:clean release:prepare"
mvn release:clean release:prepare release:perform
