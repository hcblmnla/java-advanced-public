#!/bin/bash

#SHARED="shared"
SHARED="java-advanced-2025"

BASE="$SHARED/modules/info.kgeorgiy.java.advanced.base"
IMPLEMENTOR="$SHARED/modules/info.kgeorgiy.java.advanced.implementor"
TOOLS="$SHARED/modules/info.kgeorgiy.java.advanced.implementor.tools"
LIB="$SHARED/lib/*"

cd ..
SOLUTIONS=$(pwd)
cd ..

javadoc \
  -cp "$IMPLEMENTOR:$TOOLS:$SOLUTIONS/java-solutions:$LIB:$BASE" \
  -d "$SOLUTIONS/javadoc" \
  -author \
  -private \
  "$SOLUTIONS"/java-solutions/info/kgeorgiy/ja/serov/implementor/*.java \
  "$SOLUTIONS"/java-solutions/info/kgeorgiy/ja/serov/implementor/**/*.java \
  "$IMPLEMENTOR"/info/kgeorgiy/java/advanced/implementor/Impler.java \
  "$IMPLEMENTOR"/info/kgeorgiy/java/advanced/implementor/ImplerException.java \
  "$TOOLS"/info/kgeorgiy/java/advanced/implementor/tools/JarImpler.java
