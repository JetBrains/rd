using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Util.Util;

namespace JetBrains.Util
{
  public static class ReflectionUtil
  {
    public delegate void SetValueDelegate(object instance, object value);

    /// <summary>
    /// Return setter for either field or property info
    /// </summary>
    [NotNull]
    public static SetValueDelegate GetSetter([NotNull] MemberInfo mi)
    {
      return TryGetSetter(mi) ?? throw new ArgumentOutOfRangeException($"Entity: {mi} is not supported");
    }

    /// <summary>
    /// Return setter for either field or property info, or null if can't be set.
    /// </summary>
    [CanBeNull]
    public static SetValueDelegate TryGetSetter(MemberInfo mi)
    {
      SetValueDelegate GetFieldSetter(FieldInfo backingField)
      {
        // It is possible to mutate readonly fields in current CLI without any warranty.
        // Assertion.Assert(!backingField.IsInitOnly, "Unable to mutate readonly fields");
        return backingField.SetValue;
      }

      switch (mi)
      {
        case PropertyInfo propInfo when propInfo.CanWrite:
          return (instance, val) => propInfo.SetValue(instance, val, null);

        case PropertyInfo _:
          var backingFieldName = $"<{mi.Name}>k__BackingField";
          var backingField = mi.DeclaringType.NotNull().OptionalTypeInfo().GetField(backingFieldName, BindingFlags.NonPublic | BindingFlags.Instance);
          if (backingField != null)
            return GetFieldSetter(backingField);

          break;

        case FieldInfo fieldInfo:
          return (instance, val) => fieldInfo.SetValue(instance, val);
      }

      return null;
    }

    /// <summary>
    /// Return getter for either field or property
    /// </summary>
    [NotNull]
    public static Func<object, object> GetGetter([NotNull] MemberInfo mi)
    {
      switch (mi)
      {
        case PropertyInfo propInfo:
          return instance => propInfo.GetValue(instance, null);
        case FieldInfo fieldInfo:
          return instance => fieldInfo.GetValue(instance);
        default:
          throw new ArgumentOutOfRangeException($"Entity: {mi} is not supported");
      }
    }

    /// <summary>
    /// Get field or property type.
    /// </summary>
    [NotNull]
    public static Type GetReturnType([NotNull] MemberInfo mi)
    {
      switch (mi)
      {
        case PropertyInfo propInfo:
          return propInfo.PropertyType;
        case FieldInfo fieldInfo:
          return fieldInfo.FieldType;
        default:
          throw new ArgumentOutOfRangeException($"Entity: {mi} is not supported");
      }
    }

    [CanBeNull]
    public static object InvokeGenericThis(object self, string methodName, Type argument, [CanBeNull] object[] parameters = null)
    {
      var methodInfo = self.GetType().OptionalTypeInfo().GetMethod(methodName, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic);
      try
      {
        return methodInfo.NotNull().MakeGenericMethod(argument)
          .Invoke(self, parameters ?? EmptyArray<object>.Instance);
      }
      catch (TargetInvocationException e)
      {
        if (e.InnerException != null) throw e.InnerException;

        throw;
      }
    }


    [CanBeNull]
    public static object TryGetNonStaticField(object ownerObject, string memberName)
    {
      try
      {
        var member = ownerObject.GetType().OptionalTypeInfo().GetField(memberName, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic);
        return member != null ? member.GetValue(ownerObject) : null;
      }
      catch (Exception)
      {
        return null;
      }
    }
    
    [CanBeNull]
    public static object TryGetNonStaticProperty(object ownerObject, string memberName)
    {
      try
      {
        var member = ownerObject.GetType().OptionalTypeInfo().GetProperty(memberName, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic);
        return member != null ? member.GetValue(ownerObject, null) : null;
      }
      catch (Exception)
      {
        return null;
      }
    }
    
    
    public static IEnumerable<T> EnumerateEnumValues<T>()
    {
      foreach (var value in Enum.GetValues(typeof (T)))
        yield return (T) value;
    }
    
    
    /// <summary>
    /// Evaluates property value available on object or any of the interfaces it implements
    /// </summary>
    /// <param name="o">Object to invoke property of</param>
    /// <param name="propertyName">Name of the property</param>
    /// <param name="defaultValue">Default value to return if failed</param>
    /// <typeparam name="T">Expected return type</typeparam>
    /// <returns>Evaluated property value or default value</returns>
    public static T GetPropertyValueSafe<T>([NotNull] this object o, string propertyName, T defaultValue = default(T))
    {
      T result = defaultValue;
      try
      {
        var type = o.GetType();
        var propertyInfo = type.OptionalTypeInfo().GetProperty(propertyName, BindingFlags.Instance | BindingFlags.Static | BindingFlags.Public | BindingFlags.NonPublic);
        if (propertyInfo == null)
        {
          foreach (var @interface in type.OptionalTypeInfo().GetInterfaces())
          {
            propertyInfo = @interface.OptionalTypeInfo().GetProperty(propertyName, BindingFlags.Instance | BindingFlags.Static | BindingFlags.Public | BindingFlags.NonPublic);
            if (propertyInfo != null)
              break;
          }
        }

        if (propertyInfo != null)
        {
          var value = propertyInfo.GetValue(o, new object[0]);
          if (value is T)
            result = (T) value;
        }
      }
      catch (Exception)
      {
      }

      return result;
    }
    
    
    
    #if !NET35
    public static T SetStaticInstanceProperty<T>(Lifetime lifetime, Type type)
    {
      const BindingFlags propertiesFlags = BindingFlags.DeclaredOnly | BindingFlags.Public | BindingFlags.Static;
      var members = type
        .OptionalTypeInfo()
        .GetProperties(propertiesFlags)
        .Where(propertyInfo => propertyInfo.PropertyType == type)
        .ToList();
      if (members.Count > 1)
        throw new InvalidOperationException($"{type} has several static public properties declaring instance");
      if (members.Count == 0)
        throw new InvalidOperationException($"{type} does not have static public properties declaring instance");

      const BindingFlags creationFlags = 
        BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.CreateInstance;
      var instance = (T) Activator.CreateInstance(type, creationFlags, null, EmptyArray<object>.Instance, null);

      members[0].SetValue(null, instance);
      lifetime.OnTermination(() => members[0].SetValue(null, null));
      return instance;
    }
    
    #endif
  }
}