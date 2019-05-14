:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -euxo pipefail
PACKAGES_DIR=$(pwd)/../artifacts/nuget
PACKAGE_VERSION=191.0.$BUILD_COUNTER-prerelease

rm -rf $PACKAGES_DIR

pushd RdCore
dotnet pack -p:Configuration=Release -p:PackageVersion=$PACKAGE_VERSION -o $PACKAGES_DIR --verbosity normal

popd
pushd RdFramework
dotnet pack -p:Configuration=Release -p:PackageVersion=$PACKAGE_VERSION -o $PACKAGES_DIR --verbosity normal
exit 0

:CMDSCRIPT
@echo on
pushd "%~dp0"
set PACKAGES_DIR=%~dp0..\artifacts\nuget
set BUILD_COUNTER=99999
set PACKAGE_VERSION=191.0.%BUILD_COUNTER%-prerelease

rmdir /S /Q %PACKAGES_DIR%

pushd RdCore
dotnet pack -p:Configuration=Release -p:PackageVersion=%PACKAGE_VERSION% -o %PACKAGES_DIR% --verbosity normal

popd
pushd RdFramework
dotnet pack -p:Configuration=Release -p:PackageVersion=%PACKAGE_VERSION% -o %PACKAGES_DIR% --verbosity normal