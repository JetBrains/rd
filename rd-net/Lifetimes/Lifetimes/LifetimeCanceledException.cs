using System;
using JetBrains.Annotations;

namespace JetBrains.Lifetimes
{
  public class LifetimeCanceledException : OperationCanceledException
  {
    [PublicAPI] public Lifetime Lifetime { get; }

    public LifetimeCanceledException(Lifetime lifetime) : base(
      #if !NET35
      lifetime.Def.ToCancellationToken(true)
      #endif
      )
    {
      if (lifetime.IsAlive) 
        throw new InvalidOperationException($"It's not allowed to create `{nameof(LifetimeCanceledException)}` over alive lifetime: {lifetime}");
      
      Lifetime = lifetime;
    }
  }
}