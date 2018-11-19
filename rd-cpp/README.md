# Project Title

C++ implementation of JetBrains IPC

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

What things you need to install the software and how to install them

* git
* cmake
* Visual Studio 2015+
or
* clang

### Installing

A step by step series of examples that tell you how to get a development env running

#### tl;dr script for VS build 2017

```
git clone https://github.com/JetBrains/rd.git
cd rd
git checkout --track origin/rd-cpp
cd rd-cpp
git clone https://github.com/TartanLlama/optional.git
git clone https://github.com/mpark/variant.git
git clone https://github.com/google/googletest.git
mkdir build
cd build
cmake -G "Visual Studio 15 2017" ..
cmake --build . --config Release
```
