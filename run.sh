#!/bin/bash

rm -rf target/

mvn package

if [ $? -eq 0 ]; then
    java -jar target/flink-tpch-streaming-1.0-SNAPSHOT.jar
else
    echo "Build failed! Please check the errors above."
    read -p "Press any key to continue..."
fi