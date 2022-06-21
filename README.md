# RD 
[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Maven Central](https://img.shields.io/maven-central/v/com.jetbrains.rd/rd-core)](https://mvnrepository.com/artifact/com.jetbrains.rd)

Reactive Distributed communication framework for .NET, Kotlin and C++ (experimental). 
Inspired by JetBrains Rider IDE. 

## Structure
The framework contains several libraries for single process usage and cross-process communication.

### Lifetimes
JetBrains Core library for graceful disposal, concurrency and reactive programming. For single-process usage.

* Kotlin
  * Sources: https://github.com/JetBrains/rd/tree/master/rd-kt/rd-core
  * Artifacts: https://mvnrepository.com/artifact/com.jetbrains.rd/rd-core

* .NET
  * Sources: https://github.com/JetBrains/rd/tree/master/rd-net/Lifetimes
  * Artifacts: https://www.nuget.org/packages/JetBrains.Lifetimes

### RdFramework
JetBrains Networking library for reactive distributed communication

* Kotlin
  * Sources: https://github.com/JetBrains/rd/tree/master/rd-kt/rd-framework
  * Maven artifacts: https://mvnrepository.com/artifact/com.jetbrains.rd/rd-framework

* .NET
  * Sources: https://github.com/JetBrains/rd/tree/master/rd-net/RdFramework
  * NuGet artifacts: https://www.nuget.org/packages/JetBrains.RdFramework

#### RdFramework.Reflection
Plugin for RdFramework used for defining models using regular C#

* .NET
  * Sources: https://github.com/JetBrains/rd/tree/master/rd-net/RdFramework.Reflection
  * NuGet artifacts: https://www.nuget.org/packages/JetBrains.RdFramework.Reflection

### RdGen
Rd Generator: generates stubs (Kotlin/C#/C++) classes by Kotlin DSL models
* Sources: https://github.com/JetBrains/rd/tree/master/rd-kt/rd-gen
* Gradle: https://mvnrepository.com/artifact/com.jetbrains.rd/rd-gen
  
# How to build

Firstly decide which languages will be involved in the protocol. It may be **Kotlin** and **C#**, Rider uses them for instance. 
Or **C++** only, who knows. After that prepare the environment and build needed assemblies. Choose **separate** or **common** build based on needs.

## Separate build

### .NET

Open solution in JetBrains Rider: https://github.com/JetBrains/rd/tree/master/rd-net/Rd.sln

#### Requirements

* .NET Framework >= 3.5

#### Console build instructions

* `dotnet build rd-net/Rd.sln`

### Kotlin

Open solution in IntellijIDEA:  https://github.com/JetBrains/rd

#### Requirements

* Gradle 6.2.2
* Kotlin 1.3.61

#### Console build instructions

* `gradle :build -x test`

### C++ (experimental)

Open solution in JetBrains CLion: https://github.com/JetBrains/rd/rd-cpp

#### Requirements

* git
* cmake
* Visual Studio 2015+
or
* clang 6.0+

#### Console build instructions

* `cd rd-cpp`
* `./build.cmd`

or

* `gradle :rd-cpp:build -x test`

## Build everything

* `gradle build`

### Build NuGet packages instructions

To build packages locally please use: `rd-kt/rd-gen/pack.sh`

*\* Right now it works only on Linux. Please use Docker for Windows or macOS.*

## Run tests (Kotlin part only)

### On a local computer

Don't forget to set `TEAMCITY_VERSION=1` (temporary measure for now) before running any tests.

```console
$ ./gradlew build
```

### In a Docker container

```console
$ docker build . -t rd && docker rm rd && docker run -it --name rd rd
```

To run particular tests (e.g. `:rd-gen:test`):

```console
$ docker build . -t rd && docker rm rd && docker run -it --name rd rd --entrypoint ./gradlew :rd-gen:test
```

To extract test results afterwards:

```console
$ docker cp rd:/rd/rd-kt/rd-gen/build/reports/ T:\Temp\reports
```

# How to generate models (stubs)

Generate models in each language you have chosen. 
For this purpose project _:rd-gen_ must be built.
  
## Instruction

See https://www.jetbrains.com/help/resharper/sdk/Products/Rider.html#protocol-extension for more details.

### More examples of models

See _com.jetbrains.rd.generator.test.cases.generator.demo.DemoModel_
and _com.jetbrains.rd.generator.test.cases.generator.example.ExampleModel_

### How to connect processes

Generally, it depends on the architecture. But in standard Client-Server architecture through localhost connection framework's classes are suitable.

### C#

JetBrains.Rd.Impl.Server and *.Client respectively

### Kotlin

com.jetbrains.rd.framework.Server and *.Client respectively

### C++

rd::SocketWire::Server and *.Client respectively

## Examples of connections

Look at cross tests
* _com.jetbrains.rd.framework.test.cross_ at Kotlin side
* _Test.RdCross_ at C# side
* _rd::cross_ at C++ side

## License

Rd is licensed under the [Apache 2.0](LICENSE) license. Rd distributions may include third-party software licensed separately; see [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES.TXT) for details.
