using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Serialization;
using JetBrains.Util;
using JetBrains.Util.Util;
using static System.StringComparer;
using static JetBrains.Rd.Reflection.ReflectionSerializerVerifier;


namespace JetBrains.Rd.Reflection
{
  public static class ReflectionSerializerVerifier
  {
    private static readonly HashSet<Type> ourPrimitiveTypes = new HashSet<Type>()
    {
      typeof(byte),              // serializers.Register(ReadByte, WriteByte, 1);
      typeof(short),             // serializers.Register(ReadShort, WriteShort, 2);
      typeof(int),               // serializers.Register(ReadInt, WriteInt, 3);
      typeof(long),              // serializers.Register(ReadLong, WriteLong, 4);
      typeof(float),             // serializers.Register(ReadFloat, WriteFloat, 5);
      typeof(double),            // serializers.Register(ReadDouble, WriteDouble, 6);
      typeof(char),              // serializers.Register(ReadChar, WriteChar, 7);
      typeof(bool),              // serializers.Register(ReadBool, WriteBool, 8);
      typeof(Unit),              // serializers.Register(ReadVoid, WriteVoid, 9);
      typeof(string),            // serializers.Register(ReadString, WriteString, 10);
      typeof(Guid),              // serializers.Register(ReadGuid, WriteGuid, 11);
      typeof(DateTime),          // serializers.Register(ReadDateTime, WriteDateTime, 12);
      typeof(Uri),               // serializers.Register(ReadUri, WriteUri, 13);
      typeof(RdId),              // serializers.Register(ReadRdId, WriteRdId, 14);
      typeof(RdSecureString),    // serializers.Register(ReadSecureString, WriteSecureString, 15)
      typeof(byte[]),            // serializers.Register(ReadByteArray, WriteByteArray, 31);
      typeof(short[]),           // serializers.Register(ReadShortArray, WriteShortArray, 32);
      typeof(int[]),             // serializers.Register(ReadIntArray, WriteIntArray, 33);
      typeof(long[]),            // serializers.Register(ReadLongArray, WriteLongArray, 34);
      typeof(float[]),           // serializers.Register(ReadFloatArray, WriteFloatArray, 35);
      typeof(double[]),          // serializers.Register(ReadDoubleArray, WriteDoubleArray, 36);
      typeof(char[]),            // serializers.Register(ReadCharArray, WriteCharArray, 37);
      typeof(bool[]),            // serializers.Register(ReadBoolArray, WriteBoolArray, 38);
      typeof(ushort),            // serializers.Register(ReadUShort, WriteUShort, 42);
      typeof(uint),              // serializers.Register(ReadUInt, WriteUInt, 43);
      typeof(ulong),             // serializers.Register(ReadULong, WriteULong, 44);
      typeof(ushort[]),          // serializers.Register(ReadUShortArray, WriteUShortArray, 46);
      typeof(uint[]),            // serializers.Register(ReadUIntArray, WriteUIntArray, 47);
      typeof(ulong[]),           // serializers.Register(ReadULongArray, WriteULongArray, 48);
    };

    private static readonly string ourFakeTupleFullName = typeof(ProxyGenerator.FakeTuple<>).FullName.NotNull().TrimEnd('1');

    public static bool IsPrimitive(Type typeInfo)
    {
      return ourPrimitiveTypes.Contains(typeInfo);
    }

    public static bool CanBeNull(Type type)
    {
      var returnType = type;

      var returnTypeInfo = returnType.GetTypeInfo();
      if (IsNullable(returnTypeInfo, _ => true))
        return true;

      if (returnTypeInfo.IsValueType)
        return false;

      return true;
    }

    private static bool IsFieldType(TypeInfo typeInfo, bool canBeArray = true)
    {
      bool IsValidArray()
      {
        if (!typeInfo.IsArray) return false;
        if (typeInfo.GetArrayRank() != 1) return false;

        var arrayType = typeInfo.GetElementType().GetTypeInfo();
        return IsFieldType(arrayType, false);
      }

      return typeof(IRdBindable).GetTypeInfo().IsAssignableFrom(typeInfo) ||
             IsPrimitive(typeInfo.AsType()) ||
             typeInfo.IsEnum ||
             canBeArray && IsValidArray() ||
             IsNullable(typeInfo, type => IsPrimitive(type) || type.GetTypeInfo().IsEnum);
    }

    private static bool IsNullable(TypeInfo typeInfo, Func<Type, bool> filter)
    {
      return typeInfo.IsValueType &&
             typeInfo.IsGenericType &&
             typeInfo.GetGenericTypeDefinition() == typeof(Nullable<>) &&
             filter(typeInfo.GetGenericArguments()[0]);
    }

    public static bool IsScalar(Type type)
    {
      return !typeof(IRdBindable).IsAssignableFrom(type);
    }

    public static void AssertRoot(TypeInfo type)
    {
      if (!Mode.IsAssertion)
        return;

      if (HasRdExtAttribute(type))
      {
        AssertValidRdExt(type);
        return;
      }

      if (HasRdModelAttribute(type))
      {
        return;
      }

      if (IsScalar(type))
      {
        AssertValidScalar(type);
        return;
      }

      // Generated from DSL models
      if (typeof(IRdBindable).IsAssignableFrom(type) && BuiltInSerializers.HasBuiltInFields(type))
      {
        return;
      }

      throw new InvalidOperationException($"Invalid rd type, can be only RdExt, RdModel or ValueTuple {type.ToString(true)}");
    }

    public static bool IsValueTuple(TypeInfo type)
    {
      if (!type.IsGenericType)
        return false;
      var fullName = type.FullName.NotNull();
      return fullName.StartsWith("System.ValueTuple`") || fullName.StartsWith(ourFakeTupleFullName);
    }

    public static bool HasRdExtAttribute(TypeInfo type)
    {
      var rdModelAttribute = type.GetCustomAttribute<RdExtAttribute>();
      var isRdModel = rdModelAttribute != null;
      return isRdModel;
    }

    public static void AssertValidRdExt(TypeInfo type)
    {
      if (!Mode.IsAssertion)
        return;

      var isRdModel = HasRdExtAttribute(type);
      Assertion.Assert(isRdModel, $"Error in {type.ToString(true)} model: no {nameof(RdExtAttribute)} attribute specified");
      Assertion.Assert(!type.IsValueType, $"Error in {type.ToString(true)} model: can't be ValueType");
      Assertion.Assert(typeof(RdExtReflectionBindableBase).GetTypeInfo().IsAssignableFrom(type.AsType()), $"Error in {type.ToString(true)} model: should be inherited from {nameof(RdExtReflectionBindableBase)}");

      // actually, it is possible, but error-prone.
      // you may have non-rdmodel base class and several sealed derivatives from it.
      // commented sealed check to avoid annoying colleagues.
      // Assertion.Assert(type.IsSealed, $"Error in {type.ToString(true)} model: RdModels must be sealed.");

      var extMembers = SerializerReflectionUtil.GetSerializableFields(type);
      var rpcInterface = GetRpcInterface(type);
      if (rpcInterface != null)
      {
        var rpc = new HashSet<string>(ProxyGenerator.GetBindableFieldsNames(rpcInterface), StringComparer.Ordinal);
        var ext = new HashSet<string>(extMembers.Select(f => f.Name), StringComparer.Ordinal);
        foreach (var name in GetMethodsMap(type, rpcInterface).Select(ProxyGenerator.ProxyFieldName)) 
          ext.Add(name);

        ext.SymmetricExceptWith(rpc);
        if (ext.Count > 0)
        {
          var msg = new StringBuilder("The list of BindableChildren available in RdExt and exposed by the RdRpc interface are different. Some of the members will not be connected to the counterpart during execution: ");
          foreach (var diff in ext)
          {
            msg.Append(diff)
              .Append(rpc.Contains(diff) ? "(missing in RdExt)" : "(missing in RdRpc interface)")
              .Append(',');
          }
          msg[msg.Length - 1] = '.';
          Assertion.Fail(msg.ToString());
        }
      }
    }

    public static bool HasRdModelAttribute(TypeInfo type)
    {
      var modelAttribute = type.GetCustomAttribute<RdModelAttribute>();
      var isDataModel = modelAttribute != null;
      return isDataModel;
    }

    public static void AssertValidScalar(TypeInfo type)
    {
      if (!Mode.IsAssertion)
        return;

      if (typeof(Delegate).IsAssignableFrom(type))
      {
        Assertion.Fail("Delegates cannot be serialized.");
      }

      if (HasRdModelAttribute(type) || HasRdExtAttribute(type))
      {
        Assertion.Fail($"Scalar type {type.ToString(true)} is invalid. {nameof(RdExtAttribute)} and {nameof(RdModelAttribute)} are not applicable to scalars since they can't be bound to the protocol.");
      }
      var fields = type.GetFields(BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public);
      foreach (var field in fields)
      {
        Assertion.Assert(
          IsScalar(field.FieldType),
          $"Expected to be scalar field: {type.ToString(true)}.{field.Name}." +
          "Scalar types cannot be bindable and have a bindable fields (type { " +
          $" Check requirements in {nameof(ReflectionSerializerVerifier)}.{nameof(IsFieldType)}");
      }
    }

    public static Type GetImplementingType(TypeInfo typeInfo)
    {
      if (!typeInfo.IsGenericType) return typeInfo.AsType();

      var genericDefinition = typeInfo.GetGenericTypeDefinition();
      if (genericDefinition == typeof(IViewableProperty<>)) return typeof(RdProperty<>).MakeGenericType(typeInfo.GetGenericArguments());

      if (genericDefinition == typeof(ISignal<>)) return typeof(RdSignal<>).MakeGenericType(typeInfo.GetGenericArguments());
      if (genericDefinition == typeof(IViewableSet<>)) return typeof(RdSet<>).MakeGenericType(typeInfo.GetGenericArguments());
      if (genericDefinition == typeof(IViewableList<>)) return typeof(RdList<>).MakeGenericType(typeInfo.GetGenericArguments());

      if (genericDefinition == typeof(IViewableMap<,>)) return typeof(RdMap<,>).MakeGenericType(typeInfo.GetGenericArguments());

      if (genericDefinition == typeof(IRdCall<,>)) return typeof(RdCall<,>).MakeGenericType(typeInfo.GetGenericArguments());

      return typeInfo.AsType();
    }


    public static bool IsRpcAttributeDefined(Type @interface)
    {
      return @interface.IsDefined(typeof(RdRpcAttribute), false);
    }

    public static Type? GetRpcInterface(TypeInfo typeInfo)
    {
      if (typeInfo.GetCustomAttribute<RdExtAttribute>() is RdExtAttribute rdExt && rdExt.RdRpcInterface != null)
        return rdExt.RdRpcInterface;

      foreach (var @interface in typeInfo.GetInterfaces()) 
        if (IsRpcAttributeDefined(@interface)) 
          return @interface;

      return null;
    }

    public static IEnumerable<MethodInfo> GetMethodsMap(TypeInfo typeInfo, Type rpcInterface)
    {
      IEnumerable<MethodInfo> GetInterfaceMap(Type baseInterface)
      {
        return typeInfo.GetInterfaceMap(baseInterface).InterfaceMethods.Where(m => !m.IsSpecialName);
      }

      foreach (var methodInfo in GetInterfaceMap(rpcInterface))
        yield return methodInfo;
      foreach (var baseInterface in rpcInterface.GetInterfaces())
      foreach (var methodInfo in GetInterfaceMap(baseInterface))
      {
        yield return methodInfo;
      }
    }
  }
}