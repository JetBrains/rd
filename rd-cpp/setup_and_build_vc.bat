@echo off
call get_dependencies.bat
mkdir build
cd build
cmake -G "Visual Studio 15 2017 Win64" ..
cmake --build . --config Release
@echo on