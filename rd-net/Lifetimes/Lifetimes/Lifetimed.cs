using JetBrains.Annotations;

namespace JetBrains.Lifetimes
{
  /// <summary>
  /// Special kind reference to <see cref="Value"/> 
  /// that automatically nullify it (make <c>default</c> for value types)
  /// when lifetime becomes <see cref="LifetimeStatus.Terminated"/>. 
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public class Lifetimed<T> : ITerminationHandler
  {
    [PublicAPI] public void Deconstruct(out Lifetime lifetime, out T? value)
    {
      lifetime = Lifetime;
      value = Value;
    }

    public Lifetime Lifetime { get; }
    public T? Value { get; private set; }

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