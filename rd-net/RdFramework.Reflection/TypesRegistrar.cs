using System;
using System.Diagnostics;
using JetBrains.Util;

namespace JetBrains.Rd.Reflection
{
  public class TypesRegistrar : ITypesRegistrar
  {
    private readonly ITypesCatalog myCatalog;
    private readonly ReflectionSerializers myReflectionSerializers;

    public TypesRegistrar(ITypesCatalog catalog, ReflectionSerializers reflectionSerializers)
    {
      myCatalog = catalog ?? throw new ArgumentNullException(nameof(catalog));
      myReflectionSerializers = reflectionSerializers ?? throw new ArgumentNullException(nameof(reflectionSerializers));
    }

    public void TryRegister(RdId id, ISerializers serializers)
    {
      var clrType = myCatalog.GetById(id);
      if (clrType != null)
      {
        Register(clrType, serializers);
      }
    }

    public void TryRegister(Type clrType, ISerializers serializers)
    {
      Register(clrType, serializers);
    }

    private void Register(Type type, ISerializersContainer serializers)
    {
      var instanceSerializer = type.IsInterface || type.IsAbstract;
      var serializerPair = myReflectionSerializers.GetOrCreateMemberSerializer(type, false, instanceSerializer, null);
      ReflectionUtil.InvokeGenericThis(serializers, nameof(serializers.Register), type,
        new[] {serializerPair.Reader, serializerPair.Writer, RdId.DefineByFqn(type).Value });
    }
  }
}