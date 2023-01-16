#!/bin/bash

currentPath=$(pwd)
cd $(dirname -- $0)
echo "Compiling ..."
kotlinc ../folder-merger.kt -include-runtime -d ../folder-merger.jar
echo -e "Compilation ended !"
cd $currentPath
