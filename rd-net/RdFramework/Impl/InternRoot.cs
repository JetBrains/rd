using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Impl
{
  public class InternRoot : IInternRoot
  {
    private readonly List<object> myInternedObjects = new List<object>();
    private readonly ConcurrentDictionary<int, object> myOtherSideInternedObjects = new ConcurrentDictionary<int, object>();
    private readonly ConcurrentDictionary<object, int> myInverseMap = new ConcurrentDictionary<object, int>();

    public bool TryGetInterned(object value, out int result)
    {
      return myInverseMap.TryGetValue(value, out result);
    }

    public int Intern(object value)
    {
      int result;
      if (myInverseMap.TryGetValue(value, out result))
        return result;
      
      Proto.Wire.Send(RdId, writer =>
      {
        Polymorphic<object>.Write(SerializationContext, writer, value);
        lock (myInternedObjects)
        {
          result = myInternedObjects.Count * 2;
          myInternedObjects.Add(value);          
        }
        writer.Write(result);
      });
      
      if (!myInverseMap.TryAdd(value, result)) 
        result = myInverseMap[value];
      
      return result;
    }

    private bool IsIndexOwned(int idx)
    {
      return (idx & 1) == 0;
    }

    public T UnIntern<T>(int id)
    {
      return (T) (IsIndexOwned(id) ? myInternedObjects[id / 2] : myOtherSideInternedObjects[id / 2]);
    }

    public void SetInternedCorrespondence(int id, object value)
    {
      Assertion.Assert(!IsIndexOwned(id), "Setting interned correspondence for object that we should have written, bug?");
      
      myOtherSideInternedObjects[id / 2] = value;
      myInverseMap[value] = id;
    }

    [CanBeNull] private IRdDynamic myParent;

    public IProtocol Proto => myParent.NotNull(this).Proto;
    public SerializationCtx SerializationContext => myParent.NotNull(this).SerializationContext;
    public RName Location { get; private set; } = new RName("<<not bound>>");
    public void Print(PrettyPrinter printer)
    {
      printer.Print(ToString());
      printer.Print("(");
      printer.Print(RdId.ToString());
      printer.Print(")");
    }
    
    public override string ToString()
    {
      return GetType().ToString(false, true) + ": `" + Location+"`";
    }

    public RdId RdId { get; set; }
    
    public void Bind(Lifetime lf, IRdDynamic parent, string name)
    {
      if (myParent != null)
      {
        Assertion.Fail($"Trying to bound already bound {this} to {parent.Location}");
      }
      //todo uncomment when fix InterningTest
      //Assertion.Require(RdId != RdId.Nil, "Must be identified first");
     
      lf.Bracket(() =>
        {
          myParent = parent;
          Location = parent.Location.Sub(name);
        },
        () =>
        {
          Location = Location.Sub("<<unbound>>", "::");
          myParent = null;
          RdId = RdId.Nil;
        }
      );
      
      Proto.Wire.Advise(lf, this);
    }

    public void Identify(IIdentities identities, RdId id)
    {
      Assertion.Require(RdId.IsNil, "Already has RdId: {0}, entity: {1}", RdId, this);      
      Assertion.Require(!id.IsNil, "Assigned RdId mustn't be null, entity: {0}", this);
      
      RdId = id;
    }

    public bool Async { get => true; set => throw new NotSupportedException("Intern Roots are always async"); }
    public IScheduler WireScheduler => InternRootScheduler.Instance;
    public void OnWireReceived(UnsafeReader reader)
    {
      var value = Polymorphic<object>.Read(SerializationContext, reader);
      var id = reader.ReadInt();
      Assertion.Require((id & 1) == 0, "Other side sent us id of our own side?");
      SetInternedCorrespondence(id ^ 1, value);
    }
  }

  class InternRootScheduler : IScheduler
  {
    internal static readonly InternRootScheduler Instance = new InternRootScheduler();
    
    private int myActive = 0;
    public void Queue(Action action)
    {
      Interlocked.Increment(ref myActive);
      try
      {
        action();
      }
      finally
      {
        Interlocked.Decrement(ref myActive);
      }
    }

    public bool IsActive => myActive > 0;
    public bool OutOfOrderExecution => true;
  }
}