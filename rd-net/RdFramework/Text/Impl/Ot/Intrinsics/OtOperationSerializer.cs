using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Text.Impl.Intrinsics;

namespace JetBrains.Rd.Text.Impl.Ot.Intrinsics
{
  public static class OtOperationSerializer
  {
    private const byte RetainCode = 1;
    private const byte InsertCode = 2;
    private const byte DeleteCode = 3;

    private static readonly CtxReadDelegate<OtChange> ourReadOtChangeDelegate = (ctx, reader) =>
    {
      var id = reader.ReadByte();
      switch (id)
      {
        case RetainCode:
        {
          var offset = reader.ReadInt();
          return new Retain(offset);
        }
        case InsertCode:
        {
          var text = reader.ReadString();
          return new InsertText(text);
        }
        case DeleteCode:
        {
          var text = reader.ReadString();
          return new DeleteText(text);
        }
        default: throw new ArgumentOutOfRangeException(string.Format("Can't find reader by id: {0}", id));
      }
    };

    private static readonly CtxWriteDelegate<OtChange> ourWriteOtChangeDelegate = (ctx, writer, value) =>
    {
      if (value is Retain)
      {
        writer.Write(RetainCode);
        writer.Write(((Retain) value).Offset);
      }
      else if (value is InsertText)
      {
        writer.Write(InsertCode);
        writer.Write(((InsertText) value).Text);
      }
      else if (value is DeleteText)
      {
        writer.Write(DeleteCode);
        writer.Write(((DeleteText) value).Text);
      }
      else
      {
        throw new ArgumentOutOfRangeException(string.Format("Can't find writer for type: {0}", value.GetType().Name));
      }
    };


    public static CtxReadDelegate<OtOperation> ReadDelegate = (ctx, reader) =>
    {
      var changes = reader.ReadList(ourReadOtChangeDelegate, ctx);
      var origin = Serializers.ReadEnum<RdChangeOrigin>(ctx, reader);
      var remoteTs = reader.ReadInt();
      var kind = Serializers.ReadEnum<OtOperationKind>(ctx, reader);
      return new OtOperation(changes, origin, remoteTs, kind);
    };

    public static CtxWriteDelegate<OtOperation> WriteDelegate = (ctx, writer, value) =>
    {
      var changes = value.Changes as List<OtChange> ?? value.Changes.ToList(); // todo move AsList() into Lifetimes
      writer.WriteList(ourWriteOtChangeDelegate, ctx, changes);
      Serializers.WriteEnum(ctx, writer, value.Origin);
      Serializers.WriteInt(ctx, writer, value.Timestamp);
      Serializers.WriteEnum(ctx, writer, value.Kind);
    };
  }
}