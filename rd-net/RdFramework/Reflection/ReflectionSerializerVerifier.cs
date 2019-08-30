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

     Member ::= RdProperty| RdList| RdSet| RdMap | RdModel | RdCall
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
      typeof(byte), typeof(short), typeof(int), typeof(long), typeof(float), typeof(double), typeof(char), typeof(bool), typeof(Unit), typeof(string), typeof(Guid), typeof(DateTime), typeof(Uri), typeof(RdId), typeof(RdSecureString), typeof(byte[]), typeof(short[]), typeof(int[]), typeof(long[]), typeof(float[]), typeof(double[]), typeof(char[]), typeof(bool[])
    };

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

    public static bool CanBeNull(MemberInfo memberInfo)
    {
      var returnType = ReflectionUtil.GetReturnType(memberInfo);

      var returnTypeInfo = returnType.GetTypeInfo();
      if (IsNullable(returnTypeInfo, _ => true))
        return true;

      if (returnTypeInfo.IsValueType)
        return false;

      foreach (var attribute in memberInfo.GetCustomAttributes(false))
        if (attribute is CanBeNullAttribute)
          return true;

      // NotNull by default for any reference type
      return false;
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
               genericDefinition == typeof(RdMap<,>) ||
               (genericDefinition == typeof(InprocRpc<,>) && IsScalar(arguments[0]) && IsScalar(arguments[1])) ||
               IsFromRdProperty(typeInfo); // hack to support UProperty in RdExt

        bool IsFromRdProperty(TypeInfo tInfo)
        {
          var genericArguments = tInfo.GetGenericArguments();
          if (genericArguments.Length != 1)
            return false;
          var rdProperty = typeof(IViewableProperty<>).MakeGenericType(typeInfo.GetGenericArguments());
          return rdProperty.GetTypeInfo().IsAssignableFrom(implementingType);
        }
      }

      var hasRdExt = typeInfo.GetCustomAttribute<RdExtAttribute>() != null;
      if (hasRdExt)
        return true;

      return false;
    }

    private static bool IsScalar(Type type)
    {
      return !typeof(IRdBindable).IsAssignableFrom(type);
    }

    public static void AssertEitherExtModelAttribute(TypeInfo type)
    {
      /*Assertion.Assert((HasRdExtAttribute(type) || HasRdModelAttribute(type)), $"Invalid RdModel: expected to have either {nameof(RdModelAttribute)} or {nameof(RdExtAttribute)} ({type.ToString(true)}).");*/
      Assertion.Assert((HasRdExtAttribute(type) ^ HasRdModelAttribute(type)), $"Invalid RdModel: expected to have only one of {nameof(RdModelAttribute)} or {nameof(RdExtAttribute)}.");
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

      if (IsValueTuple(type))
      {
        AssertValidTuple(type);
        return;
      }

      throw new InvalidOperationException($"Invalid rd type, can be only RdExt, RdModel or ValueTuple {type.ToString(true)}");
    }

    public static bool IsValueTuple(TypeInfo type)
    {
      return type.IsGenericType && type.FullName.NotNull().StartsWith("System.ValueTuple`");
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
      Assertion.Assert(typeof(RdReflectionBindableBase).GetTypeInfo().IsAssignableFrom(type.AsType()), $"Error in {type.ToString(true)} model: should be inherited from {nameof(RdReflectionBindableBase)}");

      // actually, it is possible, but error-prone.
      // you may have non-rdmodel base class and several sealed derivatives from it.
      // commented sealed check to avoid annoying colleagues.
      // Assertion.Assert(type.IsSealed, $"Error in {type.ToString(true)} model: RdModels must be sealed.");

      foreach (var member in ReflectionSerializers.GetBindableMembers(type))
      {
        if (member is PropertyInfo || member is FieldInfo)
        {
          AssertMemberDeclaration(member);
        }

        if (member is TypeInfo)
        {
          // out scope for current validation
        }
        // methods and events are allowed in model
      }
    }

    public static void AssertMemberDeclaration(MemberInfo member)
    {
      var isMember = IsMemberDeclaration(member);
      Assertion.Assert(isMember,
        $"Error in {member.DeclaringType?.ToString(true)}: model: member {member.Name} " +
        $"can't be {ReflectionUtil.GetReturnType(member)} type, " +
        "only RdProperty | RdList | RdSet | RdMap | RdModel | RdCall allowed in RdModel or RdExt!");
    }

    public static void AssertValidRdModel([NotNull] TypeInfo type)
    {
      var isDataModel = HasRdModelAttribute(type);
      Assertion.Assert(isDataModel, $"Error in {type.ToString(true)} model: no {nameof(RdModelAttribute)} attribute specified");

      Assertion.Assert(!type.IsValueType, $"Error in {type.ToString(true)} model: data model can't be ValueType");

      // No way to prevent serialization errors for intrinsic serializers, just skip for now
      if (HasIntrinsicMethods(type))
        return;

      foreach (var member in ReflectionSerializers.GetBindableMembers(type))
      {
        if (member is PropertyInfo || member is FieldInfo)
        {
          AssertDataMemberDeclaration(member);
        }

        if (member is TypeInfo)
        {
          // out scope for current validation
        }
        // methods events are allowed in model
      }
    }

    public static bool HasRdModelAttribute(TypeInfo type)
    {
      var modelAttribute = type.GetCustomAttribute<RdModelAttribute>();
      var isDataModel = modelAttribute != null;
      return isDataModel;
    }

    private static void AssertValidTuple(TypeInfo type)
    {
      var genericArguments = type.GetGenericArguments();
      Assertion.Assert(genericArguments.Length <= 7, "Value tuples can only have no more than 7 arguments: {0}", type.ToString(true));
      foreach (var tupleArgument in genericArguments)
      {
        var argumentTypeInfo = tupleArgument.GetTypeInfo();
        Assertion.Assert(
          IsFieldType(argumentTypeInfo, true) || HasRdModelAttribute(argumentTypeInfo),
          $"Invalid value tuple model: {type.ToString(true)}, argument {tupleArgument.ToString(true)} is not valid field type. Check requirements in {nameof(ReflectionSerializerVerifier)}.{nameof(IsFieldType)}");
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

      if (genericDefinition == typeof(IRdCall<,>)) return typeof(InprocRpc<,>).MakeGenericType(typeInfo.GetGenericArguments());

      return typeInfo.AsType();
    }

    public static bool HasIntrinsicMethods(TypeInfo t)
    {
      if (t.GetMethods(BindingFlags.Public | BindingFlags.Static)
        .Any(m => m.Name == "Read" || m.Name == "Write"))
        return true;
      return false;
    }
  }
}