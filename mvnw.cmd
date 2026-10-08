@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal enabledelayedexpansion

set MAVEN_PROJECTBASEDIR=%cd%
if not "%MAVEN_HOME%"=="" set MAVEN_HOME=%MAVEN_HOME:"=%
if "%MAVEN_HOME%"=="" set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.8.8"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "MAVEN_CONFIG=%USERPROFILE%\.m2"

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%WRAPPER_JAR%'"
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Maven not found, downloading...
    mkdir "%MAVEN_HOME%"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.8/apache-maven-3.8.8-bin.zip' -OutFile '%TEMP%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%MAVEN_HOME%\..' -Force"
)

set "M2_HOME=%MAVEN_HOME%"
set "PATH=%MAVEN_HOME%\bin;%PATH%"

if not exist "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" goto endReadJvmConfig
set "MAVEN_OPTS=@.mvn\jvm.config"
:endReadJvmConfig

%MAVEN_HOME%\bin\mvn.cmd %*

endlocal
