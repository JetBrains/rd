using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Serialization;
using JetBrains.Util;
using JetBrains.Util.Util;

#if NET35
using TypeInfo = System.Type;
#endif

namespace JetBrains.Rd.Reflection
{
  /**
    <summary>
    Struct, Aggregate, ImmutableList are not supported.

    This enbf-like scheme can only be used to understand basic concepts and terms, there is no
    any strong semantics behind several leaf rules.
    <code>
     //  RdBasic, may have inexact mapping to C# types.
     IType ::= IBindable | IScalar
     Bindable ::= NullableBindable | Array[Bindable] | ImmutableListBindable | Class

     IScalar           ::= NullableScalar | NonNullableScalar
     NullableScalar    ::= Maybe[NonNullableScalar]
     NonNullableScalar ::= List[IScalar] | Array[IScalar] | PredefinedType | Struct

     NonNullableBindable ::= Array[Bindable] | IReadOnlyList[Bindable] | Class
     NonNullable::= NonNullableScalar | NonNullableBindable

     FieldType ::=  IScalar|IType|Aggregate

     RdProperty ::= RdProperty[FieldType]
     RdSet      ::= RdSet[INonNullableScalar]
     RdMap      ::= RdMap[INonNullableScalar, INonNullable]
     RdCall     ::= RdCall[IScalar, IScalar]
     RdSignal   ::= RdSignal[IScalar]

     // C# declarations, [ and ] mean &lt; &gt;.
     FieldDeclaration[T] ::= C#(public readonly? T identifier)
     PropertyDeclaration[T] ::= C#(public T identifier { get; })                   |
																C#(public T identifier { get; private set; })
																// etc.
																//
     PropOrFieldDeclaration[T] ::= FieldDeclaration[T] || PropertyDeclaration[T]
     EnumDeclaration ::= C#(Enum[enum_const*])
     // Not supported. No RdGenerator analogue.
     // StructDeclaration ::= C#(struct field* )

     Member ::= RdSignal | RdProperty| RdList | RdSet | RdMap | RdModel | RdCall
     Declaration ::= BindableDeclaration | Struct | Enum | RdExtDeclaration
     BindableDeclaration ::= TopLevel | Class

     MemberDeclaration ::= PropOrFieldDeclaration[Member]
     RdModelMemberDeclaration ::= PropOrFieldDeclaration[Member|FieldType]

     RdModelDeclaration ::= C#([RdModel] class {RdModelMemberDeclaration*}) | EnumDeclaration | ValueTuple[FieldType{1,7}]
     RdExtDeclaration ::= C#([RdExt] class {MemberDeclaration}* )

     ROOT ::= RdModelDeclaration ROOT | RdExtDeclaration ROOT | Nothing
    </code>
    </summary>
  **/
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

    public static bool IsModelMemberDeclaration(MemberInfo memberInfo)
    {
      var returnType = ReflectionUtil.GetReturnType(memberInfo);
      var typeInfo = returnType.GetTypeInfo();

      return IsFieldType(typeInfo) || IsMemberType(typeInfo);
    }

    public static bool CanBeNull(Type type)
    {
      var returnType = type;

      var returnTypeInfo = returnType.GetTypeInfo();
      if (IsNullable(returnTypeInfo, _ => true))
        return true;

      if (returnTypeInfo.IsValueType)
        return false;
/*
      foreach (var attribute in memberInfo.GetCustomAttributes(false))
        if (attribute is CanBeNullAttribute)
          return true;
*/

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

    private static bool IsNullable([NotNull] TypeInfo typeInfo, Func<Type, bool> filter)
    {
      return typeInfo.IsValueType &&
             typeInfo.IsGenericType &&
             typeInfo.GetGenericTypeDefinition() == typeof(Nullable<>) &&
             filter(typeInfo.GetGenericArguments()[0]);
    }

    public static bool IsMemberDeclaration(MemberInfo memberInfo)
    {
      var returnType = ReflectionUtil.GetReturnType(memberInfo);
      var typeInfo = returnType.GetTypeInfo();
      return IsMemberType(typeInfo);
    }

    private static bool IsMemberType(TypeInfo typeInfo)
    {
      if (typeInfo.IsGenericType)
      {
        var implementingType = GetImplementingType(typeInfo);
        var genericDefinition = implementingType.GetGenericTypeDefinition();

        var arguments = implementingType.GetGenericArguments();
        return genericDefinition == typeof(RdSignal<>) ||
               genericDefinition == typeof(RdProperty<>) ||
               genericDefinition == typeof(RdList<>) ||
               genericDefinition == typeof(RdSet<>) ||
               genericDefinition == typeof(RdMap<,>) ||                          // TResponse can be LiveModel
               (genericDefinition == typeof(RdCall<,>) && IsScalar(arguments[0]) /*&& IsScalar(arguments[1])*/) ||
               // Custom classes support
               (IsSealedClassAssignableFromIRdBindable(typeInfo));
      }

      if (IsScalar(typeInfo))
        return true;

      var hasRdExt = typeInfo.GetCustomAttribute<RdExtAttribute>() != null;
      if (hasRdExt)
        return true;

      var hasRdModel = typeInfo.GetCustomAttribute<RdModelAttribute>() != null;
      if (hasRdModel)
        return true;

      // Custom classes support
      if (IsSealedClassAssignableFromIRdBindable(typeInfo))
        return true;

      return false;
    }

    public static bool IsSealedClassAssignableFromIRdBindable(TypeInfo typeInfo)
    {
      return typeInfo.IsClass && typeInfo.IsSealed && typeof(IRdBindable).IsAssignableFrom(typeInfo);
    }

    public static bool IsScalar(Type type)
    {
      return !typeof(IRdBindable).IsAssignableFrom(type);
    }

    public static void AssertEitherExtModelAttribute(TypeInfo type)
    {
      /*Assertion.Assert((HasRdExtAttribute(type) || HasRdModelAttribute(type)), $"Invalid RdModel: expected to have either {nameof(RdModelAttribute)} or {nameof(RdExtAttribute)} ({type.ToString(true)}).");*/
      Assertion.Assert(HasRdExtAttribute(type) ^ HasRdModelAttribute(type), $"Invalid RdModel {type.ToString(true)}: expected to have only one of {nameof(RdModelAttribute)} or {nameof(RdExtAttribute)} or be a sealed class assignable from {nameof(IRdBindable)}.");
    }

    public static void AssertRoot(TypeInfo type)
    {
      if (HasRdExtAttribute(type))
      {
        AssertEitherExtModelAttribute(type);
        AssertValidRdExt(type);
        return;
      }

      if (HasRdModelAttribute(type))
      {
        AssertEitherExtModelAttribute(type);
        AssertValidRdModel(type);
        return;
      }

      if (IsScalar(type))
      {
        AssertValidScalar(type);
        return;
      }

      // Generated from DSL models
      if (typeof(RdBindableBase).IsAssignableFrom(type) && HasIntrinsicFields(type))
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
      var isRdModel = HasRdExtAttribute(type);
      Assertion.Assert(isRdModel, $"Error in {type.ToString(true)} model: no {nameof(RdExtAttribute)} attribute specified");
      Assertion.Assert(!type.IsValueType, $"Error in {type.ToString(true)} model: can't be ValueType");
      Assertion.Assert(typeof(RdExtReflectionBindableBase).GetTypeInfo().IsAssignableFrom(type.AsType()), $"Error in {type.ToString(true)} model: should be inherited from {nameof(RdExtReflectionBindableBase)}");

      // actually, it is possible, but error-prone.
      // you may have non-rdmodel base class and several sealed derivatives from it.
      // commented sealed check to avoid annoying colleagues.
      // Assertion.Assert(type.IsSealed, $"Error in {type.ToString(true)} model: RdModels must be sealed.");

      foreach (var member in SerializerReflectionUtil.GetBindableMembers(type))
      {
        AssertMemberDeclaration(member);
      }
    }

    public static void AssertMemberDeclaration(MemberInfo member)
    {
      var isMember = IsMemberDeclaration(member);
      Assertion.Assert(isMember,
        $"Error in {member.DeclaringType?.ToString(true)}: model: member {member.Name} " +
        $"can't be {ReflectionUtil.GetReturnType(member)} type, " +
        "only (RdProperty | RdList | RdSet | RdMap | RdModel | RdCall | custom sealed bindable types) allowed in RdModel or RdExt!");
    }

    public static void AssertValidRdModel([NotNull] TypeInfo type)
    {
      var isDataModel = HasRdModelAttribute(type);
      Assertion.Assert(isDataModel, $"Error in {type.ToString(true)} model: no {nameof(RdModelAttribute)} attribute specified");
      Assertion.Assert(typeof(RdReflectionBindableBase).GetTypeInfo().IsAssignableFrom(type.AsType()), $"Error in {type.ToString(true)} model: should be inherited from {nameof(RdReflectionBindableBase)}");

      // No way to prevent serialization errors for intrinsic serializers, just skip for now
      if (HasIntrinsic(type))
        return;

      foreach (var member in SerializerReflectionUtil.GetBindableMembers(type))
      {
        AssertDataMemberDeclaration(member);
      }
    }

    public static bool HasRdModelAttribute(TypeInfo type)
    {
      var modelAttribute = type.GetCustomAttribute<RdModelAttribute>();
      var isDataModel = modelAttribute != null;
      return isDataModel;
    }

    private static void AssertValidScalar(TypeInfo type)
    {
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

    public static void AssertDataMemberDeclaration(MemberInfo member)
    {
      var isMember = IsModelMemberDeclaration(member);
      Assertion.Assert(isMember,
        $"Error in {member.DeclaringType?.ToString(true)}: data model: member {member.Name} " +
        $"can't be {ReflectionUtil.GetReturnType(member)} type, " +
        $"can be only DataMemberDeclaration, see {nameof(ReflectionSerializerVerifier)} XMLDoc for more details.");
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


    public static bool HasIntrinsic(TypeInfo t)
    {
      return HasIntrinsicNonProtocolMethods(t) || HasIntrinsicProtocolMethods(t) || HasIntrinsicFields(t) || HasRdExtAttribute(t);
    }

    public static bool HasIntrinsicAttribute(TypeInfo t)
    {
      var rdScalar = t.GetCustomAttribute<RdScalarAttribute>();
      return rdScalar != null && rdScalar.Marshaller != null;
    }

    public static bool HasIntrinsicFields(TypeInfo t)
    {
      foreach (var member in t.GetFields(BindingFlags.Static | BindingFlags.Public))
      {
        if (member.Name == "Read" || member.Name == "Write")
        {
          return true;
        }
      }

      return false;

    }

    public static bool HasIntrinsicNonProtocolMethods(TypeInfo t)
    {
      foreach (var member in t.GetMethods(BindingFlags.Static | BindingFlags.Public))
      {
        if ((member.Name == "Read" || member.Name == "Write") &&
            member.GetParameters() is var p && p.Length == 1 && p[0].ParameterType == typeof(UnsafeReader))
        {
          return true;
        }
      }

      return false;
    }


    public static bool HasIntrinsicProtocolMethods(TypeInfo t)
    {
      foreach (var method in t.GetMethods(BindingFlags.Static | BindingFlags.Public))
      {
        if (method.Name == "Read" || method.Name == "Write" && method.GetParameters().Any(p => p.ParameterType == typeof(SerializationCtx)))
        {
          return true;
        }
      }

      return false;
    }

    public static bool IsRpcAttributeDefined(Type @interface)
    {
      return @interface.IsDefined(typeof(RdRpcAttribute), false);
    }
  }
}