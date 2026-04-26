#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF)
# Maven wrapper shell script
# ----------------------------------------------------------------------------

if [ -z "$JAVA_HOME" ]; then
  JAVACMD=java
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

if [ -r "$MAVEN_WRAPPER_PROPERTIES" ]; then
  DISTRIBUTION_URL=$(grep "distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2)
fi

MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"
MAVEN_WRAPPER_HOME="$MAVEN_USER_HOME/wrapper/dists"

DIST_NAME=$(basename "$DISTRIBUTION_URL" .zip)
DIST_DIR="$MAVEN_WRAPPER_HOME/$DIST_NAME"

if [ ! -d "$DIST_DIR" ]; then
  mkdir -p "$DIST_DIR"
  echo "Downloading Maven $DIST_NAME..."
  if command -v curl > /dev/null; then
    curl -fsSL "$DISTRIBUTION_URL" -o "$DIST_DIR/$DIST_NAME.zip"
  elif command -v wget > /dev/null; then
    wget -q "$DISTRIBUTION_URL" -O "$DIST_DIR/$DIST_NAME.zip"
  else
    echo "Error: curl or wget is required to download Maven." >&2
    exit 1
  fi
  unzip -q "$DIST_DIR/$DIST_NAME.zip" -d "$DIST_DIR"
fi

MAVEN_HOME="$DIST_DIR/${DIST_NAME%-bin}"
exec "$MAVEN_HOME/bin/mvn" "$@"
