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
    private readonly ConcurrentDictionary<int, object> myInternedObjects = new ConcurrentDictionary<int, object>();
    private readonly ConcurrentDictionary<int, object> myOtherSideInternedObjects = new ConcurrentDictionary<int, object>();
    private readonly ConcurrentDictionary<object, IdPair> myInverseMap = new ConcurrentDictionary<object, IdPair>();

    public bool TryGetInterned(object value, out int result)
    {
      var hasValue = myInverseMap.TryGetValue(value, out var pair);
      result = hasValue ? pair.Id : default;
      return hasValue;
    }

    public int Intern(object value)
    {
      IdPair pair;
      if (myInverseMap.TryGetValue(value, out pair))
        return pair.Id;
      
      Proto.Wire.Send(RdId, writer =>
      {
        writer.Write((byte) MessageType.SetIntern);
        Polymorphic<object>.Write(SerializationContext, writer, value);
        int allocatedId;
        lock (myInternedObjects)
        {
          allocatedId = myInternedObjects.Count * 2;
          myInternedObjects[allocatedId / 2] = value;          
        }

        pair.Id = allocatedId;
        pair.ExtraId = -1;
        writer.Write(allocatedId);
      });

      if (myInverseMap.TryAdd(value, pair))
        SendConfirmIndex(pair.Id);
      else
      {
        SendEraseIndex(pair.Id);
        pair = myInverseMap[value];
      }

      return pair.Id;
    }

    private void SendConfirmIndex(int index)
    {
      Proto.Wire.Send(RdId, writer =>
      {
        writer.Write((byte) MessageType.ConfirmIndex);
        writer.Write(index);
      });
    }

    private void SendEraseIndex(int index)
    {
      Proto.Wire.Send(RdId, writer =>
      {
        writer.Write((byte) MessageType.ResetIndex);
        writer.Write(index);
      });
      myInternedObjects.TryRemove(index, out _);
    }

    private bool IsIndexOwned(int idx)
    {
      return (idx & 1) == 0;
    }

    private object TryGetValue(int id)
    {
      object value;
      if (IsIndexOwned(id))
        myInternedObjects.TryGetValue(id / 2, out value);
      else
        myOtherSideInternedObjects.TryGetValue(id / 2, out value);
      return value;
    }

    public void Remove(object value)
    {
      if (myInverseMap.TryRemove(value, out var pair))
      {
        Proto.Wire.Send(RdId, writer =>
        {
          writer.Write((byte) MessageType.RequestRemoval);
          writer.Write(pair.Id);
          writer.Write(pair.ExtraId);
        });
      }
    }

    public T UnIntern<T>(int id)
    {
      return (T) (IsIndexOwned(id) ? myInternedObjects[id / 2] : myOtherSideInternedObjects[id / 2]);
    }
    
    private void HandleSetIntern(UnsafeReader reader)
    {
      var value = Polymorphic<object>.Read(SerializationContext, reader);
      var id = reader.ReadInt();
      Assertion.Require((id & 1) == 0, "Other side sent us id of our own side?");
      myOtherSideInternedObjects[id / 2] = value;
    }

    private void EraseIndex(int index)
    {
      if (IsIndexOwned(index))
        myInternedObjects.TryRemove(index / 2, out _);
      else
        myOtherSideInternedObjects.TryRemove(index / 2, out _);
    }
    
    private void HandleAckRemoval(UnsafeReader reader)
    {
      var id1 = reader.ReadInt();
      var id2 = reader.ReadInt();
      
      EraseIndex(id1 ^ 1);
      if(id2 != -1) EraseIndex(id2 ^ 1);
    }

    private void HandleRequestRemoval(UnsafeReader reader)
    {
      var id1 = reader.ReadInt() ^ 1;
      var id2r = reader.ReadInt();
      var id2 = id2r ^ 1;

      var value = TryGetValue(id1);
      if (value != null)
      {
        if (myInverseMap.TryRemove(value, out var oldPair))
        {
          Assertion.Assert(oldPair.Id == id1 || oldPair.Id == id2, "oldPair.Id == id1 || oldPair.Id == id2");
          Assertion.Assert(oldPair.ExtraId == -1 || oldPair.ExtraId == id1 || oldPair.ExtraId == id2, "oldPair.ExtraId == -1 || oldPair.ExtraId == id1 || oldPair.ExtraId == id2");
        }
        EraseIndex(id1);
        if(id2r != -1) EraseIndex(id2);
      }

      SendAckRemoval(id1, id2r == -1 ? -1 : id2);
    }

    private void SendAckRemoval(int id1, int id2)
    {
      Proto.Wire.Send(RdId, writer =>
      {
        writer.Write((byte) MessageType.AckRemoval);
        writer.Write(id1);
        writer.Write(id2);
      });
    }

    private void HandleConfirmIndex(UnsafeReader reader)
    {
      var id = reader.ReadInt() ^ 1;
      var pair = new IdPair() { Id = id, ExtraId = -1 };
      var value = myOtherSideInternedObjects[id / 2];
      if (myInverseMap.TryAdd(value, pair)) 
        return;
      
      pair = myInverseMap[value];
      pair.ExtraId = id;
      myInverseMap[value] = pair;
    }

    private void HandleResetIndex(UnsafeReader reader)
    {
      var id = reader.ReadInt();
      myOtherSideInternedObjects.TryRemove(id / 2, out _);
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
      var messageType = (MessageType) reader.ReadByte();
      switch (messageType)
      {
        case MessageType.SetIntern:
          HandleSetIntern(reader);
          break;
        case MessageType.ResetIndex:
          HandleResetIndex(reader);
          break;
        case MessageType.ConfirmIndex:
          HandleConfirmIndex(reader);
          break;
        case MessageType.RequestRemoval:
          HandleRequestRemoval(reader);
          break;
        case MessageType.AckRemoval:
          HandleAckRemoval(reader);
          break;
        default:
          throw new ArgumentOutOfRangeException();
      }
    }

    private enum MessageType : byte {
      SetIntern,
      ResetIndex,
      ConfirmIndex,
      RequestRemoval,
      AckRemoval,
    }

    private struct IdPair
    {
      public int Id;
      public int ExtraId;
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