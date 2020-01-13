using System;
using JetBrains.Annotations;

namespace JetBrains.Lifetimes
{
  public class Lifetimed<T> : ITerminationHandler
  {
    [PublicAPI] public void Deconstruct(out Lifetime lifetime, out T value)
    {
      lifetime = Lifetime;
      value = Value;
    }

    public Lifetime Lifetime { get; }
    public T Value { get; private set; }

    public Lifetimed(Lifetime lifetime, T value)
    {
      Lifetime = lifetime;

      using (var cookie = lifetime.UsingExecuteIfAlive())
      {
        if (!cookie.Succeed) return;

        Value = value;
        lifetime.OnTermination(this);
      }
    }

    void ITerminationHandler.OnTermination(Lifetime lifetime) => Value = default;
  }
}