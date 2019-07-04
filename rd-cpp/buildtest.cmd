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
cmake -DENABLE_TESTS_OPTION:BOOL=ON -G "Visual Studio 16 2019" ..
cmake --build . --config Release -j 4 
cmake -DENABLE_TESTS_OPTION:BOOL=ON -DBUILD_TYPE=Release -P cmake_install.cmake -j 4
popd