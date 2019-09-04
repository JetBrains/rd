using System;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.Lifetimes
{
  public abstract class LifetimesTestBase
  {
    private IDisposable myDisposable;

    protected LifetimeDefinition LifetimeDefinition;
    protected Lifetime TestLifetime;

    [SetUp]
    public virtual void SetUp()
    {
      myDisposable = Log.UsingLogFactory(TestLogger.Factory);

      LifetimeDefinition = Lifetime.Define(Lifetime.Eternal);
      TestLifetime = LifetimeDefinition.Lifetime;
    }

    [TearDown]
    public virtual void TearDown()
    {
      TearDownInternal();
      LifetimeDefinition.Terminate();
      ThrowLoggedExceptions();
      myDisposable.Dispose();
    }

    protected virtual void TearDownInternal() {}

    protected void ThrowLoggedExceptions()
    {
      TestLogger.Logger.ThrowLoggedExceptions();
    }
  }
}