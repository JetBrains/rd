# RD

Reactive Distributed communication framework for .NET, Kotlin and C++ (experimental). 
Inspired by JetBrains Rider IDE. 

Motivation and brief course:

https://www.youtube.com/watch?v=W9Jtagm8nVQ&feature=youtu.be&list=PLQ176FUIyIUaiCssbIPQjFGvQX92HPtGU

## Structure
The framework contains of several libraries for single process usage and cross process communication.

### Lifetimes
JetBrains Core library for graceful disposal, concurrency and reactive programming. For single process usage.

* Kotlin
  * Sources: https://github.com/JetBrains/rd/tree/master/rd-kt/rd-core
  * Artifacts: https://www.myget.org/feed/rd-snapshots/package/maven/com.jetbrains.rd/rd-core/0.201.40 

* .NET
  * Sources: https://github.com/JetBrains/rd/tree/master/rd-net/Lifetimes
  * Artifacts: https://www.nuget.org/packages/JetBrains.Lifetimes

### RdFramework
JetBrains Networking library for reactive distributed communication

* Kotlin
  * Sources: https://github.com/JetBrains/rd/tree/master/rd-kt/rd-framework
  * Maven artifacts: https://www.myget.org/feed/rd-snapshots/package/maven/com.jetbrains.rd/rd-framework/0.201.40 

* .NET
  * Sources: https://github.com/JetBrains/rd/tree/master/rd-net/RdFramework
  * Maven artifacts: https://www.nuget.org/packages/JetBrains.RdFramework
  
### RdGen
Rd Generator: generates stubs (Kotlin/C#/C++) classes by Kotlin DSL models
* Sources: https://github.com/JetBrains/rd/tree/master/rd-kt/rd-gen
* Gradle: https://www.myget.org/feed/rd-snapshots/package/maven/com.jetbrains.rd/rd-gen/0.201.40
* NuGet: https://www.nuget.org/packages/JetBrains.RdGen



  
# How to build

Firstly decide which languages will be involved in protocol. It may be **Kotlin** and **C#**, Rider uses them for instance. 
Or **C++** only, who knows. After that prepare the environment and build needed assemblies. Choose **separate** or **common** build based on needs.

## Separate build

### .NET

Open solution in JetBrains Rider: https://github.com/JetBrains/rd/tree/master/rd-net/Rd.sln

#### Requirements

* .NET Framework >= 3.5

#### Console build instructions

* cd rd-net
* ./build.cmd

or

* gradle :rd-net:build -x test

### Kotlin

Open solution in IntellijIDEA:  https://github.com/JetBrains/rd

#### Requirements

* Gradle 6.0
* Kotlin 1.3.50+

#### Console build instructions

* gradle :build -x test

### C++ (experimental)

Open solution in JetBrains CLion: https://github.com/JetBrains/rd/rd-cpp

#### Requirements

* git
* cmake
* Visual Studio 2015+
or
* clang 6.0+

#### Console build instructions

* cd rd-cpp
* ./build.cmd

or

* gradle :rd-cpp:build -x test

## Build everything

* gradle build

# How to generate models (stubs)

Generate models in each language you have chosen. 
For this purpose project _:rd-gen_ must be built.
  
## Instruction

See https://www.jetbrains.com/help/resharper/sdk/Products/Rider.html#protocol-extension for more details.

### More examples of models

See _com.jetbrains.rd.generator.test.cases.generator.demo.DemoModel_
and _com.jetbrains.rd.generator.test.cases.generator.example.ExampleModel_

### How to connect processes

Generally it depends on the architecture. But in standard Client-Server architecture through localhost connection framework's classes are suitable.

### C#

JetBrains.Rd.Impl.Server and *.Client respectively

### Kotlin

com.jetbrains.rd.framework.Server and *.Client respectively

### С++

rd::SocketWire::Server and *.Client respectively

## Examples of connections

Look at cross tests
* _com.jetbrains.rd.framework.test.cross_ at Kotlin side
* _Test.RdCross_ at C# side
* _rd::cross_ at C++ side

 
