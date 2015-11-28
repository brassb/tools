#!/bin/bash

export JAVA_HOME=/usr/local/jdk
export APP_HOME=/home/joeuser/Parallel_SSH/classes

stty -echo
stty raw

$JAVA_HOME/bin/java -cp $APP_HOME RunCommandOnAllNodes `stty size`

stty -raw
stty echo
