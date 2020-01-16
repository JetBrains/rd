# RdReflection

A special subsystem in Rd with C#-language-first models

### Features
  - All models are defined in pure C#
  - All Rd entities are supported (RdMap, RdSet, RdCall, RdList,...)
  - Live models with Rd entities
  - Scalar serializers
  - Proxy class generation for a simple RPC, based on RdCall

### Disadvantages
  - For communications only between C#-C#. No interop with models generated from DSL files

### Example

See `Test.Reflection.App` for a complete small application example. More
examples are available in tests.
