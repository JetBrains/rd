How to Use RdGen
=================
RdGen is a tool to generate model serialization and deserialization code in multiple languages (all compatible with each other): C#, Kotlin, C++.

To define a model for RdGen, a domain-specific language (DSL)-styled library for Kotlin is provided.

To invoke RdGen, Gradle build system is used in most cases.

Basic Terminology
-----------------
Each connection use a separate set of models, collected under a _root model_. The root model can be extended via _extension models_, while each of the extensions may contain their own extensions as well, forming a protocol tree.

In the runtime, the set of the models is asynchronously replicated between both the sides of a connection (and Rd connections are always two-sided).

The _root model_ and all the _extension models_ are collectively called _top-level models_, because they are defined on the top of the file in RdGen, and have a common set of properties, such as the ability to contain other entities.

Each of these models may have a set of _members_, such as:
- read-only members
  - constants
  - fields
- reactive members (i.e. the members that have a notification mechanism either side of the connection may _react_ to)
  - properties
  - lists
  - maps
  - sets
- callable members
  - signals (sinks / signal sources): "fire and forget"
  - calls, callbacks: it's possible to return a result

The protocol allows use for persistent models that have mutable members in memory, as well as in a RPC-like way without any persistence via signals and calls.
 
Model Definition
----------------
In this section, we'll omit the following import statement on top of all the code examples:
```kotlin
import com.jetbrains.rd.generator.nova.*
```

### Root and Extensions
Each protocol definition starts from the root model:

```kotlin
object ExampleRoot : Root()
```

This root object is a top-level model, and thus can contain members (see below), but it's also possible to add extensions on top of it:

```kotlin
object ExampleExt : Ext(ExampleRootNova)
```

This extension is a separate model (you may imagine them like namespaces with an additional ability to hold members with static lifetime).

You may put everything from your protocol into a root model, or define separate extensions for product subsystems and such. 

### Types
Before we discuss the model members, let's discuss the member types. RdGen allows the user to define their own types that then may be used in models (and may be nested into each other). You define a type via several ways:

1. By calling a corresponding method on a top-level object, for example:
   ```kotlin
   object Example : Root() {
      val nestedType = structdef {
          // Nested members of the type go here
      }
   }
   ```
   
   This defines a struct named `nestedType` inside of an `Example` model.
2. By calling a method in a place where a corresponding type is expected, in an ad-hoc manner, for example:
   ```kotlin
   object Example : Root() {
      init {
          field("foobar", structdef("OptionalNameForThisType") {
              // Nested members of the type go here
          })
      }
   }
   ```
   
You can reuse a same type declared once in several places (for example, as a type for several fields or a return type of a call).

#### Type Categories
Most of the types from the coming section are split into two big categories: _bindable types_ and _scalars_.

A bindable type is a type that can contain reactive members, while a scalar type is immutable and cannot contain any reactive or bindable members in it.

#### Entity Types
The protocol allows to declare the following type of entities:
1. `structdef`: a simple _non-bindable type_ that cannot contain any reactive properties; only fields are allowed. Use structs for simple data, normally for value objects only.
   
   Structs support inheritance, use `basestruct` and `extends` construct if you need:
   ```kotlin
   val BaseStruct = basestruct {
       field("baseField", PredefinedType.int)
   }
   val DerivedStruct = structdef extends BaseStruct {
       field("derivedField", PredefinedType.bool)
   }
   ```
2. `classdef`: a bindable type that may contain any reactive members inside. Classes also support inheritance:
   ```kotlin
   val BaseClass = baseclass {
       field("baseField", PredefinedType.int)
   }
   val DerivedClass = classdef extends BaseClass {
       field("derivedField", PredefinedType.bool)
   }
   ```

   Classes may be open for external inheritance as well. For that, use `openclass` instead of `classdef`.
3. `interfacedef`: declares an interface that may be implemented by other types. Example:
   ```kotlin
   val Interface = interfacedef {
       call("foo", PredefinedType.void, PredefinedType.void)
   }
   val BaseClassWithInterface = baseclass implements Interface with {
   }
   val BaseStructWithInterface = basestruct implements Interface with {
   }
   ```
   
   Note that you can chain multiple `implements` calls if you need one type to implement several interfaces:
   ```kotlin
   val DerivedClassWith2Interfaces = baseclass extends BaseClass implements Interface implements Interface2 with {
       field("derivedField", PredefinedType.bool)
   }
   ```
4. `enum` types:
   ```kotlin
   val MyEnum = enum {
       +"zero"
       +"two"
       +"three"
       +"five"
       +"six"
   }
   ```

There are also certain predefined types inside of the `PredefinedType` object, such as `bool`, `int`, `string`, `guid`, `timeSpan` and so on, that are normally mapped to standard types of the corresponding languages during the generation stage.  

Also, collection types may be used, see `array(type)` or `immutableList(type)` for collection type definitions.

#### Nullability
To mark a type as nullable, mark add `nullable` after it. For example:

```kotlin
property("foo1", PredefinedType.int.nullable)
```

This will have a language-specific effect on the generated code.

#### Attributes
It is possible to add certain attributes to entity types. For now, there are three attributes supported: `Nls`, `NlsSafe`, `NonNls`. All three affect localization, and are only emitted for Kotlin code right now.

They are mapped to the following Java annotations:
- `Nls` → `org.jetbrains.annotations.Nls`
- `NonNls` → `org.jetbrains.annotations.NonNls`
- `NlsSafe` → `com.intellij.openapi.util.NlsSafe` (IntelliJ-specific, should not be used outside of IntelliJ ecosystem)

Also, there are extension members defined in `com.jetbrains.rd.generator.nova` to declare common string types:
- `nlsString`
- `nonNlsString`
- `nlsSafeString`

For other cases, you can use the attributes directly via `.attrs()` call. For example, to declare a nullable localizeable string:
```
field("nls_nullable_field", PredefinedType.string.nullable.attrs(KnownAttrs.Nls))
```

#### Interning
It's possible to reduce protocol traffic by enabling interning of certain objects. In the model definition, you can call `.interned(scope)` on any scalar definition, and that will start interning the values. The interned values are only sent the first time they appear in a scope, and in future appearances, only their id is sent.

`scope` is the scope in that you want the values to be interned. For globally-interned values, use global `ProtocolInternScope`. Additionally, you can declare an intern scope in any top-level definition.

For each field, the closest scope in the model hierarchy will be chosen and used for interning. If there's no such intern scope, then interning will be automatically disabled.

Some examples:

```kotlin
map("issues", PredefinedType.int, structdef("ProtocolWrappedStringModel") {
    // These strings will be interned for the lifetime of the whole protocol:
    field("text", PredefinedType.string.interned(ProtocolInternScope))
})

object MyExample : Root() {
    val TestInternScope = internScope()

    internRoot(TestInternScope)
    val InterningTestModel = classdef {
        internRoot(TestInternScope)

        map("issues", PredefinedType.int, structdef("WrappedStringModel") {
            // These values will live as long as the parent class lives.
            field("text", PredefinedType.string.interned(TestInternScope))
        })
    }

    val InterningNestedTestModel = structdef {
        // These values will live for as long as the protocol root. 
        field("inner", this.interned(TestInternScope).nullable)
    }
}
```

### Members
Model types may contain a variety of members:
- constants: `const("name", type`
- read-only fields: `field("name", type, value)`
- signals (bi-directional): `signal("name", signalValueType)`
- sources (the signalling side of a signal): `source("name", signalValueType)`
- sinks (the receiving side of a signal): `sink("name", signalValueType)`
- calls (for RPC, awaitable, the calling side): `call("name", argumentType, returnValue)`
- callbacks (the receiving side of the call): `callback("name", argumentType, returnValue)`
- properties: `property("name", value)`
- TODO: asyncProperty and below
- methods (for interfaces): `method("name", returnType, vararg argumentTypes)`

### Settings
TODO

#### Transformations (as-is, reversed, symmetric)
TODO

### Threading
TODO: default threading model, `.async`, new threading model

Gradle Configuration
--------------------
TODO

Examples
--------
TODO (add links)

Walkthrough
-----------
TODO