:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -euxo pipefail
./get_dependencies.cmd
mkdir -p build
pushd build
cmake ..
make

exit 0

:CMDSCRIPT
@echo on
pushd "%~dp0"
call get_dependencies.cmd
mkdir build
pushd build
cmake -DENABLE_TESTS_OPTION:BOOL=OFF -G "Visual Studio 16 2019" ..
cmake --build . --config Release 
cmake -DENABLE_TESTS_OPTION:BOOL=OFF -DBUILD_TYPE=Release -P cmake_install.cmake
popd