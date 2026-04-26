@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@REM Maven wrapper Windows script
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MAVEN_WRAPPER_PROPERTIES=.mvn\wrapper\maven-wrapper.properties
set MAVEN_USER_HOME=%USERPROFILE%\.m2
set MAVEN_WRAPPER_HOME=%MAVEN_USER_HOME%\wrapper\dists

@REM Parse distributionUrl from properties file
for /f "tokens=2 delims==" %%a in ('findstr "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') do set DISTRIBUTION_URL=%%a

@REM Extract dist name from URL (filename without .zip)
for %%f in ("%DISTRIBUTION_URL:.zip=%") do set DIST_NAME=%%~nxf
set DIST_DIR=%MAVEN_WRAPPER_HOME%\%DIST_NAME%

if not exist "%DIST_DIR%" (
    mkdir "%DIST_DIR%"
    echo Downloading Maven %DIST_NAME%...
    powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DIST_DIR%\%DIST_NAME%.zip'"
    powershell -Command "Expand-Archive -Path '%DIST_DIR%\%DIST_NAME%.zip' -DestinationPath '%DIST_DIR%'"
)

for /d %%d in ("%DIST_DIR%\apache-maven-*") do set MAVEN_HOME=%%d

"%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
