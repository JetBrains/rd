﻿using System;
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

#nullable disable

namespace JetBrains.Rd.Impl
{
  public class InternRoot<TBase> : IInternRoot<TBase>, IRdWireable
  {
    private readonly ConcurrentDictionary<InternId, TBase> myDirectMap = new();

    private readonly ConcurrentDictionary<TBase, IdPair> myInverseMap = new();
    private int myInternedIdCounter;
    private readonly CtxReadDelegate<TBase> myReadDelegate;
    private readonly CtxWriteDelegate<TBase> myWriteDelegate;

    public InternId TryGetInterned(TBase value)
    {
      var hasValue = myInverseMap.TryGetValue(value, out var pair);
      if (hasValue)
        return pair.Id;
      return InternId.Invalid;
    }

    public InternId Intern(TBase value)
    {
      var proto = TryGetProto();
      if (proto == null || !TryGetSerializationContext(out var serializationCtx))
        return InternId.Invalid;
      
      if (myInverseMap.TryGetValue(value, out var pair))
        return pair.Id;

      pair.Id = pair.ExtraId = InternId.Invalid;

      if (myInverseMap.TryAdd(value, pair))
      {
        InternId allocatedId = new InternId(Interlocked.Increment(ref myInternedIdCounter) * 2);
        
        Assertion.Assert(allocatedId.IsLocal, "Newly allocated ID must be local");
        
        RdReactiveBase.SendTrace?.Log($"InternRoot `{Location}` ({RdId}):: {allocatedId} = {value}");
        
        myDirectMap[allocatedId] = value;
        using(proto.Contexts.CreateSendWithoutContextsCookie())
          proto.Wire.Send(RdId, writer =>
          {
            myWriteDelegate(serializationCtx, writer, value);
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

    public bool TryUnIntern<T>(InternId id, out T result) where T : TBase
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

    public void Remove(TBase value)
    {
      if (myInverseMap.TryRemove(value, out var pair))
      {
        myDirectMap.TryRemove(pair.Id, out _);
        myDirectMap.TryRemove(pair.ExtraId, out _);
      }
    }

    public T UnIntern<T>(InternId id) where T : TBase
    {
      return (T) TryGetValue(id);
    }

    [CanBeNull] private IRdDynamic myParent;

    public InternRoot([CanBeNull] CtxReadDelegate<TBase> readDelegate = null, [CanBeNull] CtxWriteDelegate<TBase> writeDelegate = null)
    {
      myReadDelegate = readDelegate ?? Polymorphic<TBase>.Read;
      myWriteDelegate = writeDelegate ?? Polymorphic<TBase>.Write;
    }

    public IProtocol TryGetProto() => myParent?.TryGetProto();
    public bool TryGetSerializationContext(out SerializationCtx ctx)
    {
      var parent = myParent;
      if (parent != null)
        return parent.TryGetSerializationContext(out ctx);

      ctx = default;
      return default;
    }

    
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
    public bool IsBound => myParent != null;

    public void PreBind(Lifetime lf, IRdDynamic parent, string name)
    {
      if (myParent != null)
      {
        Assertion.Fail($"Trying to bound already bound {this} to {parent.Location}");
      }
      //todo uncomment when fix InterningTest
      //Assertion.Require(RdId != RdId.Nil, "Must be identified first");

      var proto = parent.TryGetProto();
      if (proto == null)
        return;

      lf.TryBracket(() =>
        {
          myParent = parent;
          Location = parent.Location.Sub(name);
      
          myDirectMap.Clear();
          myInverseMap.Clear();
        },
        () =>
        {
          Location = Location.Sub("<<unbound>>", "::");
          myParent = null;
          RdId = RdId.Nil;
        }
      );

      proto.Wire.Advise(lf, this);
    }

    public void Bind()
    {
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

    public bool ValueCanBeNull
    {
      get => false;
      set => throw new NotSupportedException("Intern Roots are always non-nullable");
    }

    public void OnWireReceived(UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
    {
      if (!TryGetSerializationContext(out var serializationCtx))
      {
        RdReactiveBase.ourLogReceived.Trace($"{this} is not bound. Message for ({dispatchHelper.RdId}) will not be processed");
        return;
      }
          
      var value = myReadDelegate(serializationCtx, reader);
      var id = InternId.Read(reader);
      Assertion.Require(!id.IsLocal, "Other side sent us id of our own side?");
      Assertion.Require(id.IsValid, "Other side sent us invalid id?");
      RdReactiveBase.ReceiveTrace?.Log($"InternRoot `{Location}` ({RdId}):: {id} = {value}");
      myDirectMap[id] = value;
      var pair = new IdPair { Id = id, ExtraId = InternId.Invalid };
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
}