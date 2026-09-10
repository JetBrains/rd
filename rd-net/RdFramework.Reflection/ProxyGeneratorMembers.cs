using System;
using System.Linq;
using System.Reflection;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Tasks;

namespace JetBrains.Rd.Reflection;

public class ProxyGeneratorMembers
{
  public static readonly ProxyGeneratorMembers Default = new();

  public static readonly ConstructorInfo OurRdExtConstructor = typeof(RdExtAttribute)
    .GetConstructors()
    .Single(c => c.GetParameters().Length == 1)
    .NotNull(nameof(OurRdExtConstructor));

  public static readonly FieldInfo OurUnitInstance = typeof(Unit)
    .GetField(nameof(Unit.Instance))
    .NotNull(nameof(OurUnitInstance));

  // ReSharper disable once PossibleNullReferenceException
  private static readonly MethodInfo ourEternalLifetimeGetter = typeof(Lifetime)
    .GetProperty(nameof(Lifetime.Eternal), BindingFlags.Static | BindingFlags.Public)
    .GetGetMethod()
    .NotNull(nameof(DefaultCallLifetimeGetter));

  private static readonly MethodInfo ourSyncNested = typeof(ProxyGeneratorUtil)
    .GetMethods()
    .Single(m => m.Name == nameof(ProxyGeneratorUtil.SyncNested) && m.GetParameters().Length == 4)
    .NotNull(nameof(SyncNested));

  private static readonly MethodInfo ourToTask = typeof(ProxyGeneratorUtil)
    .GetMethod(nameof(ProxyGeneratorUtil.ToTask))
    .NotNull(nameof(ToTask));

  private static readonly MethodInfo ourCreateRpcTimeoutsMethod = typeof(ProxyGeneratorUtil)
    .GetMethod(nameof(ProxyGeneratorUtil.CreateRpcTimeouts))
    .NotNull(nameof(CreateRpcTimeoutsMethod));

  private static readonly MethodInfo ourRdTaskSuccessful = typeof(RdTask)
    .GetMethod(nameof(RdTask.Successful))
    .NotNull(nameof(RdTaskSuccessful));

  /// static Lifetime DefaultCallLifetimeGetter()
  public virtual MethodInfo DefaultCallLifetimeGetter => ourEternalLifetimeGetter;

  /// static Task<T> ToTask<T>(IRdTask<T> task)
  public virtual MethodInfo ToTask => ourToTask;

  /// static RpcTimeouts CreateRpcTimeouts(long ticksWarning, long ticksError)
  public virtual MethodInfo CreateRpcTimeoutsMethod => ourCreateRpcTimeoutsMethod;

  /// static RdTask<T> Successful<T>(T result)
  public virtual MethodInfo RdTaskSuccessful => ourRdTaskSuccessful;

  /// static TRes SyncNested<TReq, TRes>(RdCall<TReq, TRes> call, Lifetime lifetime, TReq request, RpcTimeouts? timeouts)
  public virtual MethodInfo SyncNested => ourSyncNested;

  /// IRdTask<TRes> Start(Lifetime lifetime, TReq request, IScheduler? responseScheduler)
  /// or static IRdTask<TRes> Start(IRdCall<TReq, TRes> call, Lifetime lifetime, TReq request, IScheduler? responseScheduler)
  public virtual MethodInfo StartRdCall(Type rdCallType)
  {
    return rdCallType
      .GetMethods()
      .Single(info => info.Name == nameof(IRdCall<int, int>.Start) && info.GetParameters().Length == 3)
      .NotNull(nameof(StartRdCall));
  }
}