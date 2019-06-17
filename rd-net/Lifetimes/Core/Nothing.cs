using System;
using JetBrains.Annotations;

namespace JetBrains.Core
{
  /// <summary>
  /// Type that has no instances. Subclass of all classes, so can be casted to any class.
  /// </summary>
  // ReSharper disable once ClassNeverInstantiated.Global
  public class Nothing
  {
    private Nothing() { }

    /// <summary>
    /// Always throws <see cref="InvalidOperationException"/>. Could be used as a return value for unreachable code.
    /// </summary>
    /// <typeparam name="T"></typeparam>
    /// <returns>always fail</returns>
    [PublicAPI] public T As<T>() => throw new InvalidOperationException("This method should never be called. It's only for type checks.");
    
    /// <summary>
    /// Always throws <see cref="InvalidOperationException"/>. Could be used as a return value for unreachable code.
    /// </summary>
    /// <typeparam name="T"></typeparam>
    /// <returns>always fail</returns>
    public static T Unreachable<T>() => throw new InvalidOperationException("This method should never be called. It's must be unreachable for execution flow,");
    
    /// <summary>
    /// Always throws <see cref="InvalidOperationException"/>. Could be used as an assertion in unreachable code.
    /// </summary>
    /// <returns>always fail</returns>
    public static Nothing Unreachable() => throw new InvalidOperationException("This method should never be called. It's unreachable for execution flow.");
  }
}