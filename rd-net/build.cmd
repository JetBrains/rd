:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -eux
PACKAGES_DIR=$(pwd)/artifacts/nuget
BUILD_COUNTER=0
PACKAGE_VERSION=201.0.$BUILD_COUNTER

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
set PACKAGE_VERSION=201.0.%BUILD_COUNTER%

rmdir /S /Q %PACKAGES_DIR%

pushd Lifetimes
dotnet pack -p:Configuration=Release -p:PackageVersion=%PACKAGE_VERSION% -o %PACKAGES_DIR% --verbosity normal

popd
pushd RdFramework
dotnet pack -p:Configuration=Release -p:PackageVersion=%PACKAGE_VERSION% -o %PACKAGES_DIR% --verbosity normal
