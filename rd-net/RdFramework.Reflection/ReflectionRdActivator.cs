using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Util;
using JetBrains.Util.Util;


namespace JetBrains.Rd.Reflection
{
  /// <summary>
	/// To get maximal performance of  Rd framework you should directly provide serializers to every Model and Property.
	///
	/// Creating models by hand with this approach is tedious and error-prone.  The main idea behind <see
	/// cref="ReflectionRdActivator" /> is to automatically create RdExt and initialize fields and properties with
	/// appropriate serializers.
	/// 
  /// </summary>
  public class ReflectionRdActivator
  {
    private static readonly ILog ourLog = Log.GetLog<ReflectionRdActivator>();

    private readonly ReflectionSerializers mySerializers;
    private readonly IProxyGenerator myProxyGenerator;
    private readonly ITypesCatalog? myTypesCatalog;

    public ReflectionSerializers Serializers => mySerializers;

    public IProxyGenerator Generator => myProxyGenerator;

    public ITypesCatalog? TypesCatalog => myTypesCatalog;

    /// <summary>
    /// current activation stack.
    ///
    /// used to protect from circular dependencies only.
    /// </summary>
    [ThreadStatic]
    private static Queue<Type>? myCurrentActivationChain;

    public ReflectionRdActivator(ReflectionSerializers serializers, ITypesCatalog? typesCatalog)
      : this(serializers, new ProxyGenerator(), typesCatalog)
    {
    }

    public ReflectionRdActivator(ReflectionSerializers serializers, IProxyGenerator proxyGenerator, ITypesCatalog? typesCatalog)
    {
      mySerializers = serializers;
      myTypesCatalog = typesCatalog;
      myProxyGenerator = proxyGenerator;
    }

    /// <summary>
    /// Create and bind class with <see cref="RdExtAttribute"/>
    /// </summary>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    public T ActivateBind<T>(Lifetime lifetime, IProtocol protocol) where T : RdBindableBase
    {
      var instance = Activate<T>();

      var typename = GetTypeName(typeof(T));
      instance.Identify(protocol.Identities, protocol.Identities.Mix(RdId.Root, typename));
      instance.BindTopLevel(lifetime, protocol, typename);

      return instance;
    }

    /// <summary>
    /// Create and bind class with <see cref="RdExtAttribute"/>
    /// </summary>
    /// <returns></returns>
    [PublicAPI]
    public RdExtReflectionBindableBase ActivateBind(Type type, Lifetime lifetime, IProtocol protocol)
    {
      var instance = Activate(type);

      var typename = GetTypeName(type);
      var bindable = (RdExtReflectionBindableBase) instance;
      bindable.Identify(protocol.Identities, protocol.Identities.Mix(RdId.Root, typename));
      bindable.BindTopLevel(lifetime, protocol, typename);

      return bindable;
    }


    /// <summary>
    /// Creates and initializes reactive primitives, RdModels and RdExts.
    /// </summary>
    public T Activate<T>()
    {
      return (T) Activate(typeof(T));
    }

    /// <summary>
    /// Activate <see cref="RdExtAttribute"/> or <see cref="RdModelAttribute"/> or its members.
    /// </summary>
    public object Activate(Type type, string name)
    {
      if (Mode.IsAssertion)
      {
        myCurrentActivationChain = myCurrentActivationChain ?? new Queue<Type>();
        myCurrentActivationChain.Clear(); // clear previous attempts to activate different types
      }

      // We should register serializer for current type and all of it members to have possibility to get valid serializers for arguments.
      myTypesCatalog?.AddType(type);

      return ActivateMember(type, name).NotNull();
    }

    /// <summary>
    /// Activate <see cref="RdExtAttribute"/> or <see cref="RdModelAttribute"/> or its members.
    /// </summary>
    public object Activate(Type type)
    {
      return Activate(type, "Anonymous");
    }

    private object ActivateRd(Type type)
    {
      if (Mode.IsAssertion)
      {
        Assertion.AssertNotNull(myCurrentActivationChain);
        Assertion.Assert(!myCurrentActivationChain.Contains(type),
            $"Unable to activate {type.FullName}: circular dependency detected: {string.Join(" -> ", myCurrentActivationChain.Select(t => t.FullName).ToArray())}");
        myCurrentActivationChain.Enqueue(type);
      }

      var typeInfo = type.GetTypeInfo();
      var implementingType = ReflectionSerializerVerifier.GetImplementingType(typeInfo);
      if (Mode.IsAssertion) Assertion.Assert(typeof(RdBindableBase).GetTypeInfo().IsAssignableFrom(implementingType),
        $"Unable to activate {type.FullName}: type should be {nameof(RdBindableBase)}");

      object instance;
      try
      {
        instance = Activator.CreateInstance(implementingType);
      }
      catch (MissingMethodException e)
      {
        throw new MissingMethodException($"Unable to create instance of: {implementingType.ToString(true)}.{e.Message}");
      }

      ReflectionInitInternal(instance);

      if (Mode.IsAssertion)
        myCurrentActivationChain!.Dequeue();

      return instance;
    }

    public object ReflectionInit(object instance)
    {
      if (Mode.IsAssertion)
      {
        myCurrentActivationChain = myCurrentActivationChain ?? new Queue<Type>();
        myCurrentActivationChain.Clear(); // clear previous attempts to activate different types
      }

      return ReflectionInitInternal(instance);
    }

    private object ReflectionInitInternal(object instance)
    {
      var typeInfo = instance.GetType().GetTypeInfo();

      if (ReflectionSerializerVerifier.HasRdExtAttribute(instance.GetType().GetTypeInfo()))
        ReflectionSerializerVerifier.AssertValidRdExt(typeInfo);

      foreach (var mi in SerializerReflectionUtil.GetSerializableFields(typeInfo))
      {
        var currentValue = ReflectionUtil.GetGetter(mi)(instance);
        if (currentValue == null)
        {
          currentValue = ActivateMember(ReflectionUtil.GetReturnType(mi), mi.Name);
          var memberSetter = ReflectionUtil.GetSetter(mi);
          memberSetter(instance, currentValue);
        }
        else
        {
          var implementingType = ReflectionSerializerVerifier.GetImplementingType(ReflectionUtil.GetReturnType(mi).GetTypeInfo());
          if (Mode.IsAssertion) Assertion.Assert(currentValue.GetType() == implementingType, 
            "Bindable field {0} was initialized with incompatible type. Expected type {1}, actual {2}", 
            mi, 
            implementingType.ToString(true), 
            currentValue.GetType().ToString(true));
        }
      }
      
      // Add RdEndpoint for Impl class (counterpart of Proxy)
      var interfaces = typeInfo.GetInterfaces();
      bool isProxy = interfaces.Contains(typeof(IProxyTypeMarker));
      var rpcInterface = ReflectionSerializerVerifier.GetRpcInterface(typeInfo);

      if (!isProxy && rpcInterface != null)
      {
        var interfaceMethods = ReflectionSerializerVerifier.GetMethodsMap(typeInfo, rpcInterface);

        foreach (var interfaceMethod in interfaceMethods)
        {
          var adapter = myProxyGenerator.CreateAdapter(rpcInterface, interfaceMethod);

          var name = ProxyGenerator.ProxyFieldName(interfaceMethod);
          var requestType = ProxyGenerator.GetRequstType(interfaceMethod)[0];
          EnsureFakeTupleRegistered(requestType);

          var responseNonTaskType = ProxyGenerator.GetResponseType(interfaceMethod, unwrapTask: true);
          var responseType = ProxyGenerator.GetResponseType(interfaceMethod, unwrapTask: false);
          var endPointType = typeof(RdCall<,>).MakeGenericType(requestType, responseNonTaskType);
          var endpoint = ActivateGenericMember(name, endPointType.GetTypeInfo()).NotNull();
          SetAsync(endpoint);
          if (endpoint is RdReactiveBase reactiveBase)
            reactiveBase.ValueCanBeNull = true;
          if (ProxyGenerator.IsSync(interfaceMethod))
          {
            var delType = typeof(Func<,,>).MakeGenericType(typeof(Lifetime), requestType, typeof(RdTask<>).MakeGenericType(responseNonTaskType));
            var @delegate = adapter.CreateDelegate(delType, instance);
            var methodInfo = typeof(ReflectionRdActivator).GetMethod(nameof(SetHandler)).NotNull().MakeGenericMethod(requestType, responseNonTaskType);
            methodInfo.Invoke(null, new[] {endpoint, @delegate});
          }
          else
          {
            if (responseType == typeof(Task))
            {
              var delType = typeof(Func<,,>).MakeGenericType(typeof(Lifetime), requestType, typeof(Task));
              var @delegate = adapter.CreateDelegate(delType, instance);
              var methodInfo = typeof(ReflectionRdActivator).GetMethod(nameof(SetHandlerTaskVoid)).NotNull().MakeGenericMethod(requestType);
              methodInfo.Invoke(null, new[] {endpoint, @delegate});
            }
            else
            {
              var delType = typeof(Func<,,>).MakeGenericType(typeof(Lifetime), requestType, typeof(Task<>).MakeGenericType(responseNonTaskType));
              var @delegate = adapter.CreateDelegate(delType, instance);
              var methodInfo = typeof(ReflectionRdActivator).GetMethod(nameof(SetHandlerTask)).NotNull().MakeGenericMethod(requestType, responseNonTaskType);
              methodInfo.Invoke(null, new[] {endpoint, @delegate});
            }
          }
          var bindableChildren = ((IReflectionBindable)instance).BindableChildren;
          bindableChildren.Add(new KeyValuePair<string, object>(name, endpoint));
        }
      }
      else if (rpcInterface != null)
      {
        foreach (var interfaceMethod in rpcInterface.GetMethods())
        {
          var requestType = ProxyGenerator.GetRequstType(interfaceMethod)[0];
          EnsureFakeTupleRegistered(requestType);
        }
      }

      // Allow initialize to setup bindings to composite properties.
      if (instance is IReflectionBindable reflectionBindable)
      {
        reflectionBindable.EnsureBindableChildren();
        if (reflectionBindable.BindableChildren.Count == 0)
        {
          ourLog.Warn($"{reflectionBindable.GetType().ToString(true)} RdExt without bindable children was activated.");
        }

        reflectionBindable.OnActivated();
      }

      return instance;
    }

    private void EnsureFakeTupleRegistered(Type type)
    {
      Assertion.AssertNotNull(myTypesCatalog, "myPolymorphicTypesCatalog required to be NotNull when RPC is used");
      myTypesCatalog.AddType(type);
    }

    /// <summary>
    /// Wrapper method to simplify search with overload resolution for two methods in RdEndpoint.
    ///
    /// Used for async methods returning generic Task.
    /// </summary>
    public static void SetHandlerTask<TReq, TRes>(RdCall<TReq, TRes> endpoint, Func<Lifetime, TReq, Task<TRes>> handler)
    {
      endpoint.Set(handler);
    }

    /// <summary>
    /// Wrapper method to simplify search with overload resolution for two methods in RdEndpoint.
    ///
    /// Used for async methods returning non-generic Task.
    /// </summary>
    public static void SetHandlerTaskVoid<TReq>(RdCall<TReq, Unit> endpoint, Func<Lifetime, TReq, Task> handler)
    {
      endpoint.SetRdTask((lt, req) => handler(lt, req).ToRdTask());
    }

    /// <summary>
    /// Wrapper method to simplify search with overload resolution for two methods in RdEndpoint.
    ///
    /// Used for sync calls only.
    /// </summary>
    public static void SetHandler<TReq, TRes>(RdCall<TReq, TRes> endpoint, Func<Lifetime, TReq, RdTask<TRes>> handler)
    {
      var scheduler = new SwitchingScheduler(endpoint);
      endpoint.SetRdTask(handler, scheduler, scheduler);
    }
    
    private object? ActivateMember(Type memberType, string memberName)
    {
      var typeInfo = memberType.GetTypeInfo();
      var implementingType = ReflectionSerializerVerifier.GetImplementingType(typeInfo);

      object? result;
      if (implementingType.GetTypeInfo().IsGenericType)
      {
        result = ActivateGenericMember(memberName, typeInfo);
      }
      else if (!ReflectionSerializerVerifier.IsScalar(memberType))
      {
        result = ActivateRd(memberType);
      }
      else
      {
        result = null;
      }

      SetAsync(result);

      return result;
    }

    private static void SetAsync(object? result)
    {
      if (result is IRdReactive activatedBindable)
      {
        //foreach (var _ in mi.GetCustomAttributes(typeof(RdAsyncAttribute), false))
        activatedBindable.Async = true;
      }
    }

    private SerializerPair GetProperSerializer(Type type)
    {
      // registration for all statically known types
      myTypesCatalog?.AddType(type);

      return mySerializers.GetOrRegisterSerializerPair(type, true);
    }

    private object? ActivateGenericMember(string memberName, TypeInfo memberType)
    {
      var implementingType = ReflectionSerializerVerifier.GetImplementingType(memberType);
      var genericDefinition = implementingType.GetGenericTypeDefinition();

      var genericArguments = implementingType.GetTypeInfo().GetGenericArguments();
      var argument = genericArguments[0];
      var serializerPair = GetProperSerializer(argument);
      
      if (genericDefinition == typeof(RdProperty<>) ||
          genericDefinition == typeof(RdSignal<>) ||
          genericDefinition == typeof(RdSet<>))
      {
        var instance = Activator.CreateInstance(implementingType, serializerPair.Reader, serializerPair.Writer);
        if (IsMonomorphic(argument))
          ReflectionUtil.InvokeStaticGeneric(typeof(ReflectionRdActivator), nameof(SetOptimizeNested1), argument, instance);
        return instance;
      }

      if (genericDefinition == typeof(RdList<>))
      {
        var instance = Activator.CreateInstance(implementingType, serializerPair.Reader, serializerPair.Writer, 1L /*nextVersion*/);
        if (IsMonomorphic(argument))
          ReflectionUtil.InvokeStaticGeneric(typeof(ReflectionRdActivator), nameof(SetOptimizeNested1), argument, instance);
        return instance;
      }

      if (genericArguments.Length == 2)
      {
        var argument2 = genericArguments[1];
        var serializerPair2 = GetProperSerializer(argument2);
        var instance = Activator.CreateInstance(implementingType, serializerPair.Reader, serializerPair.Writer, serializerPair2.Reader, serializerPair2.Writer);
        if (IsMonomorphic(argument2))
          ReflectionUtil.InvokeStaticGeneric2(typeof(ReflectionRdActivator), nameof(SetOptimizeNested2), argument, argument2, instance);
        return instance;
      }

      if (genericDefinition == typeof(Nullable<>))
      {
        // already initialized to null
        return null;
      }

      // hack for UProperty & USignal
      if (genericArguments.Length == 1 && typeof(IRdBindable).IsAssignableFrom(implementingType) && implementingType.IsClass)
      {
        foreach (var ctor in implementingType.GetTypeInfo().GetConstructors(BindingFlags.Public | BindingFlags.Instance))
        {
          var parameters = ctor.GetParameters();
          if (parameters.Length == 3 && parameters[0].Name == "id")
          {
            return Activator.CreateInstance(implementingType, memberName, serializerPair.Reader, serializerPair.Writer);
          }
        }
      }

      throw new Assertion.AssertionException($"Unable to activate generic type: {memberType}");

      static bool IsMonomorphic(Type type)
      {
        return type.IsValueType || (!typeof(IRdBindable).IsAssignableFrom(type) && type.IsSealed);
      }
    }

    [UsedImplicitly]
    private static void SetOptimizeNested1<T>(object container) where T : notnull
    {
      if (container is RdProperty<T> property)
      {
        property.OptimizeNested = true;
      }
      else if (container is RdSet<T> set)
      {
        set.OptimizeNested = true;
      }
      else if (container is RdList<T> list)
      {
        list.OptimizeNested = true;
      }
    }

    [UsedImplicitly]
    private static void SetOptimizeNested2<TKey, TValue>(object container) where TKey : notnull
    {
      if (container is RdMap<TKey, TValue> map) 
        map.OptimizeNested = true;
    }

    public static string GetTypeName(Type type)
    {
      var typename = type.AssemblyQualifiedName;
      if (typeof(RdExtReflectionBindableBase).IsAssignableFrom(type))
      {
        var rpcInterface = ReflectionSerializerVerifier.GetRpcInterface(type.GetTypeInfo());
        if (rpcInterface != null)
          return rpcInterface.AssemblyQualifiedName;
      }

      return typename;
    }
  }
}