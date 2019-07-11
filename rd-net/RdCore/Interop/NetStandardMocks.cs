#if NETSTANDARD
using System.Reflection;

namespace System {
#if !NETSTANDARD2_0
  public class Serializable : Attribute {}
#endif
  public static class Extensions
  {
    public static bool IsSubclassOf(this Type t, Type t2)
    {
      return t.GetTypeInfo().IsSubclassOf(t2);
    }
  }
}
  
namespace System.Net.Sockets
{
  public static class SocketsEx
  {
    public static void Close(this Socket s)
    {
      s.Dispose();
    }
  }
}

namespace System.Runtime.Serialization
{
  public class SerializationInfo
  {
  }
}
namespace System.ComponentModel
{
  [AttributeUsage(AttributeTargets.All, Inherited = false, AllowMultiple = true)]
  public sealed class LocalizableAttribute : Attribute
  {
    public LocalizableAttribute(bool value)
    {
    }
  }
  
  [AttributeUsage(AttributeTargets.All, AllowMultiple = true, Inherited = true)]
  public sealed class EditorAttribute : Attribute
  {
    private string baseTypeName;
    private string typeName;
    private string typeId;

    public string EditorBaseTypeName
    {
      get
      {
        return this.baseTypeName;
      }
    }

    public string EditorTypeName
    {
      get
      {
        return this.typeName;
      }
    }

    public /*override*/ object TypeId
    {
      get
      {
        if (this.typeId == null)
        {
          string str = this.baseTypeName;
          int length = str.IndexOf(',');
          if (length != -1)
            str = str.Substring(0, length);
          this.typeId = this.GetType().FullName + str;
        }
        return (object) this.typeId;
      }
    }

    public EditorAttribute()
    {
      this.typeName = string.Empty;
      this.baseTypeName = string.Empty;
    }

    public EditorAttribute(string typeName, string baseTypeName)
    {
      typeName.ToUpper(/*CultureInfo.InvariantCulture*/);
      this.typeName = typeName;
      this.baseTypeName = baseTypeName;
    }

    public EditorAttribute(string typeName, Type baseType)
    {
      typeName.ToUpper(/*CultureInfo.InvariantCulture*/);
      this.typeName = typeName;
      this.baseTypeName = baseType.AssemblyQualifiedName;
    }

    public EditorAttribute(Type type, Type baseType)
    {
      this.typeName = type.AssemblyQualifiedName;
      this.baseTypeName = baseType.AssemblyQualifiedName;
    }

    public override bool Equals(object obj)
    {
      if (obj == this)
        return true;
      EditorAttribute editorAttribute = obj as EditorAttribute;
      if (editorAttribute != null && editorAttribute.typeName == this.typeName)
        return editorAttribute.baseTypeName == this.baseTypeName;
      return false;
    }

    public override int GetHashCode()
    {
      return base.GetHashCode();
    }
  }
}
#endif
