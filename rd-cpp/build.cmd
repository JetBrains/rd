:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -eux
./get_dependencies.cmd
mkdir -p build
cd build
cmake -G "Visual Studio 15 2017" ..
make

exit 0

:CMDSCRIPT
@echo on
pushd "%~dp0"
mkdir build
cd build
cmake -DENABLE_TESTS_OPTION:BOOL=OFF  -G "Visual Studio 15 2017 Win64"  ..
cmake --build . --config Release -j 4
cmake --build . --config Debug -j 4
cmake -DENABLE_TESTS_OPTION:BOOL=OFF -DBUILD_TYPE=Release -P cmake_install.cmake -j 4
cmake -DENABLE_TESTS_OPTION:BOOL=OFF -DBUILD_TYPE=Debug -P cmake_install.cmake -j 4
popd