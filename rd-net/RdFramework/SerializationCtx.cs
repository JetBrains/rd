using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd
{
  public struct SerializationCtx
  {
    public ISerializers Serializers { get; private set; }

    [NotNull] public readonly IDictionary<string, IInternRoot> InternRoots;

    public SerializationCtx(ISerializers serializers, IDictionary<string, IInternRoot> internRoots = null) : this()
    {
      Serializers = serializers;
      InternRoots = internRoots ?? new Dictionary<string, IInternRoot>();
    }

    public SerializationCtx(IProtocol protocol, IDictionary<string, IInternRoot> internRoots = null) : this(protocol.Serializers, internRoots)
    {
    }

    public SerializationCtx WithInternRootsHere(RdBindableBase owner, params string[] keys)
    {
      var newInternRoots = new Dictionary<string, IInternRoot>(InternRoots);
      foreach (var key in keys)
      {
        var root = owner.GetOrCreateHighPriorityExtension("InternRoot-" + key, () => new InternRoot { RdId = owner.RdId.Mix(".InternRoot-" + key) });
        newInternRoots[key] = root;
      }
      return new SerializationCtx(Serializers, newInternRoots);
    }
    
    public T ReadInterned<T>(UnsafeReader stream, string internKey, CtxReadDelegate<T> readValueDelegate)
    {
      if (!InternRoots.TryGetValue(internKey, out var interningRoot))
        return readValueDelegate(this, stream);
       
      return interningRoot.UnIntern<T>(stream.ReadInt() ^ 1);
    }

    public void WriteInterned<T>(UnsafeWriter stream, T value, string internKey, CtxWriteDelegate<T> writeValueDelegate)
    {
      if (!InternRoots.TryGetValue(internKey, out var interningRoot))
      {
        writeValueDelegate(this, stream, value);
        return;
      }

      stream.Write(interningRoot.Intern(value));
    }
  }
}