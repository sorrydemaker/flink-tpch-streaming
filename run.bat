@echo off

rmdir /s /q target

call mvn package

if %ERRORLEVEL% EQU 0 (
    java -jar target/flink-tpch-streaming-1.0-SNAPSHOT.jar
) else (
    echo Build failed! Please check the errors above.
    pause
)