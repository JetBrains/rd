#Steps to write your own cross test

1. Go to `com.jetbrains.rd.gradle.plugins.CrossTestPlugin.kt` or `rd-net/build.gradle.kts` 
or `rd-cpp/build.gradle.kts` and create at least two delegated properties that present
parts of your test. Let their names would be **A** and **B**.
2. Write tests on Kotlin or C# or C++, in `package com.jetbrains.rd.cross.cases`
or `namespace Test.RdCross.Cases` or `namespace rd.cross` respectively. Name of class
and target must have names **A** or **B**. 
3. Go to CrossTestPlugin.kt and create delegated property accumulating previous
tasks via taskServer and taskClient parameters by their names. Let it name would be **C**.
4. Go to com.jetbrains.rd.cross.test.cases.diff.CrossTest and create test with name
`"test${C}"` (in terms of Kotlin string literals).
5. To run it use appropriate JUnit runner. 
**NB!** If nothing's changed, i.e. framework's code and build output are the same,
cross test uses previous result and just compare _.tmp_ and _.gold_ files.