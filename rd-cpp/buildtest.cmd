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
call get_dependencies.cmd
mkdir build
cd build
cmake -DENABLE_TESTS_OPTION:BOOL=ON ..
cmake --build . --config Release -j 4 
cmake -DENABLE_TESTS_OPTION:BOOL=ON -DBUILD_TYPE=Release -P cmake_install.cmake -j 4
popd