using System;

namespace JetBrains.Util
{
  public static class EmptyAction
  {
    public static readonly Action Instance = () => { };
  }

  // ReSharper disable StaticFieldInGenericType

  public static class EmptyAction<T>
  {
    public static readonly Action<T> Instance = t => { };
  }

  public static class EmptyAction<T1, T2>
  {
    public static readonly Action<T1, T2> Instance = (t1, t2) => { };
  }

  public static class EmptyAction<T1, T2, T3>
  {
    public static readonly Action<T1, T2, T3> Instance = (t1, t2, t3) => { };
  }

  // ReSharper restore StaticFieldInGenericType
}