using System;
using System.Collections.Concurrent;
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
    private readonly ConcurrentDictionary<InternId, object> myDirectMap =
      new ConcurrentDictionary<InternId, object>();

    private readonly ConcurrentDictionary<object, IdPair> myInverseMap = new ConcurrentDictionary<object, IdPair>();
    private int myInternedIdCounter;

    public bool TryGetInterned(object value, out InternId result)
    {
      var hasValue = myInverseMap.TryGetValue(value, out var pair);
      result = hasValue ? pair.Id : default;
      return hasValue;
    }

    public InternId Intern(object value)
    {
      IdPair pair;
      if (myInverseMap.TryGetValue(value, out pair))
        return pair.Id;

      pair.Id = pair.ExtraId = InternId.Invalid;

      if (myInverseMap.TryAdd(value, pair))
      {
        InternId allocatedId = new InternId(Interlocked.Increment(ref myInternedIdCounter) * 2);
        
        Assertion.Assert(allocatedId.IsLocal, "Newly allocated ID must be local");
        
        myDirectMap[allocatedId] = value;
        Proto.Wire.Send(RdId, writer =>
        {
          Polymorphic<object>.Write(SerializationContext, writer, value);
          InternId.Write(writer, allocatedId);
        });

        while (true)
        {
          var oldPair = myInverseMap[value];
          var modifiedPair = oldPair;
          modifiedPair.Id = allocatedId;
          if (myInverseMap.TryUpdate(value, modifiedPair, oldPair)) break;
        }
      }

      return myInverseMap[value].Id;
    }

    private object TryGetValue(InternId id)
    {
      myDirectMap.TryGetValue(id, out var value);
      return value;
    }

    public bool TryUnIntern<T>(InternId id, out T result)
    {
      var value = TryGetValue(id);
      if (value != null)
      {
        result = (T) value;
        return true;
      }

      result = default;
      return false;
    }

    public void Remove(object value)
    {
      if (myInverseMap.TryRemove(value, out var pair))
      {
        myDirectMap.TryRemove(pair.Id, out _);
        myDirectMap.TryRemove(pair.ExtraId, out _);
      }
    }

    public T UnIntern<T>(InternId id)
    {
      return (T) TryGetValue(id);
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
      return GetType().ToString(false, true) + ": `" + Location + "`";
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
      
      myDirectMap.Clear();
      myInverseMap.Clear();

      Proto.Wire.Advise(lf, this);
    }

    public void Identify(IIdentities identities, RdId id)
    {
      Assertion.Require(RdId.IsNil, "Already has RdId: {0}, entity: {1}", RdId, this);      
      Assertion.Require(!id.IsNil, "Assigned RdId mustn't be null, entity: {0}", this);
      
      RdId = id;
    }

    public bool Async
    {
      get => true;
      set => throw new NotSupportedException("Intern Roots are always async");
    }

    public IScheduler WireScheduler => InternRootScheduler.Instance;

    public void OnWireReceived(UnsafeReader reader)
    {
      var value = Polymorphic<object>.Read(SerializationContext, reader);
      var id = InternId.Read(reader);
      Assertion.Require(!id.IsLocal, "Other side sent us id of our own side?");
      Assertion.Require(id.IsValid, "Other side sent us invalid id?");
      myDirectMap[id] = value;
      var pair = new IdPair() { Id = id, ExtraId = InternId.Invalid };
      if (!myInverseMap.TryAdd(value, pair))
      {
        while (true)
        {
          var oldPair = myInverseMap[value];
          Assertion.Assert(!oldPair.ExtraId.IsValid, "Remote send duplicated IDs for value {0}", value);
          var modifiedPair = oldPair;
          modifiedPair.ExtraId = id;
          if (myInverseMap.TryUpdate(value, modifiedPair, oldPair)) break;
        }
      }
    }

    private struct IdPair : IEquatable<IdPair>
    {
      public InternId Id;
      public InternId ExtraId;

      public bool Equals(IdPair other)
      {
        return Id.Equals(other.Id) && ExtraId.Equals(other.ExtraId);
      }

      public override bool Equals(object obj)
      {
        return obj is IdPair other && Equals(other);
      }

      public override int GetHashCode()
      {
        unchecked
        {
          return (Id.GetHashCode() * 397) ^ ExtraId.GetHashCode();
        }
      }

      public static bool operator ==(IdPair left, IdPair right)
      {
        return left.Equals(right);
      }

      public static bool operator !=(IdPair left, IdPair right)
      {
        return !left.Equals(right);
      }
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