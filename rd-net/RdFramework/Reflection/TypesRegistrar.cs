using System;
using JetBrains.Annotations;
using JetBrains.Util;

namespace JetBrains.Rd.Reflection
{
  public class TypesRegistrar : ITypesRegistrar
  {
    private readonly ITypesCatalog myCatalog;
    private readonly ReflectionSerializersFactory myReflectionSerializersFactory;

    public TypesRegistrar([NotNull] ITypesCatalog catalog, [NotNull] ReflectionSerializersFactory reflectionSerializersFactory)
    {
      myCatalog = catalog ?? throw new ArgumentNullException(nameof(catalog));
      myReflectionSerializersFactory = reflectionSerializersFactory ?? throw new ArgumentNullException(nameof(reflectionSerializersFactory));
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

    internal void Register(Type type, ISerializers serializers)
    {
      var serializerPair = myReflectionSerializersFactory.GetOrRegisterSerializerPair(type);
      ReflectionUtil.InvokeGenericThis(serializers, nameof(serializers.Register), type,
        new[] {serializerPair.Reader, serializerPair.Writer, null});
    }
  }
}