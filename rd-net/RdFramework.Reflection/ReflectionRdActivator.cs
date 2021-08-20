using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Util;
using JetBrains.Util.Util;

#if NET35
using TypeInfo = System.Type;
#else
#endif

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

    [NotNull] private readonly ReflectionSerializersFactory mySerializersFactory;
    [NotNull] private readonly IProxyGenerator myProxyGenerator;
    [CanBeNull] private readonly ITypesCatalog myTypesCatalog;

    [NotNull]
    public ReflectionSerializersFactory SerializersFactory => mySerializersFactory;

    [NotNull]
    public IProxyGenerator Generator => myProxyGenerator;

    [CanBeNull]
    public ITypesCatalog TypesCatalog => myTypesCatalog;

#if JET_MODE_ASSERT
    /// <summary>
    /// current activation stack.
    ///
    /// used to protect from circular dependencies only.
    /// </summary>
    [ThreadStatic]
    private static Queue<Type> myCurrentActivationChain;

#endif

    public ReflectionRdActivator([NotNull] ReflectionSerializersFactory serializersFactory, [CanBeNull] ITypesCatalog typesCatalog)
      : this(serializersFactory, new ProxyGenerator(), typesCatalog)
    {
    }

    public ReflectionRdActivator([NotNull] ReflectionSerializersFactory serializersFactory, [NotNull] IProxyGenerator proxyGenerator, [CanBeNull] ITypesCatalog typesCatalog)
    {
      mySerializersFactory = serializersFactory;
      myTypesCatalog = typesCatalog;
      myProxyGenerator = proxyGenerator;
    }

    /// <summary>
    /// Create and bind class with <see cref="RdExtAttribute"/>
    /// </summary>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [NotNull]
    public T ActivateBind<T>(Lifetime lifetime, [NotNull] IProtocol protocol) where T : RdBindableBase
    {
      var instance = Activate<T>();

      var typename = GetTypeName(typeof(T));
      instance.Identify(protocol.Identities, RdId.Root.Mix(typename));
      instance.Bind(lifetime, protocol, typename);

      return instance;
    }

    /// <summary>
    /// Create and bind class with <see cref="RdExtAttribute"/>
    /// </summary>
    /// <returns></returns>
    [NotNull, PublicAPI]
    public RdExtReflectionBindableBase ActivateBind(Type type, Lifetime lifetime, [NotNull] IProtocol protocol)
    {
      var instance = Activate(type);

      var typename = GetTypeName(type);
      var bindable = (RdExtReflectionBindableBase) instance;
      bindable.Identify(protocol.Identities, RdId.Root.Mix(typename));
      bindable.Bind(lifetime, protocol, typename);

      return bindable;
    }


    /// <summary>
    /// Create and initialize RdModel root and its members (including nested RdModels)
    ///
    /// It doesn't bind model to Protocol. You may want to use <see cref="ActivateBind{T}"/>
    /// </summary>
    public T Activate<T>() where T : RdBindableBase
    {
      return (T) Activate(typeof(T));
    }

    /// <summary>
    /// Activate <see cref="RdExtAttribute"/> and its members.
    /// Can't be used for data models
    /// </summary>
    /// <param name="type"></param>
    /// <returns></returns>
    [NotNull]
    public object Activate(Type type)
    {
      #if JET_MODE_ASSERT
      myCurrentActivationChain = myCurrentActivationChain ?? new Queue<Type>();
      myCurrentActivationChain.Clear(); // clear previous attempts to activate different types
      #endif

      // We should register serializer for current type and all of it members to have possibility to get valid serializers for arguments.
      myTypesCatalog?.AddType(type);

      return ActivateRd(type);
    }

    private object ActivateRd(Type type)
    {
#if JET_MODE_ASSERT
      Assertion.Assert(!myCurrentActivationChain.Contains(type),
        $"Unable to activate {type.FullName}: circular dependency detected: {string.Join(" -> ", myCurrentActivationChain.Select(t => t.FullName).ToArray())}");
      myCurrentActivationChain.Enqueue(type);
#endif

      var typeInfo = type.GetTypeInfo();
      if (!ReflectionSerializerVerifier.IsSealedClassAssignableFromIRdBindable(typeInfo))
        ReflectionSerializerVerifier.AssertEitherExtModelAttribute(typeInfo);

      var implementingType = ReflectionSerializerVerifier.GetImplementingType(typeInfo);
      Assertion.Assert(typeof(RdBindableBase).GetTypeInfo().IsAssignableFrom(implementingType),
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
#if JET_MODE_ASSERT
      myCurrentActivationChain.Dequeue();
#endif
      return instance;
    }

    public object ReflectionInit(object instance)
    {
#if JET_MODE_ASSERT
      myCurrentActivationChain = myCurrentActivationChain ?? new Queue<Type>();
      myCurrentActivationChain.Clear(); // clear previous attempts to activate different types
#endif

      return ReflectionInitInternal(instance);
    }

    private object ReflectionInitInternal(object instance)
    {
      var typeInfo = instance.GetType().GetTypeInfo();

      if (ReflectionSerializerVerifier.HasRdExtAttribute(instance.GetType().GetTypeInfo()))
        ReflectionSerializerVerifier.AssertValidRdExt(typeInfo);

      foreach (var mi in SerializerReflectionUtil.GetBindableMembers(typeInfo))
      {
        ReflectionSerializerVerifier.AssertMemberDeclaration(mi);
        var currentValue = ReflectionUtil.GetGetter(mi)(instance);
        if (currentValue == null)
        {
          currentValue = ActivateRdExtMember(mi);

          var memberSetter = ReflectionUtil.GetSetter(mi);
          memberSetter(instance, currentValue);
        }
        else
        {
          var implementingType = ReflectionSerializerVerifier.GetImplementingType(ReflectionUtil.GetReturnType(mi).GetTypeInfo());
          Assertion.Assert(currentValue.GetType() == implementingType, 
            "Bindable field {0} was initialized with incompatible type. Expected type {1}, actual {2}", 
            mi, 
            implementingType.ToString(true), 
            currentValue.GetType().ToString(true));
        }
      }
      
      // Add RdEndpoint for Impl class (counterpart of Proxy)
      var interfaces = typeInfo.GetInterfaces();
      bool isProxy = interfaces.Contains(typeof(IProxyTypeMarker));
      var rpcInterface = ReflectionSerializersFactory.GetRpcInterface(typeInfo);

      if (!isProxy && rpcInterface != null)
      {
        // Dynamic adapters for Properties are not required, so skip them
        var ignoreMethods = new HashSet<string>(StringComparer.Ordinal);
        List<MethodInfo> interfaceMethods = new List<MethodInfo>();
        void RegisterInterface(Type baseInterface)
        {
          interfaceMethods.AddRange(typeInfo.GetInterfaceMap(baseInterface).InterfaceMethods);
          foreach (var propertyInfo in baseInterface.GetProperties(BindingFlags.Instance | BindingFlags.Public))
          {
            ignoreMethods.Add(propertyInfo.GetSetMethod()?.Name);
            ignoreMethods.Add(propertyInfo.GetGetMethod()?.Name);
          }
        }

        RegisterInterface(rpcInterface);
        foreach (var baseInterface in rpcInterface.GetInterfaces())
          RegisterInterface(baseInterface);

        foreach (var interfaceMethod in interfaceMethods)
        {
          if (ignoreMethods.Contains(interfaceMethod.Name))
            continue;

          var adapter = myProxyGenerator.CreateAdapter(rpcInterface, interfaceMethod);

          var name = ProxyGenerator.ProxyFieldName(interfaceMethod);
          var requestType = ProxyGenerator.GetRequstType(interfaceMethod)[0];
          EnsureFakeTupleRegistered(requestType);

          var responseNonTaskType = ProxyGenerator.GetResponseType(interfaceMethod, unwrapTask: true);
          var responseType = ProxyGenerator.GetResponseType(interfaceMethod, unwrapTask: false);
          var endPointType = typeof(RdCall<,>).MakeGenericType(requestType, responseNonTaskType);
          var endpoint = ActivateGenericMember(name, endPointType.GetTypeInfo());
          SetAsync(interfaceMethod, endpoint);
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
      endpoint.Set((lt, req) => handler(lt, req).ToRdTask());
    }

    /// <summary>
    /// Wrapper method to simplify search with overload resolution for two methods in RdEndpoint.
    ///
    /// Used for sync calls only.
    /// </summary>
    public static void SetHandler<TReq, TRes>(RdCall<TReq, TRes> endpoint, Func<Lifetime, TReq, RdTask<TRes>> handler)
    {
      var scheduler = new SwitchingScheduler(endpoint);
      endpoint.Set(handler, scheduler, scheduler);
    }
    
    [CanBeNull]
    private object ActivateRdExtMember(MemberInfo mi)
    {
      var returnType = ReflectionUtil.GetReturnType(mi);
      var typeInfo = returnType.GetTypeInfo();
      var implementingType = ReflectionSerializerVerifier.GetImplementingType(typeInfo);

      object result;
      if (implementingType.GetTypeInfo().IsGenericType)
      {
        result = ActivateGenericMember(mi.Name, typeInfo);
      }
      else if (!ReflectionSerializerVerifier.IsScalar(returnType))
      {
        result = ActivateRd(returnType);
      }
      else
      {
        result = null;
      }

      SetAsync(mi, result);

      return result;
    }

    private static void SetAsync(MemberInfo mi, object result)
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

      return mySerializersFactory.GetOrRegisterSerializerPair(type, true);
    }

    private object ActivateGenericMember(string memberName, TypeInfo memberType)
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
        return Activator.CreateInstance(implementingType, serializerPair.Reader, serializerPair.Writer);
      }

      if (genericDefinition == typeof(RdList<>))
      {
        return Activator.CreateInstance(implementingType, serializerPair.Reader, serializerPair.Writer, 1L /*nextVersion*/);
      }

      if (genericArguments.Length == 2)
      {
        var argument2 = genericArguments[1];
        var serializerPair2 = GetProperSerializer(argument2);
        var instance = Activator.CreateInstance(implementingType, serializerPair.Reader, serializerPair.Writer, serializerPair2.Reader, serializerPair2.Writer);
        if (ourLog.IsTraceEnabled())
          ourLog.Trace("Create 2-generic: {0}.{1}, TReq poly: {2}, TRes poly: {3}", implementingType.FullName, memberName, serializerPair.IsPolymorphic, serializerPair2.IsPolymorphic);
        if (instance is RdReactiveBase reactive)
          reactive.ValueCanBeNull = true;
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

      throw new Exception($"Unable to activate generic type: {memberType}");
    }

    public static string GetTypeName(Type type)
    {
      var typename = type.FullName;
      if (typeof(RdExtReflectionBindableBase).IsAssignableFrom(type))
      {
        var rpcInterface = ReflectionSerializersFactory.GetRpcInterface(type.GetTypeInfo());
        if (rpcInterface != null)
          return rpcInterface.FullName;
      }

      return typename;
    }
  }
}