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

    private void Register(Type type, ISerializersContainer serializers)
    {
      var instanceSerializer = type.IsInterface || type.IsAbstract;
      var serializerPair = myReflectionSerializersFactory.GetOrRegisterSerializerPair(type, instanceSerializer);
      ReflectionUtil.InvokeGenericThis(serializers, nameof(serializers.Register), type,
        new[] {serializerPair.Reader, serializerPair.Writer, RdId.DefineByFqn(type).Value });
    }
  }
}