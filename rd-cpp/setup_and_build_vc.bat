@echo off
pushd "%~dp0"
call get_dependencies.bat
mkdir build
pushd build
cmake -DENABLE_TESTS_OPTION:BOOL=OFF -G "Visual Studio 15 2017 Win64" ..
cmake --build . --config Release 
cmake -DENABLE_TESTS_OPTION:BOOL=OFF -DBUILD_TYPE=Release -P cmake_install.cmake
popd
@echo on