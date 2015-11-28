#!/bin/bash

export JAVA_HOME=/usr/local/jdk
export APP_HOME=../classes

$JAVA_HOME/bin/javac -d $APP_HOME RunCommandOnAllNodes.java
