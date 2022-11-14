#if NET35
using System;
using System.Reflection;

namespace JetBrains.Rd.Reflection;

public static class Net35Extensions
{
  public static Type GetTypeInfo(this Type type)
  {
    return type;
  }

  public static Type AsType(this Type type)
  {
    return type;
  }

  public static T GetCustomAttribute<T>(this MemberInfo mi) where T : Attribute
  {
    return (T) Attribute.GetCustomAttribute(mi, typeof(T));
  }
}
#endif