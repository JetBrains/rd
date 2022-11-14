using System;
using JetBrains.Annotations;

namespace JetBrains.Rd.Reflection
{
  [AttributeUsage(AttributeTargets.Class, Inherited = false), MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  [BaseTypeRequired(typeof(RdExtReflectionBindableBase))]
  public class RdExtAttribute : Attribute
  {
    public Type? RdRpcInterface { get; }

    public RdExtAttribute() { }

    /// <summary>
    /// Mark RdExt as implementing contract from specific RdRpc interface. That means that this RdExt will be exposed by
    /// interface name, not by the type itself. It may be used when explicit marking of RdRpc is undesirable.
    /// </summary>
    /// <param name="rdRpcInterface">
    ///   RdRpc interface type. Must be implemented by type, which marked by this RdExt attribute.
    /// </param>
    public RdExtAttribute(Type rdRpcInterface)
    {
      RdRpcInterface = rdRpcInterface;
    }
  }

  /// <summary>
  /// Mark implementing interface of RdExt by this attribute to indicate intent to use this interface for proxy generation
  /// </summary>
  [AttributeUsage(AttributeTargets.Interface), MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  public class RdRpcAttribute : Attribute { }

  [MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  [AttributeUsage(AttributeTargets.Class | AttributeTargets.Enum, Inherited = false)]
  [BaseTypeRequired(typeof(RdReflectionBindableBase))]
  public class RdModelAttribute : Attribute { }

  /// <summary>
  /// It has no special semantic. Used only to tell ReSharper about ImplicitUse.
  /// </summary>
  [MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  [AttributeUsage(AttributeTargets.Class | AttributeTargets.Enum | AttributeTargets.Struct | AttributeTargets.Interface, Inherited = false)]
  public class RdScalarAttribute : Attribute
  {
    public Type? Marshaller { get; }

    public RdScalarAttribute()
    {
    }

    /// <summary>
    /// Provides external marshaller for this type
    /// </summary>
    /// <param name="marshaller">
    /// A type which implements <see cref="IIntrinsicMarshaller{T}"/> for this type or any base interface.
    /// Keep in mind that if you provide an serializer for base interface a runtime casting error is possible on the
    /// receiver side if receiver want to have an inheritor from this interface
    /// </param>
    public RdScalarAttribute(Type marshaller)
    {
      Marshaller = marshaller;
    }
  }

  [Obsolete("RdAsync enabled by default for everything")]
  [AttributeUsage(AttributeTargets.Field | AttributeTargets.Property | AttributeTargets.Method)]
  public class RdAsyncAttribute : Attribute { }

  /// <summary>
  /// Marker interface for proxy types.
  /// Used to distinguish between proxy-implemented methods, for which we should only initialize RdCall fields and other reactive properties
  /// and real methods in types, for which we should Bind appropriate RdEndpoint.
  /// </summary>
  public interface IProxyTypeMarker
  {
  }
}