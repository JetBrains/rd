using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.RdCore
{ 
  public abstract class RdCoreTestBase
  {
    protected LifetimeDefinition LifetimeDefinition;
    protected Lifetime TestLifetime;

    [SetUp]
    public virtual void SetUp()
    {
      Log.DefaultFactory = TestLogger.Factory;
      LifetimeDefinition = Lifetime.Define(Lifetime.Eternal);
      TestLifetime = LifetimeDefinition.Lifetime;
    }

    [TearDown]
    public void TearDown()
    {
      TearDownInternal();
      LifetimeDefinition.Terminate();
      ThrowLoggedExceptions();
    }
    
    protected virtual void TearDownInternal() {}

    protected void ThrowLoggedExceptions()
    {
      TestLogger.Logger.ThrowLoggedExceptions();
    }
  }
}