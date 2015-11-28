#!/bin/bash

export JAVA_HOME=/usr/local/jdk
export APP_HOME=/home/joeuser/Parallel_SSH/classes

$JAVA_HOME/bin/java -cp $APP_HOME RunScriptOnAllNodes $*
