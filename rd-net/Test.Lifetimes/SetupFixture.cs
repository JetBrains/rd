using System;
using JetBrains.Diagnostics;
using NUnit.Framework;

namespace Test.Lifetimes;

[SetUpFixture, TestFixture]
public class SetupFixture
{
  [OneTimeSetUp]
  public void Setup()
  {
    if (!ModeInitializer.Init(true))
      throw new Exception($"Assertion mode cannot be initialized. (default value was used: {ModeInitializer.GetIsAssertionUndefined()})");
  }
}
