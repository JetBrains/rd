:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -eux
./get_dependencies.cmd
mkdir -p build
cd build
cmake ..
make

exit 0

:CMDSCRIPT
@echo on
pushd "%~dp0"
mkdir build
cd build
cmake -DENABLE_TESTS_OPTION:BOOL=OFF ..
cmake --build . --config Release -j 4
cmake -DENABLE_TESTS_OPTION:BOOL=OFF -DBUILD_TYPE=Release -P cmake_install.cmake -j 4
popd