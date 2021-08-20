using System;
using JetBrains.Annotations;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd.Reflection
{
  /// <summary>
  /// Abstraction to send <see cref="OuterLifetime"/> over the protocol
  /// </summary>
  public sealed class RdOuterLifetime: RdReactiveBase
  {
    [NonSerialized] private LifetimeDefinition myLifetimeDefinition;
    [NonSerialized] private bool myIsClientSide = false;

    public static implicit operator OuterLifetime (RdOuterLifetime rdOuterLifetime)
    {
      return rdOuterLifetime.myLifetimeDefinition;
    }

    [UsedImplicitly]
    public RdOuterLifetime()
    {
    }

    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);
      myLifetimeDefinition = Lifetime.Define(lifetime);
      myLifetimeDefinition.AllowTerminationUnderExecution = true;
      Proto.Wire.Advise(lifetime, this);

      if (!myIsClientSide) 
        myLifetimeDefinition.Lifetime.OnTermination(() => Proto.Wire.Send(RdId, writer => writer.Write(1)));
    }

    public override void OnWireReceived(UnsafeReader reader)
    {
      myLifetimeDefinition.Terminate();
    }

    /// <summary>
    /// Used on a sender side to 
    /// </summary>
    public void AttachToLifetime(Lifetime lifetime)
    {
      if (!lifetime.TryOnTermination(myLifetimeDefinition))
        myLifetimeDefinition.Terminate();
    }

    #region Intrinsic

    public static RdOuterLifetime Read(SerializationCtx ctx, UnsafeReader reader)
    {
      var id = reader.ReadRdId();
      var rdOuterLifetime = new RdOuterLifetime()
      {
        myIsClientSide = true
      }.WithId(id);
      return rdOuterLifetime;
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdOuterLifetime value)
    {
      writer.Write(value.RdId);
    }

    #endregion
  }
}