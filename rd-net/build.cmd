:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -euxo pipefail
PACKAGES_DIR=$(pwd)/artifacts/nuget
BUILD_COUNTER=0
PACKAGE_VERSION=193.0.$BUILD_COUNTER-prerelease

rm -rf $PACKAGES_DIR

pushd Lifetimes
dotnet pack -p:Configuration=Release -p:PackageVersion=$PACKAGE_VERSION -o $PACKAGES_DIR --verbosity normal

popd
pushd RdFramework
dotnet pack -p:Configuration=Release -p:PackageVersion=$PACKAGE_VERSION -o $PACKAGES_DIR --verbosity normal
exit 0

:CMDSCRIPT
@echo on
pushd "%~dp0"
set PACKAGES_DIR=%~dp0\artifacts\nuget
set BUILD_COUNTER=0
set PACKAGE_VERSION=191.0.%BUILD_COUNTER%-prerelease

rmdir /S /Q %PACKAGES_DIR%

pushd Lifetimes
dotnet pack -p:Configuration=Release -p:PackageVersion=%PACKAGE_VERSION% -o %PACKAGES_DIR% --verbosity normal

popd
pushd RdFramework
dotnet pack -p:Configuration=Release -p:PackageVersion=%PACKAGE_VERSION% -o %PACKAGES_DIR% --verbosity normal
