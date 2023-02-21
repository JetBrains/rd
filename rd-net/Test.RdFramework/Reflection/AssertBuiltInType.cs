using System;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection;

/// <summary>
/// Mark test class as expected to have specific built-in serializer type.
/// It is used both for visual help during test data review and enforces by special test which load all types from
/// the currently running assembly and check all of them.
/// </summary>
[AttributeUsage(AttributeTargets.Class)]
public class AssertBuiltInTypeAttribute : Attribute
{
  public BuiltInSerializers.BuiltInType BuiltInType { get; }

  public AssertBuiltInTypeAttribute(BuiltInSerializers.BuiltInType type)
  {
    BuiltInType = type;
  }
}