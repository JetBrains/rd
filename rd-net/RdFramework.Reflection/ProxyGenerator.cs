using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Reflection.Emit;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Tasks;
using JetBrains.Util;

namespace JetBrains.Rd.Reflection
{
  public class ProxyGenerator : IProxyGenerator
  {
    private static ILog ourLog = Log.GetLog<ProxyGenerator>();

    private const String DynamicAssemblyName = "JetBrains.Rd.ProxyGenerator";
    private readonly bool myAllowSave;

    // todo remove
    /*
     * ValueTuple package does not exist for net35
     */
    public struct FakeTuple<T1> {
      public T1 Item1;
      public FakeTuple(T1 item1) { Item1 = item1; }
    }
    public struct FakeTuple<T1, T2> {
      public T1 Item1; public T2 Item2;
      public FakeTuple(T1 item1, T2 item2) { Item1 = item1; Item2 = item2; }
    }
    public struct FakeTuple<T1, T2, T3> {
      public T1 Item1; public T2 Item2; public T3 Item3;
      public FakeTuple(T1 item1, T2 item2, T3 item3) { Item1 = item1; Item2 = item2; Item3 = item3; }
    }
    public struct FakeTuple<T1, T2, T3, T4> {
      public T1 Item1; public T2 Item2; public T3 Item3; public T4 Item4;
      public FakeTuple(T1 item1, T2 item2, T3 item3, T4 item4) { Item1 = item1; Item2 = item2; Item3 = item3; Item4 = item4; }
    }
    public struct FakeTuple<T1, T2, T3, T4, T5> {
      public T1 Item1; public T2 Item2; public T3 Item3; public T4 Item4; public T5 Item5;
      public FakeTuple(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5) { Item1 = item1; Item2 = item2; Item3 = item3; Item4 = item4; Item5 = item5; }
    }
    public struct FakeTuple<T1, T2, T3, T4, T5, T6> {
      public T1 Item1; public T2 Item2; public T3 Item3; public T4 Item4; public T5 Item5; public T6 Item6;
      public FakeTuple(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6) { Item1 = item1; Item2 = item2; Item3 = item3; Item4 = item4; Item5 = item5; Item6 = item6; }
    }
    public struct FakeTuple<T1, T2, T3, T4, T5, T6, T7> {
      public T1 Item1; public T2 Item2; public T3 Item3; public T4 Item4; public T5 Item5; public T6 Item6; public T7 Item7;
      public FakeTuple(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7) { Item1 = item1; Item2 = item2; Item3 = item3; Item4 = item4; Item5 = item5; Item6 = item6; Item7 = item7; }
    }
    public struct FakeTuple<T1, T2, T3, T4, T5, T6, T7, TRest> {
      public T1 Item1; public T2 Item2; public T3 Item3; public T4 Item4; public T5 Item5; public T6 Item6; public T7 Item7; public TRest Rest;
      public FakeTuple(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, TRest rest) { Item1 = item1; Item2 = item2; Item3 = item3; Item4 = item4; Item5 = item5; Item6 = item6; Item7 = item7; Rest = rest; }
    }

    public static readonly Type[] ValueTuples = new[]
    {
      typeof(FakeTuple<>),
      typeof(FakeTuple<,>),
      typeof(FakeTuple<,,>),
      typeof(FakeTuple<,,,>),
      typeof(FakeTuple<,,,,>),
      typeof(FakeTuple<,,,,,>),
      typeof(FakeTuple<,,,,,,>),  // T1, T2, T3, T4, T5, T6, T7
      typeof(FakeTuple<,,,,,,,>), // T1, T2, T3, T4, T5, T6, T7, TRest
    };

    public const int MaxTuplePayload = 7;

    private static Lazy<ProxyGeneratorMembers> ourLazyMembers => new Lazy<ProxyGeneratorMembers>(() => new ProxyGeneratorMembers());
    private static ProxyGeneratorMembers Members => ourLazyMembers.Value;

    private readonly Lazy<AssemblyBuilder> myAssemblyBuilder;
    private readonly Lazy<ModuleBuilder> myModuleBuilder;

    public AssemblyBuilder DynamicAssembly => myAssemblyBuilder.Value;
    public ModuleBuilder DynamicModule => myModuleBuilder.Value;

    public ProxyGenerator(bool allowSave = false)
    {
      myAllowSave = allowSave;
#if NETSTANDARD
     myAssemblyBuilder = new Lazy<AssemblyBuilder>(() => AssemblyBuilder.DefineDynamicAssembly(new AssemblyName(DynamicAssemblyName), AssemblyBuilderAccess.Run));
     myModuleBuilder = new Lazy<ModuleBuilder>(() => myAssemblyBuilder.Value.DefineDynamicModule(DynamicAssemblyName));
#else
      myAssemblyBuilder = new Lazy<AssemblyBuilder>(() => AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName(DynamicAssemblyName), allowSave ? AssemblyBuilderAccess.RunAndSave : AssemblyBuilderAccess.Run));
      if (allowSave)
        myModuleBuilder = new Lazy<ModuleBuilder>(() => myAssemblyBuilder.Value.DefineDynamicModule(DynamicAssemblyName, "RdProxy.dll"));
      else
        myModuleBuilder = new Lazy<ModuleBuilder>(() => myAssemblyBuilder.Value.DefineDynamicModule(DynamicAssemblyName));
#endif
    }

    public Type CreateType(Type interfaceType)
    {
      if (!interfaceType.IsInterface)
        throw new ArgumentException("Only interfaces are supported.");

      if (interfaceType.GetGenericArguments().Length > 0)
        throw new ArgumentException("Generic interfaces are not supported.");

      // RdRpc attribute can be specified in RdExt attribute. Therefore, now this assert cannot be verified.
      // if (!ReflectionSerializerVerifier.IsRpcAttributeDefined(typeof(TInterface)))
      //   throw new ArgumentException($"Unable to create proxy for {typeof(TInterface)}. No {nameof(RdRpcAttribute)} specified.");

      var moduleBuilder = myModuleBuilder.Value;
      var className = interfaceType.Name.Substring(1);
      var proxyTypeName = "Proxy." + className;
      var typebuilder = moduleBuilder.DefineType(
        proxyTypeName,
        TypeAttributes.NotPublic | TypeAttributes.Class | TypeAttributes.Sealed,
        typeof(RdExtReflectionBindableBase));

      // Implement interface
      typebuilder.AddInterfaceImplementation(interfaceType);

      // mark it as proxy type
      typebuilder.AddInterfaceImplementation(typeof(IProxyTypeMarker));

      // Add RdExt attribute to type
      var rdExtConstructor = Members.RdExtConstructor;
      typebuilder.SetCustomAttribute(new CustomAttributeBuilder(rdExtConstructor, new object[]{ interfaceType }));

      var ctx = new TypeBuilderContext(typebuilder);
      ImplementInterface(interfaceType, ctx);
      foreach (var baseInterface in interfaceType.GetInterfaces())
        ImplementInterface(baseInterface, ctx);

      void ImplementInterface(Type baseInterface, TypeBuilderContext ctx)
      {
        if (baseInterface.GetCustomAttribute<RpcTimeoutAttribute>() is { } timeouts)
        {
          ctx.SetDefaultTimeout(timeouts);
        }

        foreach (var member in baseInterface.GetMembers(BindingFlags.Instance | BindingFlags.Public))
        {
          ImplementMember(ctx, member);
        }
      }

      if (ctx.TimeoutFields.IsValueCreated)
      {
        var cctor = typebuilder.DefineTypeInitializer();
        var il = cctor.GetILGenerator();
        foreach (var kvp in ctx.TimeoutFields.Value)
        {
          // Re-create RpcTimeouts attribute. It used only to re-create RpcTimeouts. It is more robust to use types for
          // ProxyGeneration which are located on our project.
          il.Emit(OpCodes.Ldc_I8, kvp.Value.WarnAwaitTime.Ticks);
          il.Emit(OpCodes.Ldc_I8, kvp.Value.ErrorAwaitTime.Ticks);
          il.Emit(OpCodes.Call, ProxyGeneratorMembers.CreateRpcTimeoutMethod);
          
          // set the static field
          il.Emit(OpCodes.Stsfld, kvp.Key);
        }
        il.Emit(OpCodes.Ret);
      }

      return typebuilder.CreateTypeInfo().NotNull("Unable to create type");
    }


    /// <summary>
    /// Wrap method into Tuple-like adapter for using regular .NEt method as RdCall endpoint.
    ///
    /// Expected signature for sync methods
    ///   (this, Lifetime, TReq) → RdTask{TRes}
    /// async methods:
    ///   (this, Lifetime, TReq) → Task{TRes}
    /// </summary>
    /// <returns></returns>
    public DynamicMethod CreateAdapter(Type selfType, MethodInfo method)
    {
      Assertion.Require(!method.IsGenericMethod, "generics are not supported");
      Assertion.Require(!method.IsStatic, "only instance methods are supported");

      // var type = ModuleBuilder.DefineType(selfType.FullName + "_adapter",
      //   TypeAttributes.Public & TypeAttributes.Sealed & TypeAttributes.Abstract & TypeAttributes.BeforeFieldInit);
      var requestType = GetRequstType(method)[0];
      var responseType = GetResponseType(method, unwrapTask: false);
      Type returnType;
      if (IsSync(method))
      {
        returnType = typeof(RdTask<>).MakeGenericType(responseType);
      }
      else
      {
        returnType = responseType;
      }

      var methodBuilder = new DynamicMethod(method.Name, returnType, new[] { selfType, typeof(Lifetime), requestType }, DynamicModule);
      var il = methodBuilder.GetILGenerator();

      // Invoke adapter method
      il.Emit(OpCodes.Ldarg_0); // this/self
      FieldInfo[] fields;
      if (requestType == typeof(Unit))
      {
        fields = new FieldInfo[0];
      }
      else
      {
        fields = requestType.GetFields();
      }

      var parameters = method.GetParameters();
      for (int parameterIndex = 0, fi = 0; parameterIndex < parameters.Length; parameterIndex++)
      {
        if (parameters[parameterIndex].ParameterType == typeof(Lifetime))
        {
          LoadArgument(il, 1 /* external cancellation lifetime in SetHandler */);
        }
        else
        {
          il.Emit(OpCodes.Ldarg_2); // value tuple
          
          int i = fi;
          var f = fields;
          while (i >= MaxTuplePayload)
          {
            il.Emit(OpCodes.Ldfld, f[MaxTuplePayload]);
            f = f[MaxTuplePayload].FieldType.GetFields();
            i -= MaxTuplePayload;
          }
          if (Mode.IsAssertion)
            Assertion.Assert(parameters[parameterIndex].ParameterType == f[i].FieldType, "parameters[pi].ParameterType == fields[i].FieldType");

          il.Emit(OpCodes.Ldfld, f[i]);

          fi++;
        }
      }
      // call wrapped method
      il.Emit(OpCodes.Callvirt, method);

      // load Unit result if necessary
      if (method.ReturnType == typeof(void) && IsSync(method))
      {
        il.Emit(OpCodes.Ldsfld, Members.UnitInstance);
      }

      if (IsSync(method))
      {
        // Create RdTask
        var taskFactoryMethod = typeof(RdTask).GetMethod(nameof(RdTask.Successful))?.MakeGenericMethod(responseType);
        il.Emit(OpCodes.Call, taskFactoryMethod.NotNull("RdTask.Successful<Unit> not found"));
      }
      else
      {
        // regular task already on stack
      }

      il.Emit(OpCodes.Ret);

/*      if (myAllowSave)
      {
        // shadow methods are required only for reviewing dynamic methods bodies in dotpeek
        var typeBuilder = ModuleBuilder.DefineType(selfType.FullName + "_shadow");
        foreach (var dynamicMethod in result)
        {
          var shadowMethod = typeBuilder.DefineMethod(dynamicMethod.Name, MethodAttributes.Static | MethodAttributes.Public, dynamicMethod.ReturnType, dynamicMethod.GetParameters().Select(p => p.ParameterType).ToArray());
          var body = dynamicMethod.GetMethodBody();
          shadowMethod.SetMethodBody(body.GetILAsByteArray(), body.MaxStackSize, new byte[0], new ExceptionHandler[0], new int[0]);
        }

        typeBuilder.CreateType();
      }*/

      return methodBuilder;
    }

    public static bool IsSync(MethodInfo impl)
    {
      var returnType = impl.ReturnType;
      return (returnType != typeof(Task)) && (!returnType.IsGenericType || returnType.GetGenericTypeDefinition() != typeof(Task<>));
    }

    private void ImplementMember(TypeBuilderContext ctx, MemberInfo member)
    {
      switch (member.MemberType)
      {
        case MemberTypes.Constructor:
          throw new NotSupportedException("Unexpected constructor member in an interface.");
        case MemberTypes.Event:
          throw new NotSupportedException("Events delegation not supported yet.");
        case MemberTypes.Field:
          throw new NotSupportedException("Unexpected field member in an interface.");
        case MemberTypes.Method:
          if (member is MethodInfo { IsSpecialName: false } method)
          {
            ImplementMethod(ctx, method);
          }
          break;
        case MemberTypes.Property:
          ImplementProperty(ctx, ((PropertyInfo)member));
          break;
        default:
          var ex = new InvalidOperationException("Unexpected Member Type bit fields combination.");
          throw ex;
      }
    }

    private void ImplementProperty(TypeBuilderContext ctx, PropertyInfo propertyInfo)
    {
      var typebuilder = ctx.Builder;
      var type = propertyInfo.PropertyType;

      var property = typebuilder.DefineProperty(propertyInfo.Name, PropertyAttributes.HasDefault, type, EmptyArray<Type>.Instance);

      var field = typebuilder.DefineField(MakeBackingFieldName(propertyInfo.Name), type, FieldAttributes.Private);

      if (propertyInfo.GetSetMethod() != null)
      {
        throw new Exception("Setter for properties in proxy interface is prohibited due to unclear semantic");
      }

      if (propertyInfo.GetGetMethod() != null)
      {
        var getMethod = typebuilder.DefineMethod(propertyInfo.GetGetMethod().Name, MethodAttributes.Final | MethodAttributes.Virtual | MethodAttributes.Private, type, EmptyArray<Type>.Instance);
        var il = getMethod.GetILGenerator();
        il.Emit(OpCodes.Ldarg_0);
        il.Emit(OpCodes.Ldfld, field);
        il.Emit(OpCodes.Ret);
        typebuilder.DefineMethodOverride(getMethod, propertyInfo.GetGetMethod());
      }
    }

    /// <summary>
    /// Get the list of tuples, used to
    ///
    /// Note the special treatment of Lifetime and CancellationToken types - they are not included in the result.
    /// </summary>
    /// <param name="method"></param>
    /// <returns></returns>
    public static Type[] GetRequstType(MethodInfo method)
    {
      var parameters = method.GetParameters();
      if (parameters.Length == 0)
        return new[] {typeof(Unit)};

      var parms = new List<Type>(parameters.Length);
      foreach (var p in parameters)
      {
        // Lifetime treats as cancellation token
        if (p.ParameterType != typeof(Lifetime) || p.ParameterType == typeof(CancellationToken))
          parms.Add(p.ParameterType);
      }

      if (parms.Count == 0)
        return new[] {typeof(Unit)};

      Type? type = null;
      while (parms.Count > 0)
      {
        // fold the last aligned to MaxTupleArgs types to the "Rest" value type
        var rest = parms.Count % MaxTuplePayload;
        var toFold = rest == 0 ? MaxTuplePayload : rest;

        var foldTypeArguments = parms.GetRange(parms.Count - toFold, toFold);
        parms.RemoveRange(parms.Count - toFold, toFold);

        if (type != null) foldTypeArguments.Add(type);
        type = ValueTuples[foldTypeArguments.Count - 1].MakeGenericType(foldTypeArguments.ToArray());
      }

      return new[] { type! };
    }

    public static Type GetResponseType(MethodInfo method, bool unwrapTask = false)
    {
      if (method.ReturnType == typeof(void))
        return typeof(Unit);

      if (unwrapTask && !IsSync(method))
      {
        if (method.ReturnType == typeof(Task))
          return typeof(Unit);

        var arguments = method.ReturnType.GetGenericArguments();
        if (arguments.Length == 1)
          return arguments[0];
      }

      return method.ReturnType;
    }

    private void ImplementMethod(TypeBuilderContext ctx, MethodInfo method)
    {
      var typebuilder = ctx.Builder;
      // add field for IRdCall instance
      var requestType = GetRequstType(method)[0];
      var responseType = GetResponseType(method, true);

      Assertion.Require(!requestType.IsByRef, "ByRef is not supported. ({0}.{1})", typebuilder, requestType);
      Assertion.Require(!responseType.IsByRef, "ByRef is not supported. ({0}.{1})", typebuilder, responseType);

      var fieldType = typeof(IRdCall<,>).MakeGenericType(requestType, responseType);
      var field = typebuilder.DefineField(ProxyFieldName(method), fieldType , FieldAttributes.Public);

      var isSyncCall = !typeof(IAsyncResult).IsAssignableFrom(method.ReturnType);

      FieldInfo? timeoutsField = null;
      if (isSyncCall && method.GetCustomAttribute<RpcTimeoutAttribute>() is { } timeouts)
      {
        timeoutsField = ctx.DefineCustomRpcTimeout(timeouts, method);
      }

      var parameters = method.GetParameters();
      MethodBuilder methodbuilder = typebuilder.DefineMethod(method.Name,
        MethodAttributes.Final | MethodAttributes.Virtual | MethodAttributes.Private,
        method.CallingConvention,
        method.ReturnType,
        method.ReturnParameter.GetRequiredCustomModifiers(),
        method.ReturnParameter.GetOptionalCustomModifiers(),
        parameters.Select(param => param.ParameterType).ToArray(),
        parameters.Select(param => param.GetRequiredCustomModifiers()).ToArray(),
        parameters.Select(param => param.GetOptionalCustomModifiers()).ToArray());
      ILGenerator ilgen = methodbuilder.GetILGenerator();

      // load IRdCall field for further call
      ilgen.Emit(OpCodes.Ldarg_0);
      ilgen.Emit(OpCodes.Ldfld, field);

      int lifetimeArgument = -1;
      // Lifetime
      for (int i = 0; i < parameters.Length; i++)
      {
        if (parameters[i].ParameterType == typeof(Lifetime))
        {
          Assertion.Require(lifetimeArgument == -1, "Only one lifetime parameter is allowed");
          lifetimeArgument = i;
        }
      }
      if (lifetimeArgument != -1)
        LoadArgument(ilgen, lifetimeArgument + 1);
      else
      {
        ilgen.Emit(OpCodes.Call, Members.EternalLifetimeGet);
      }

      // TReq
      if (parameters.Length - (lifetimeArgument == -1 ? 0 : 1) > 0)
      {
        // Others arguments, skip `this` argument (0)
        for (int i = 0; i < parameters.Length; i++)
        {
          if (i != lifetimeArgument)
          {
            // load args
            LoadArgument(ilgen, i + 1 /* #0 is `self/this` argument */);
          }
        }

        var genArgs = requestType.GetGenericArguments();
        if (genArgs.Length > MaxTuplePayload)
        {
          var toInit = new Stack<ConstructorInfo>();
          var rest = genArgs[MaxTuplePayload];
          while (rest != null)
          {
            toInit.Push(rest.GetConstructors().Single());
            var ar = rest.GetGenericArguments();
            rest = ar.Length > MaxTuplePayload ? ar[MaxTuplePayload] : null;
          }

          while (toInit.Count != 0)
          {
            ilgen.Emit(OpCodes.Newobj, toInit.Pop());
          }
        }

        // create tuple and load it to stack
        ilgen.Emit(OpCodes.Newobj, requestType.GetConstructors().Single());
      }
      else
      {
        ilgen.Emit(OpCodes.Ldsfld, Members.UnitInstance);
      }

      if (isSyncCall)
      {
        // RpcTimeouts
        var rpcTimeoutsField = timeoutsField ?? ctx.DefaultTimeoutField;
        if (rpcTimeoutsField != null)
          ilgen.Emit(OpCodes.Ldsfld, rpcTimeoutsField);
        else
          ilgen.Emit(OpCodes.Ldnull);

        ilgen.Emit(OpCodes.Call, Members.SyncNested4.MakeGenericMethod(requestType, responseType));
      }
      else
      {
        // Start(Lifetime, TReq, Scheduler)
        var startMethod = ProxyGeneratorMembers.StartRdCall(fieldType);

        // async
        ilgen.Emit(OpCodes.Ldnull); // ResponseScheduler
        ilgen.Emit(OpCodes.Callvirt, startMethod.NotNull("fieldType.GetMethod(Start) != null"));

        ilgen.Emit(OpCodes.Call, Members.ToTask.MakeGenericMethod(responseType));
      }

      if (method.ReturnType == typeof(void))
      {
        ilgen.Emit(OpCodes.Pop);
      }
      else
      {
        // ilgen.Emit(OpCodes.Ldnull);
      }

      ilgen.Emit(OpCodes.Ret);

      typebuilder.DefineMethodOverride(methodbuilder, method);
    }


    private static string MakeBackingFieldName(string propertyName)
    {
      // Debug.Assert((char)GeneratedNameKind.AutoPropertyBackingField == 'k');
      return "<" + propertyName + ">k__BackingField";
    }


    public static string ProxyFieldName(MethodInfo method) => $"{method}_proxy";

    /// <summary>
    /// Return the expected list of names in BindableChildren collection for <see cref="RdRpcAttribute"/> interfaces
    /// </summary>
    public static IEnumerable<string> GetBindableFieldsNames(Type rpcInterface)
    {
      foreach (var i in rpcInterface.GetInterfaces())
      foreach (var fieldName in GetBindableFieldsNames(i))
        yield return fieldName;

      Assertion.Require(rpcInterface.IsInterface, "Interface is expected");
      foreach (var member in rpcInterface.GetMembers(BindingFlags.Instance | BindingFlags.DeclaredOnly | BindingFlags.Public))
      {
        switch (member.MemberType)
        {
          case MemberTypes.Method:
            if (member is MethodInfo { IsSpecialName: false } method)
              yield return ProxyFieldName(method);
            break;

          case MemberTypes.Property:
            yield return MakeBackingFieldName(member.Name);
            break;
        }
      }
    }

    /// <summary>
    /// Loads the given argument on the stack
    /// </summary>
    private static void LoadArgument(ILGenerator ilgen, int nArg)
    {
      switch (nArg)
      {
        case 0:
          ilgen.Emit(OpCodes.Ldarg_0);
          return;
        case 1:
          ilgen.Emit(OpCodes.Ldarg_1);
          return;
        case 2:
          ilgen.Emit(OpCodes.Ldarg_2);
          return;
        case 3:
          ilgen.Emit(OpCodes.Ldarg_3);
          return;
      }

      if (nArg < 0x100)
        ilgen.Emit(OpCodes.Ldarg_S, (byte)nArg);
      else
        ilgen.Emit(OpCodes.Ldarg, (short)nArg);
    }

    private class TypeBuilderContext
    {
      public TypeBuilderContext(TypeBuilder builder)
      {
        Builder = builder;
        TimeoutFields = new (() => new Dictionary<FieldInfo, RpcTimeouts>());
      }

      public TypeBuilder Builder { get; }
      
      public Lazy<Dictionary<FieldInfo, RpcTimeouts>> TimeoutFields { get; }
      public FieldInfo? DefaultTimeoutField { get; private set; }

      public FieldInfo DefineCustomRpcTimeout(RpcTimeoutAttribute timeouts, MethodInfo method) => DefineStaticTimeoutsField(method.Name + "_rpcTimeout", timeouts);

      public void SetDefaultTimeout(RpcTimeoutAttribute timeouts)
      {
        if (DefaultTimeoutField == null)
        {
          var field = DefineStaticTimeoutsField("default__rpcTimeout", timeouts);
          DefaultTimeoutField = field;
        }
        else
          ourLog.Warn($"Default timeout for RdRpc was defined more than once for {Builder.FullName}. The value on the nearest interface will be used.");
      }

      private FieldInfo DefineStaticTimeoutsField(string fieldName, RpcTimeoutAttribute timeouts)
      {
        var timeoutsField = Builder.DefineField(fieldName, typeof(RpcTimeouts), FieldAttributes.Private | FieldAttributes.Static);
        TimeoutFields.Value.Add(timeoutsField, timeouts.Timeout);
        return timeoutsField;
      }
    }
  }

  internal class ProxyGeneratorMembers
  {
    public readonly ConstructorInfo RdExtConstructor = typeof(RdExtAttribute)
      .GetConstructors()
      .Single(c => c.GetParameters().Length == 1)
      .NotNull(nameof(RdExtConstructor));

    public readonly FieldInfo UnitInstance = typeof(Unit)
      .GetField(nameof(Unit.Instance))
      .NotNull(nameof(UnitInstance));

    // ReSharper disable once PossibleNullReferenceException
    public readonly MethodInfo EternalLifetimeGet = typeof(Lifetime)
      .GetProperty(nameof(Lifetime.Eternal), BindingFlags.Static | BindingFlags.Public)
      .GetGetMethod()
      .NotNull(nameof(EternalLifetimeGet));

    public readonly MethodInfo SyncNested4 = typeof(ProxyGeneratorUtil)
      .GetMethods()
      .Single(m => m.Name == nameof(ProxyGeneratorUtil.SyncNested) && m.GetParameters().Length == 4)
      .NotNull(nameof(SyncNested4));

    public MethodInfo ToTask = (typeof(ProxyGeneratorUtil))
      .GetMethod(nameof(ProxyGeneratorUtil.ToTask))
      .NotNull(nameof(ToTask));

    public static readonly MethodInfo CreateRpcTimeoutMethod = typeof(ProxyGeneratorUtil)
      .GetMethod(nameof(ProxyGeneratorUtil.CreateRpcTimeouts))
      .NotNull();

    public static MethodInfo StartRdCall(Type rdCallType)
    {
      return rdCallType.GetMethods().Single(info => info.Name == nameof(IRdCall<int, int>.Start) && info.GetParameters().Length == 3);
    }
  }
}