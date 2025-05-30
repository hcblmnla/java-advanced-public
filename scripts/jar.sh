#!/bin/bash

#SHARED="shared"
SHARED="java-advanced-2025"

IMPLEMENTOR="$SHARED/modules/info.kgeorgiy.java.advanced.implementor"
TOOLS="$SHARED/modules/info.kgeorgiy.java.advanced.implementor.tools"

cd ..
SOLUTIONS=$(pwd)
cd ..

javac \
  -cp "$IMPLEMENTOR:$TOOLS:$SOLUTIONS/java-solutions" \
  -d "$SOLUTIONS/scripts" \
  "$SOLUTIONS/java-solutions/info/kgeorgiy/ja/serov/implementor/Implementor.java"

cd "$SOLUTIONS/scripts"
jar cfm Implementor.jar MANIFEST.MF info
rm -r info
