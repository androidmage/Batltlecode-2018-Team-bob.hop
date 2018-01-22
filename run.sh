#!/bin/sh

# Compile
echo javac $(find . -name '*.java') -classpath ../battlecode/java
javac $(find . -name '*.java') -classpath ../battlecode/java

# Run
echo java -classpath .:../battlecode/java SeedingPlayer
java -classpath .:../battlecode/java Seedinglayer
