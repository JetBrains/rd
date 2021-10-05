using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using System.Reflection;
using JetBrains.Annotations;

namespace JetBrains.Util.Util
{
  /// <summary>
  /// Extensions to <see cref="Type"/>
  /// </summary>
  public static class Types
  {
    /// <summary>
    /// To prettify Int32, Int64 and so on.
    /// </summary>
    private static readonly Dictionary<Type, string> ourPrimitives = new Dictionary<Type, string>
    {
      {typeof(byte), "byte"},
      {typeof(short), "short"},
      {typeof(int), "int"},
      {typeof(long), "long"},
      {typeof(sbyte), "sbyte"},
      {typeof(ushort), "ushort"},
      {typeof(uint), "uint"},
      {typeof(ulong), "ulong"},
      {typeof(float), "float"},
      {typeof(double), "double"},
      {typeof(decimal), "decimal"},
      {typeof(bool), "bool"},
      {typeof(char), "char"},
      {typeof(void), "void"}
    };

    /// <summary>
    /// Different <c>ToString()</c> of <see cref="Type"/>.
    /// 
    /// Samples are provide for:  
    /// <code>
    /// namespace Foo { 
    ///   class Outer&lt;T1&gt;
    ///   {
    ///      internal class Inner&lt;T2&gt; {}
    ///   }    
    /// }
    /// 
    /// </code>     
    /// </summary>
    /// <param name="thisType"><c>typeof(Outer&lt;string&gt;.Inner&lt;int&gt;)</c></param>
    /// <param name="withNamespaces">if `true` (and <see cref="withGenericArguments"/> == `false`) then <c>Foo.Outer+Inner</c> else <c>Outer+Inner</c> </param>
    /// <param name="withGenericArguments">if `true` (and <see cref="withNamespaces"/> == `false`) then <c>Outer&lt;string&gt;+Inner&lt;int&gt;</c> else <c>Outer+Inner</c> </param>
    /// <returns>More natural than <see cref="Type.ToString"/> string representation of type</returns>
    [ContractAnnotation("thisType:null=>null;=>notnull")]
    public static string ToString(this Type thisType, bool withNamespaces, bool withGenericArguments = true)
    {
      [return: NotNullIfNotNull("type")]
      string? Present(Type? type, Type[]? genericArguments = null)
      {
        if (type == null)
          return null;

        genericArguments = genericArguments ?? type.GetGenericArguments();

        if (type.IsArray)
          return Present(type.GetElementType()) + "[" + new string(',', type.GetArrayRank() - 1) + "]";

        if (type.IsGenericType && type.GetGenericTypeDefinition() == typeof(Nullable<>))
          return Present(type.GetGenericArguments().Single()) + "?";

        if (type.IsPrimitive)
          return ourPrimitives.TryGetValue(type, out var primitive) ? primitive : type.Name;

        if (type == typeof(string))
          return "string";

        if (type.IsGenericParameter)
          return type.Name;

        var trait = type.Name;
        var idx = trait.IndexOf('`');
        trait = idx >= 0 ? trait.Substring(0, idx) : trait;


        var allGaCount = genericArguments.Length;
        var outerGaCount = type.DeclaringType?.GetGenericArguments().Length ?? 0;

        if (withGenericArguments && allGaCount > 0)
        {
          var ownArguments = genericArguments.Skip(outerGaCount).Select(t => Present(t));

          trait = trait + "<" + string.Join(", ", ownArguments.ToArray()) + ">";
        }


        var outer = type.DeclaringType;
        return outer == null
          ? (withNamespaces && !string.IsNullOrEmpty(type.Namespace) ? type.Namespace + "." + trait : trait)
          : Present(outer, genericArguments.Take(outerGaCount).ToArray()) + "+" + trait;
      }

      return Present(thisType);
    }

    public static
      #if NET35
        Type
      #else
        System.Reflection.TypeInfo
      #endif
      OptionalTypeInfo(this Type thisType)
    {
      return thisType
#if !NET35
      .GetTypeInfo()
#endif
        ;
    } 
  }
}