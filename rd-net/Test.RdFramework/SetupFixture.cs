using System;
using NUnit.Framework;

namespace Test.RdFramework;

[SetUpFixture, TestFixture]
public class SetupFixture
{
  [OneTimeSetUp]
  public void Setup()
  {
    AppDomain.CurrentDomain.SetData("JET_MODE_ASSERT", true);
  }
}
