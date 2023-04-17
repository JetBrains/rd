using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Threading;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.RdFramework;

[TestFixture]
[Apartment(System.Threading.ApartmentState.STA)]
public class RdCollectionsTest : RdFrameworkTestBase
{
  private static readonly int ourKey = 1;

  protected override ISerializers CreateSerializers(bool isServer)
  {
    var serializers = base.CreateSerializers(isServer);

    Register(RdMap<int, RdList<RdProperty<RdSet<int>>>>.Read, RdMap<int, RdList<RdProperty<RdSet<int>>>>.Write);
    Register(RdList<RdProperty<RdSet<int>>>.Read, RdList<RdProperty<RdSet<int>>>.Write);
    Register(RdSet<int>.Read, RdSet<int>.Write);
    
    Register(RdProperty<RdSet<int>>.Read, RdProperty<RdSet<int>>.Write);
    Register(RdProperty<RdProperty<RdSet<int>>>.Read, RdProperty<RdProperty<RdSet<int>>>.Write);
    Register(RdProperty<RdProperty<RdProperty<RdSet<int>>>>.Read, RdProperty<RdProperty<RdProperty<RdSet<int>>>>.Write);
    
    return serializers;

    void Register<T>(CtxReadDelegate<T> read, CtxWriteDelegate<T> write)
    {
      serializers.Register(read, write, RdId.DefineByFqn(typeof(T)).Value);
    }
  }
  
  

  protected override IScheduler CreateScheduler(bool isServer) { return new PumpScheduler(Thread.CurrentThread); }
  protected override IScheduler TryCreateWireScheduler(bool isServer) { return new MyScheduler(Thread.CurrentThread); }

  [Flags]
  enum SchedulerKind
  {
    Client = 1,
    Server = 2,
    Both = Client | Server
  }

  private void SetSchedulerActive(SchedulerKind kind, Action action) => SetSchedulerActive(kind, () =>
  {
    action();
    return Unit.Instance;
  });
  
  private T SetSchedulerActive<T>(SchedulerKind kind, Func<T> func)
  {
    return Lifetime.Using(lifetime =>
    {
      if ((kind & SchedulerKind.Client) != 0)
        ((PumpScheduler)ClientProtocol.Scheduler).SetActive(lifetime);
      
      if ((kind & SchedulerKind.Server) != 0)
        ((PumpScheduler)ServerProtocol.Scheduler).SetActive(lifetime);

      return func();
    });
  }

  [Test]
  public void SetInitializedCollectionsTest()
  {
    var serverTopLevelProperty = BindToServer(LifetimeDefinition.Lifetime, NewRdProperty<RdMap<int, RdList<RdProperty<RdSet<int>>>>>(), ourKey);
    serverTopLevelProperty.ValueCanBeNull = true;
    
    var clientTopLevelProperty = BindToClient(LifetimeDefinition.Lifetime, NewRdProperty<RdMap<int, RdList<RdProperty<RdSet<int>>>>>(), ourKey);
    clientTopLevelProperty.ValueCanBeNull = true;

    var clientMap = NewRdMap<int, RdList<RdProperty<RdSet<int>>>>();
    var clientList = NewRdList<RdProperty<RdSet<int>>>();
    var clientProperty = NewRdProperty<RdSet<int>>();
    var clientSet = NewRdSet<int>();
    
    clientSet.Add(1);
    clientSet.Add(2);
    clientSet.Add(3);
    
    clientProperty.Value = clientSet;
    clientList.Add(clientProperty);
    clientMap[2] = clientList;

    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientTopLevelProperty.Value = clientMap;
    });
    Assert.IsFalse(serverTopLevelProperty.Maybe.HasValue);
    
    PumpAllProtocols(true);

    var serverMap = serverTopLevelProperty.Value;

    Assert.AreEqual(1, serverMap.Count);
    Assert.AreEqual(2, serverMap.Keys.Single());

    var serverList = serverMap.Values.Single();
    Assert.AreEqual(1, serverList.Count);
    var serverProperty = serverList[0];
    Assert.IsTrue(serverProperty.Maybe.HasValue);

    var serverSet = serverProperty.Value;
    Assert.AreEqual(3, serverSet.Count);
    Assert.IsTrue(serverSet.Contains(1));
    Assert.IsTrue(serverSet.Contains(2));
    Assert.IsTrue(serverSet.Contains(3));

    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientTopLevelProperty.Value = null;
    });
    
    
    PumpAllProtocols(true);
    
    Assert.AreEqual(BindState.NotBound,clientMap.BindState);
    Assert.AreEqual(BindState.NotBound,serverMap.BindState);
    
    Assert.AreEqual(BindState.NotBound,clientList.BindState);
    Assert.AreEqual(BindState.NotBound,serverList.BindState);
    
    Assert.AreEqual(BindState.NotBound,clientProperty.BindState);
    Assert.AreEqual(BindState.NotBound,serverProperty.BindState);
    
    Assert.AreEqual(BindState.NotBound,clientSet.BindState);
    Assert.AreEqual(BindState.NotBound,serverSet.BindState);
  }
  
  [Test]
  public void UnbindTest()
  {
    var serverTopLevelProperty = BindToServer(LifetimeDefinition.Lifetime, NewRdProperty<RdMap<int, RdList<RdProperty<RdSet<int>>>>>(), ourKey);
    serverTopLevelProperty.ValueCanBeNull = true;
    
    var clientTopLevelProperty = BindToClient(LifetimeDefinition.Lifetime, NewRdProperty<RdMap<int, RdList<RdProperty<RdSet<int>>>>>(), ourKey);
    clientTopLevelProperty.ValueCanBeNull = true;

    var clientMap = NewRdMap<int, RdList<RdProperty<RdSet<int>>>>();
    clientMap.ValueCanBeNull = true;
    
    var clientList = NewRdList<RdProperty<RdSet<int>>>();
    clientList.ValueCanBeNull = true;
    var clientProperty = NewRdProperty<RdSet<int>>();
    clientProperty.ValueCanBeNull = true;
    
    var clientSet = NewRdSet<int>();
    
    clientSet.Add(1);
    clientSet.Add(2);
    clientSet.Add(3);
    
    clientProperty.Value = clientSet;
    clientList.Add(clientProperty);
    clientMap[2] = clientList;

    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientTopLevelProperty.Value = clientMap;
    });
    Assert.IsFalse(serverTopLevelProperty.Maybe.HasValue);
    
    PumpAllProtocols(true);

    var serverMap = serverTopLevelProperty.Value;

    Assert.AreEqual(1, serverMap.Count);
    Assert.AreEqual(2, serverMap.Keys.Single());

    var serverList = serverMap.Values.Single();
    Assert.AreEqual(1, serverList.Count);
    var serverProperty = serverList[0];
    Assert.IsTrue(serverProperty.Maybe.HasValue);

    var serverSet = serverProperty.Value;
    Assert.AreEqual(3, serverSet.Count);
    Assert.IsTrue(serverSet.Contains(1));
    Assert.IsTrue(serverSet.Contains(2));
    Assert.IsTrue(serverSet.Contains(3));

    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientProperty.Value = NewRdSet<int>();
    });
    
    PumpAllProtocols(true);
    
    Assert.AreEqual(BindState.NotBound,clientSet.BindState);
    Assert.AreEqual(BindState.NotBound,serverSet.BindState);
    Assert.AreEqual(BindState.Bound,clientProperty.BindState);
    Assert.AreEqual(BindState.Bound,serverProperty.BindState);

    clientSet = clientProperty.Value;
    serverSet = serverProperty.Value;
    Assert.AreEqual(BindState.Bound,clientSet.BindState);
    Assert.AreEqual(BindState.Bound,serverSet.BindState);
    
    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientProperty.Value = null;
    });
    
    PumpAllProtocols(true);
    
    Assert.AreEqual(BindState.NotBound,clientSet.BindState);
    Assert.AreEqual(BindState.NotBound,serverSet.BindState);
    Assert.AreEqual(BindState.Bound,clientProperty.BindState);
    Assert.AreEqual(BindState.Bound,serverProperty.BindState);
    
    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientList[0] = NewRdProperty<RdSet<int>>();
    });
    
    PumpAllProtocols(true);
    
    Assert.AreEqual(BindState.NotBound,clientProperty.BindState);
    Assert.AreEqual(BindState.NotBound,serverProperty.BindState);
    Assert.AreEqual(BindState.Bound,clientList.BindState);
    Assert.AreEqual(BindState.Bound,serverList.BindState);


    clientProperty = clientList[0];
    serverProperty = serverList[0];
    Assert.AreEqual(BindState.Bound,clientProperty.BindState);
    Assert.AreEqual(BindState.Bound,serverProperty.BindState);
    
    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientList.Clear();
    });
    
    PumpAllProtocols(true);
    
    Assert.AreEqual(BindState.NotBound,clientProperty.BindState);
    Assert.AreEqual(BindState.NotBound,serverProperty.BindState);
    Assert.AreEqual(BindState.Bound,clientList.BindState);
    Assert.AreEqual(BindState.Bound,serverList.BindState);
    
    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientMap[clientMap.Keys.Single()] = NewRdList<RdProperty<RdSet<int>>>();
    });
    
    PumpAllProtocols(true);
    
    Assert.AreEqual(BindState.NotBound,clientList.BindState);
    Assert.AreEqual(BindState.NotBound,serverList.BindState);
    Assert.AreEqual(BindState.Bound,clientMap.BindState);
    Assert.AreEqual(BindState.Bound,serverMap.BindState);

    clientList = clientMap.Values.Single();
    serverList = serverMap.Values.Single();
    Assert.AreEqual(BindState.Bound,clientList.BindState);
    Assert.AreEqual(BindState.Bound,serverList.BindState);
    
    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientMap.Clear();
    });
    
    PumpAllProtocols(true);
    
    Assert.AreEqual(BindState.NotBound,clientList.BindState);
    Assert.AreEqual(BindState.NotBound,serverList.BindState);
    Assert.AreEqual(BindState.Bound,clientMap.BindState);
    Assert.AreEqual(BindState.Bound,serverMap.BindState);
    
    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientTopLevelProperty.Value = null;
    });
    
    PumpAllProtocols(true);
    
    Assert.AreEqual(BindState.NotBound,clientMap.BindState);
    Assert.AreEqual(BindState.NotBound,serverMap.BindState);
  }

  
  [Test]
  public void ChangeCollectionsTest()
  {
    ClientWire.AutoTransmitMode = true;
    ServerWire.AutoTransmitMode = true;
    
    var serverTopLevelProperty = BindToServer(LifetimeDefinition.Lifetime, NewRdProperty<RdMap<int, RdList<RdProperty<RdSet<int>>>>>(), ourKey);
    serverTopLevelProperty.ValueCanBeNull = true;
    var clientTopLevelProperty = BindToClient(LifetimeDefinition.Lifetime, NewRdProperty<RdMap<int, RdList<RdProperty<RdSet<int>>>>>(), ourKey);
    clientTopLevelProperty.ValueCanBeNull = true;

    var clientMap = NewRdMap<int, RdList<RdProperty<RdSet<int>>>>();;
    var clientList = NewRdList<RdProperty<RdSet<int>>>();
    var clientProperty = NewRdProperty<RdSet<int>>();
    var clientSet = NewRdSet<int>();
    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientTopLevelProperty.Value = clientMap;

      clientMap[2] = clientList;

      clientList.Add(clientProperty);

      clientProperty.Value = clientSet;

      clientSet.Add(1);
      clientSet.Add(2);
      clientSet.Add(3);
    });

    PumpAllProtocols(true);

    var serverMap = serverTopLevelProperty.Value;

    Assert.AreEqual(1, serverMap.Count);
    Assert.AreEqual(2, serverMap.Keys.Single());

    var serverList = serverMap.Values.Single();
    Assert.AreEqual(1, serverList.Count);
    var serverProperty = serverList[0];
    Assert.IsTrue(serverProperty.Maybe.HasValue);

    var serverSet = serverProperty.Value;
    Assert.AreEqual(3, serverSet.Count);
    Assert.IsTrue(serverSet.Contains(1));
    Assert.IsTrue(serverSet.Contains(2));
    Assert.IsTrue(serverSet.Contains(3));
    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientTopLevelProperty.Value = null;
    });
    PumpAllProtocols(true);
    
    Assert.AreEqual(BindState.NotBound,clientMap.BindState);
    Assert.AreEqual(BindState.NotBound,serverMap.BindState);
    
    Assert.AreEqual(BindState.NotBound,clientList.BindState);
    Assert.AreEqual(BindState.NotBound,serverList.BindState);
    
    Assert.AreEqual(BindState.NotBound,clientProperty.BindState);
    Assert.AreEqual(BindState.NotBound,serverProperty.BindState);
    
    Assert.AreEqual(BindState.NotBound,clientSet.BindState);
    Assert.AreEqual(BindState.NotBound,serverSet.BindState);
  }

  [Test]
  public void PropertyTest()
  {
    ClientWire.AutoTransmitMode = true;
    ServerWire.AutoTransmitMode = true;

    var serverTopLevelProperty = BindToServer(LifetimeDefinition.Lifetime,NewRdProperty<RdProperty<RdProperty<RdSet<int>>>>(), ourKey);
    serverTopLevelProperty.ValueCanBeNull = true;
    var clientTopLevelProperty = BindToClient(LifetimeDefinition.Lifetime,NewRdProperty<RdProperty<RdProperty<RdSet<int>>>>(), ourKey);
    clientTopLevelProperty.ValueCanBeNull = true;

    var clientNested1 = NewRdProperty<RdProperty<RdSet<int>>>();
    var clientNested2 = NewRdProperty<RdSet<int>>();
    var clientSet = NewRdSet<int>();
    clientNested2.Value = clientSet;
    clientNested1.Value = clientNested2;

    RdProperty<RdProperty<RdSet<int>>> serverProperty = null;

    SetSchedulerActive(SchedulerKind.Server, () =>
    {
      serverTopLevelProperty.View(LifetimeDefinition.Lifetime, (mapLifetime, property) =>
      {
        serverProperty = property;
      });
    });

    SetSchedulerActive(SchedulerKind.Client, () =>
    {
      clientTopLevelProperty.Value = clientNested1;
    });
    PumpAllProtocols(false);

    Assert.NotNull(serverProperty);
  }
  
  
  private void PumpAllProtocols(bool transmitMessages)
  {
    var clientScheduler = (PumpScheduler)ClientProtocol.Scheduler;
    var serverScheduler = (PumpScheduler)ServerProtocol.Scheduler;

    do
    {
      if (transmitMessages)
      {
        if (ClientWire.HasMessages)
          ClientWire.TransmitAllMessages();
        
        if (ServerWire.HasMessages)
          ServerWire.TransmitAllMessages();
      }

    } while (clientScheduler.PumpOnce() || serverScheduler.PumpOnce() || ClientWire.HasMessages || ServerWire.HasMessages);
  }

  public override void TearDown()
  {
    ((PumpScheduler)ClientProtocol.Scheduler).Dispose();
    ((PumpScheduler)ServerProtocol.Scheduler).Dispose();
    
    base.TearDown();
  }


  private class MyScheduler : IScheduler
  {
    private readonly Thread myThread;
    private int myCount;

    public MyScheduler(Thread thread) { myThread = thread; }

    public void Queue(Action action)
    {
      AssertThread();
      
      myCount++;
      try
      {
        action();
      }
      finally
      {
        myCount--;
      }
    }

    private void AssertThread()
    {
      Assert.AreEqual(myThread, Thread.CurrentThread,
        $"Wrong thread. Expected: {myThread.ToThreadString()}, Actual: {Thread.CurrentThread.ToThreadString()}");
    }

    public bool IsActive
    {
      get
      {
        AssertThread();
        return myCount > 0;
      }
    }

    public bool OutOfOrderExecution => false;
  }
  
  private class PumpScheduler : IScheduler, IDisposable
  {
    private readonly Queue<Action> myActions = new();
    private bool myDisposed = false; 
    private readonly Thread myThread;
    private int myCount;

    public PumpScheduler(Thread thread)
    {
      myThread = thread;
    }

    public void Queue(Action action)
    {
      AssertThread();

      if (myDisposed)
      {
        action();
        return;
      }

      myActions.Enqueue(() =>
      {
        myCount++;
        try
        {
          action();
        }
        finally
        {
          myCount--;
        }
      });
    }

    private void AssertThread()
    {
      Assert.AreEqual(myThread, Thread.CurrentThread, $"Wrong thread. Expected: {myThread.ToThreadString()}, Actual: {Thread.CurrentThread.ToThreadString()}");
    }

    public void SetActive(Lifetime lifetime)
    {
      AssertThread();
      lifetime.Bracket(() =>
      {
        return myCount++;
      }, () =>
      {
        AssertThread();
        myCount--;
      });
    }

    public bool PumpOnce()
    {
      AssertThread();
      if (myActions.TryDequeue(out var action))
      {
        action();
        return true;
      }

      return false;
    }

    public void PumpAll()
    {
      while (PumpOnce())
      {
      }
    }

    public bool IsActive
    {
      get
      {
        AssertThread();
        return myCount > 0;
      }
    }

    public bool OutOfOrderExecution => false;

    public void Dispose()
    {
      myCount = 1;
      myDisposed = true;
    }
  }
}