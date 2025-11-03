@echo off
setlocal

set JDK_DIR=jdk-22
set JDK_URL=https://download.java.net/openjdk/jdk22/ri/openjdk-22+36_windows-x64_bin.zip

if exist %JDK_DIR%\ (
    echo JDK already present in %JDK_DIR%
    exit /b 0
)

echo Downloading JDK...
curl -L -o jdk.zip %JDK_URL%
if %ERRORLEVEL% neq 0 (
    echo Download failed. Check your internet connection or URL.
    exit /b 1
)

echo Extracting...
tar -xf jdk.zip
del jdk.zip

echo JDK installed locally in %JDK_DIR%
endlocal
