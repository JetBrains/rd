:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -eux
mkdir -p build
cd build
cmake -G "Visual Studio 16 2019" -A x64 ..
make

exit 0

:CMDSCRIPT
@echo on
pushd "%~dp0"
mkdir build
cd build
cmake -DENABLE_TESTS_OPTION:BOOL=OFF  -G "Visual Studio 16 2019" -A x64  ..
cmake --build . --config Release -j %NUMBER_OF_PROCESSORS%
cmake --build . --config Debug -j %NUMBER_OF_PROCESSORS%
cmake -DENABLE_TESTS_OPTION:BOOL=OFF -DBUILD_TYPE=Release -P cmake_install.cmake -j %NUMBER_OF_PROCESSORS%
cmake -DENABLE_TESTS_OPTION:BOOL=OFF -DBUILD_TYPE=Debug -P cmake_install.cmake -j %NUMBER_OF_PROCESSORS%
popd
