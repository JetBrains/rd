#if NETCOREAPP

using System;
using System.Collections;
using System.Collections.Immutable;

using JetBrains.Diagnostics;
using NUnit.Framework;

namespace Test.Lifetimes.Diagnostics;

public class FirstChanceExceptionInterceptorTest
{
  public class CustomImmutableException : Exception
  {
    public override IDictionary Data => ImmutableDictionary<string, string>.Empty;
  }

  [Test]
  public void TestData()
  {
    try
    {
      using (new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo("ddd"))
      {
        throw new CustomImmutableException();
      }
    }
    catch (Exception e)
    {
      Console.WriteLine(e);
    }
  }
}

#endif