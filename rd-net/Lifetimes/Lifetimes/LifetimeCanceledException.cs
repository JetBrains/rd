using System;
using JetBrains.Annotations;

namespace JetBrains.Lifetimes
{
  /// <summary>
  /// Inheritor of <see cref="OperationCanceledException"/> which is thrown by <see cref="Lifetimes.Lifetime.ThrowIfNotAlive"/>.
  /// </summary>
  public class LifetimeCanceledException : OperationCanceledException
  {
    [PublicAPI] public Lifetime Lifetime { get; }

    public LifetimeCanceledException(Lifetime lifetime) : base(
      lifetime.Definition.ToCancellationToken(true)
      )
    {
      if (lifetime.IsAlive) 
        throw new InvalidOperationException($"It's not allowed to create `{nameof(LifetimeCanceledException)}` over alive lifetime: {lifetime}");
      
      Lifetime = lifetime;
    }
  }
}