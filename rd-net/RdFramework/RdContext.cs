using System;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using JetBrains.Util;

#nullable disable

namespace JetBrains.Rd
{
  public abstract class RdContextBase : IEquatable<RdContextBase>
  {
    [NotNull] public readonly string Key;
    public readonly bool IsHeavy;

    protected RdContextBase(string key, bool isHeavy)
    {
      Key = key;
      IsHeavy = isHeavy;
    }

    public static RdContextBase Read(SerializationCtx ctx, UnsafeReader reader)
    {
      var x = 1;
      return ctx.Serializers.Read<RdContextBase>(ctx, reader);
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdContextBase value)
    {
      ctx.Serializers.Write(ctx, writer, value);
    }

    public bool Equals(RdContextBase other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Key == other.Key;
    }

    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((RdContextBase) obj);
    }

    public override int GetHashCode() => Key.GetHashCode();

    public static bool operator ==(RdContextBase left, RdContextBase right) => Equals(left, right);

    public static bool operator !=(RdContextBase left, RdContextBase right) => !Equals(left, right);

    protected internal abstract void RegisterOn(ProtocolContexts contexts);
    protected internal abstract void RegisterOn(ISerializers serializers);
    
    internal abstract object ValueBoxed { get; }

    /// <summary>
    /// Sets current value to <paramref name="newValue"/> and returns a cookie which should restore old value on disposing
    /// </summary>
    public abstract IDisposable UpdateValueBoxed(object newValue);
  }

  /// <summary>
  /// Describes a context and provides access to value associated with this context.
  /// The associated value is thread-local and synchronized between send/advise pairs on <see cref="IWire"/>. The associated value will be the same in handler method in <see cref="IWire.Advise"/> as it was in <see cref="IWire.Send"/>.
  /// Instances of this class with the same <see cref="RdContext.Key"/> will share the associated value.
  /// Best practice is to declare contexts in toplevel entities in protocol model using <c>Toplevel.context</c>. Manual declaration is also possible.
  /// </summary>
  /// <typeparam name="T">The type of value stored by this context</typeparam>
  public abstract class RdContext<T> : RdContextBase
  {
    [NotNull] public readonly CtxReadDelegate<T> ReadDelegate;
    [NotNull] public readonly CtxWriteDelegate<T> WriteDelegate;

    /// <summary>
    /// 
    /// </summary>
    /// <param name="key">Textual name of this context. This is used to match this with protocol counterparts</param>
    /// <param name="isHeavy">Whether or not this context is heavy. A heavy context maintains a value set and interns values. A light context sends values as-is and does not maintain a value set.</param>
    /// <param name="readDelegate">Serializer to be used with this context.</param>
    /// <param name="writeDelegate">Serializer to be used with this context.</param>
    protected RdContext(string key, bool isHeavy, [NotNull] CtxReadDelegate<T> readDelegate,
      [NotNull] CtxWriteDelegate<T> writeDelegate) : base(key, isHeavy)
    {
      ReadDelegate = readDelegate;
      WriteDelegate = writeDelegate;
    }

    /// <summary>
    /// Current value for this context
    /// </summary>
    public abstract T Value { get; }

    /// <summary>
    /// Value which is used as a key inside per-context entities like <see cref="RdPerContextMap{K,V}"/>
    /// </summary>
    public virtual T ValueForPerContextEntity => Value;

    internal sealed override object ValueBoxed => Value;

    /// <inheritdoc cref="UpdateValueBoxed"/>
    public abstract IDisposable UpdateValue(T newValue);

    public override IDisposable UpdateValueBoxed(object newValue)
    {
      return UpdateValue((T) newValue);
    }

    protected internal sealed override void RegisterOn(ProtocolContexts contexts) => contexts.RegisterContext(this);
  }

  /// <summary>
  /// Implementation of <see cref="RdContext{T}"/> which uses <see cref="ThreadLocal{T}"/> or <see cref="AsyncLocal{T}"/>
  /// storage for its value and stack
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public abstract class ThreadLocalRdContext<T> : RdContext<T>
  {
    [ThreadStatic] [CanBeNull] private static SingleThreadObjectPool<UpdateValueCookie> ourPool;
    
#if NET35
    private readonly ThreadLocal<T> myValue = new ThreadLocal<T>();
#else
    private readonly AsyncLocal<T> myValue = new();
#endif

    protected ThreadLocalRdContext(string key, bool isHeavy, [NotNull] CtxReadDelegate<T> readDelegate,
      [NotNull] CtxWriteDelegate<T> writeDelegate) : base(key, isHeavy, readDelegate, writeDelegate)
    {
    }
    
    /// <summary>
    /// Current (thread- or async-local) value for this context
    /// </summary>
    public override T Value => myValue.Value;

    public override IDisposable UpdateValue(T newValue)
    {
      var pool = ourPool ??= new SingleThreadObjectPool<UpdateValueCookie>(() => new UpdateValueCookie(), null);
      var updateValueCookie = pool.Rent();
      var oldValue = myValue.Value;
      updateValueCookie.Init(this, oldValue);
      
      myValue.Value = newValue;
      return updateValueCookie;
    }

    private class UpdateValueCookie : IDisposable
    {
      [CanBeNull] private ThreadLocalRdContext<T> myContext;
      private T myOldValue;
      private readonly Thread myThread = Thread.CurrentThread;

      public void Init([NotNull] ThreadLocalRdContext<T> context, T oldValue)
      {
        myContext = context;
        myOldValue = oldValue;
      }

      public void Dispose()
      {
        Assertion.AssertCurrentThread(myThread);
        Assertion.AssertNotNull(myContext);
        myContext.myValue.Value = myOldValue;

        myContext = default;
        myOldValue = default;

        Assertion.AssertNotNull(ourPool);
        ourPool.Return(this);
      }
    }
  }
}
