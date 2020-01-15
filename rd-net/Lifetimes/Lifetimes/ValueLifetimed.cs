namespace JetBrains.Lifetimes
{
  /// <summary>
  /// Pair of lifetime and value
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public struct ValueLifetimed<T>
  {
    public void Deconstruct(out Lifetime lifetime, out T value)
    {
      lifetime = Lifetime;
      value = Value;
    }

    public Lifetime Lifetime { get; }
    public T Value { get; private set; }

    public ValueLifetimed(Lifetime lifetime, T value)
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