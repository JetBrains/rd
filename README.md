# RD

Reactive Distributed communication framework for .net, kotlin, js, c++. Inspired by Rider IDE. 

# First Step(Build)

Firstly decide which languages will be involved in protocol. It may be **Kotlin** and **C#**, Rider uses them for instance. 
Or **C++** only, who knows. After that prepare the environment and build needed assemblies. Choose **separate** or **common** build based on needs.

## Separate build

### C#

#### Requirements

* .NET Framework >= 3.5

#### Instruction

* cd rd-net
* ./build.cmd

or

* gradle :rd-net:build -x test

### Kotlin

#### Requirements

* Gradle 4.8
* Kotlin 1.3.21+

#### Instruction

* gradle :rd-kt:build -x test

### C++

#### Requirements

* git
* cmake
* Visual Studio 2015+
or
* clang 6.0+

#### Instruction

* cd rd-cpp
* ./build.cmd

or

* gradle :rd-cpp:build -x test

## Common build

## Requirements

All above ones.

## Instruction

* gradle build

# Second step(Generation models)

Further generate models in each language you have chosen. 
For this purpose project _:rd-kt:rd-gen_ must be built.
  
## Instruction

See https://www.jetbrains.com/help/resharper/sdk/Products/Rider.html#protocol-extension for more details.

## More examples of models

See _com.jetbrains.rd.generator.test.cases.generator.demo.DemoModel_
and _com.jetbrains.rd.generator.test.cases.generator.example.ExampleModel_

## Last step(Connection the sides)

Generally it depends on the architecture. But in standard Client-Server architecture through localhost connection framework's classes are suitable.

### C#

JetBrains.Rd.Impl.Server and *.Client respectively

### Kotlin

com.jetbrains.rd.framework.Server and *.Client respectively

### С++

rd::SocketWire::Server and *.Client respectively

## Examples of connections

TODO

 