using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Reflection.Emit;
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
    private readonly bool myAllowSave;

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

    private readonly Lazy<AssemblyBuilder> myAssemblyBuilder;
    private readonly Lazy<ModuleBuilder> myModuleBuilder;

    public AssemblyBuilder DynamicAssembly => myAssemblyBuilder.Value;
    public ModuleBuilder DynamicModule => myModuleBuilder.Value;

    public ProxyGenerator(bool allowSave = false)
    {
      myAllowSave = allowSave;
#if NETSTANDARD
     myModuleBuilder = new Lazy<ModuleBuilder>(() => myAssemblyBuilder.Value.DefineDynamicModule("ProxyGenerator"));
     myAssemblyBuilder = new Lazy<AssemblyBuilder>(() => AssemblyBuilder.DefineDynamicAssembly(new AssemblyName("ProxyGenerator"), AssemblyBuilderAccess.Run));
#else
      myAssemblyBuilder = new Lazy<AssemblyBuilder>(() => AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName("ProxyGenerator"), allowSave ? AssemblyBuilderAccess.RunAndSave : AssemblyBuilderAccess.Run));
      if (allowSave)
        myModuleBuilder = new Lazy<ModuleBuilder>(() => myAssemblyBuilder.Value.DefineDynamicModule("ProxyGenerator", "RdProxy.dll"));
      else
        myModuleBuilder = new Lazy<ModuleBuilder>(() => myAssemblyBuilder.Value.DefineDynamicModule("ProxyGenerator"));
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
      var rdExtConstructor = typeof(RdExtAttribute).GetConstructors().Single(c => c.GetParameters().Length == 1);
      typebuilder.SetCustomAttribute(new CustomAttributeBuilder(rdExtConstructor, new object[]{ interfaceType }));

      var memberNames = new HashSet<string>(StringComparer.Ordinal);
      ImplementInterface(interfaceType, memberNames, typebuilder);
      foreach (var baseInterface in interfaceType.GetInterfaces())
        ImplementInterface(baseInterface, memberNames, typebuilder);

      void ImplementInterface(Type baseInterface, HashSet<string> hashSet, TypeBuilder typeBuilder)
      {
        foreach (var member in baseInterface.GetMembers(BindingFlags.Instance | BindingFlags.Public))
        {
          if (!hashSet.Add(member.Name))
            throw new ArgumentException($"Duplicate member name: {member.Name}. Method overloads are not supported.");

          ImplementMember(typeBuilder, member);
        }
      }

#if NET35
      return typebuilder.CreateType();
#else
      return typebuilder.CreateTypeInfo();
#endif
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
      Assertion.Assert(!method.IsGenericMethod, "generics are not supported");
      Assertion.Assert(!method.IsStatic, "only instance methods are supported");

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
      for (int pi = 0, fi = 0; pi < parameters.Length; pi++)
      {
        if (parameters[pi].ParameterType == typeof(Lifetime))
        {
          LoadArgument(il, 1 /* external cancellation lifetime in SetHandler */);
        }
        else
        {
          Assertion.Assert(parameters[pi].ParameterType == fields[fi].FieldType, "parameters[pi].ParameterType == fields[i].FieldType");
          il.Emit(OpCodes.Ldarg_2); // value tuple
          il.Emit(OpCodes.Ldfld, fields[fi]);
          fi++;
        }
      }
      // call wrapped method
      il.Emit(OpCodes.Callvirt, method);

      // load Unit result if necessary
      if (method.ReturnType == typeof(void) && IsSync(method))
      {
        il.Emit(OpCodes.Ldsfld, typeof(Unit).GetField(nameof(Unit.Instance)));
      }

      if (IsSync(method))
      {
        // Create RdTask
        il.Emit(OpCodes.Call, returnType.GetMethod(nameof(RdTask<int>.Successful)).NotNull("RdTask<Unit>.Successful not found"));
      }
      else
      {
        // regular task already on stack
      }

      il.Emit(OpCodes.Ret);

#if !NET35
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
#endif

      return methodBuilder;
    }

    public static bool IsSync(MethodInfo impl)
    {
      var returnType = impl.ReturnType;
      return (returnType != typeof(Task)) && (!returnType.IsGenericType || returnType.GetGenericTypeDefinition() != typeof(Task<>));
    }

    private void ImplementMember(TypeBuilder typebuilder, MemberInfo member)
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
          if (member is MethodInfo method && !method.IsSpecialName)
          {
            ImplementMethod(typebuilder, method);
          }
          break;
        case MemberTypes.Property:
          ImplementProperty(typebuilder, ((PropertyInfo)member));
          break;
        default:
          var ex = new InvalidOperationException("Unexpected Member Type bit fields combination.");
          throw ex;
      }
    }

    private void ImplementProperty(TypeBuilder typebuilder, PropertyInfo propertyInfo)
    {
      string MakeBackingFieldName(string propertyName)
      {
        // Debug.Assert((char)GeneratedNameKind.AutoPropertyBackingField == 'k');
        return "<" + propertyName + ">k__BackingField";
      }

      var type = propertyInfo.PropertyType;

      var property = typebuilder.DefineProperty(propertyInfo.Name, PropertyAttributes.HasDefault, type, EmptyArray<Type>.Instance);

      // backing field should be public to be listed in BindableMembers

      var field = typebuilder.DefineField(MakeBackingFieldName(propertyInfo.Name), type, FieldAttributes.Public);

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
      // todo: support more than 7 parameters
      var parameters = method.GetParameters();
      if (parameters.Length == 0)
        return new[] {typeof(Unit)};

      Assertion.Assert(parameters.Length <= 7, "parameters.Length <= 7");
      List<Type> list = new List<Type>();
      foreach (var p in parameters)
      {
        // Lifetime treated as cancellation token
        if (p.ParameterType != typeof(Lifetime) || p.ParameterType == typeof(CancellationToken)) 
          list.Add(p.ParameterType);
      }

      if (list.Count == 0)
        return new[] {typeof(Unit)};

      return new[] {ValueTuples[list.Count - 1].MakeGenericType(list.ToArray()) };
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

    private void ImplementMethod(TypeBuilder typebuilder, MethodInfo method)
    {
      // add field for IRdCall instance
      var requestType = GetRequstType(method)[0];
      var responseType = GetResponseType(method, true);

      Assertion.Assert(!requestType.IsByRef, "ByRef is not supported. ({0}.{1})", typebuilder, requestType);
      Assertion.Assert(!responseType.IsByRef, "ByRef is not supported. ({0}.{1})", typebuilder, responseType);

      var fieldType = typeof(IRdCall<,>).MakeGenericType(requestType, responseType);
      var field = typebuilder.DefineField(ProxyFieldName(method), fieldType , FieldAttributes.Public);

      var isSyncCall = !typeof(IAsyncResult).IsAssignableFrom(method.ReturnType);

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
      if (!isSyncCall)
      {
        // Lifetime
        for (int i = 0; i < parameters.Length; i++)
        {
          if (parameters[i].ParameterType == typeof(Lifetime))
          {
            Assertion.Assert(lifetimeArgument == -1, "Only one lifetime parameter is allowed");
            lifetimeArgument = i;
          }
        }

        if (lifetimeArgument != -1)
          LoadArgument(ilgen, lifetimeArgument + 1);
        else
        {
          var getEternalLifetimeMethod = typeof(Lifetime)
            .GetProperty(nameof(Lifetime.Eternal), BindingFlags.Static | BindingFlags.Public)?.GetGetMethod();
          ilgen.Emit(OpCodes.Call, getEternalLifetimeMethod);
        }
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

        // create tuple and load it to stack
        ilgen.Emit(OpCodes.Newobj, requestType.GetConstructors().Single());
      }
      else
      {
        ilgen.Emit(OpCodes.Ldsfld, typeof(Unit).GetField(nameof(Unit.Instance)));
      }

      if (isSyncCall)
      {
        Assertion.Assert(lifetimeArgument == -1, "Unable to implement proxy method {0}. CancellationToken or Lifetime can't be used with sync methods.", method);
        ilgen.Emit(OpCodes.Ldnull); // RpcTimeouts
        ilgen.Emit(OpCodes.Callvirt, fieldType.GetMethod(nameof(IRdCall<int,int>.Sync)).NotNull("fieldType.GetMethod(Sync) != null"));
      }
      else
      {
        // Start(Lifetime, TReq, Scheduler)
        var startMethod = fieldType.GetMethods().Single(info => info.Name == nameof(IRdCall<int, int>.Start) && info.GetParameters().Length == 3);

        // async
        ilgen.Emit(OpCodes.Ldnull); // ResponseScheduler
        ilgen.Emit(OpCodes.Callvirt, startMethod.NotNull("fieldType.GetMethod(Start) != null"));

        ilgen.Emit(OpCodes.Call, (typeof(ProxyGeneratorUtil))
          .GetMethod(nameof(ProxyGeneratorUtil.ToTask)).NotNull("No ToTask method").MakeGenericMethod(responseType));
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

    public static string ProxyFieldName(MethodInfo method)
    {
      return method.Name + "_proxy";
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
  }
}
