namespace JetBrains.Lifetimes
{
  public struct Lifetimed<T>
  {
    public void Deconstruct(out Lifetime lifetime, out T value)
    {
      lifetime = Lifetime;
      value = Value;
    }

    public Lifetime Lifetime { get; }
    public T Value { get; private set; }

    public Lifetimed(Lifetime lifetime, T value)
    {
      Lifetime = lifetime;
      Value = value;
    }

    public void ClearValueIfNotAlive()
    {
      if (!Lifetime.IsAlive)
        Value = default(T);
    }
  }
}