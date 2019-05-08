using System;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Text.Impl.Intrinsics;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Text.Impl.Ot.Intrinsics
{
  // auto-generated
  public class RdOtState : RdBindableBase {
    //fields
    //public fields
    [NotNull] public IViewableProperty<OtOperation> Operation { get { return _Operation; }}
    [NotNull] public ISignal<RdAck> Ack { get { return _Ack; }}
    [NotNull] public IViewableProperty<RdAssertion> AssertedMasterText { get { return _AssertedMasterText; }}
    [NotNull] public IViewableProperty<RdAssertion> AssertedSlaveText { get { return _AssertedSlaveText; }}

    //private fields
    [NotNull] private readonly RdProperty<OtOperation> _Operation;
    [NotNull] private readonly RdSignal<RdAck> _Ack;
    [NotNull] private readonly RdProperty<RdAssertion> _AssertedMasterText;
    [NotNull] private readonly RdProperty<RdAssertion> _AssertedSlaveText;

    //primary constructor
    private RdOtState(
      [NotNull] RdProperty<OtOperation> operation,
      [NotNull] RdSignal<RdAck> ack,
      [NotNull] RdProperty<RdAssertion> assertedMasterText,
      [NotNull] RdProperty<RdAssertion> assertedSlaveText
    )
    {
      if (operation == null) throw new ArgumentNullException("operation");
      if (ack == null) throw new ArgumentNullException("ack");
      if (assertedMasterText == null) throw new ArgumentNullException("assertedMasterText");
      if (assertedSlaveText == null) throw new ArgumentNullException("assertedSlaveText");

      _Operation = operation;
      _Ack = ack;
      _AssertedMasterText = assertedMasterText;
      _AssertedSlaveText = assertedSlaveText;
      _Operation.OptimizeNested = true;
      _AssertedMasterText.OptimizeNested = true;
      _AssertedSlaveText.OptimizeNested = true;
      _Operation.ValueCanBeNull = true;
      BindableChildren.Add(new KeyValuePair<string, object>("operation", _Operation));
      BindableChildren.Add(new KeyValuePair<string, object>("ack", _Ack));
      BindableChildren.Add(new KeyValuePair<string, object>("assertedMasterText", _AssertedMasterText));
      BindableChildren.Add(new KeyValuePair<string, object>("assertedSlaveText", _AssertedSlaveText));
    }
    //secondary constructor
    public RdOtState (
    ) : this (
      new RdProperty<OtOperation>(ReadOtOperationNullable, WriteOtOperationNullable),
      new RdSignal<RdAck>(RdAck.Read, RdAck.Write),
      new RdProperty<RdAssertion>(RdAssertion.Read, RdAssertion.Write),
      new RdProperty<RdAssertion>(RdAssertion.Read, RdAssertion.Write)
    ) {}
    //statics

    public static CtxReadDelegate<RdOtState> Read = (ctx, reader) =>
    {
      var _id = RdId.Read(reader);
      var operation = RdProperty<OtOperation>.Read(ctx, reader, ReadOtOperationNullable, WriteOtOperationNullable);
      var ack = RdSignal<RdAck>.Read(ctx, reader, RdAck.Read, RdAck.Write);
      var assertedMasterText = RdProperty<RdAssertion>.Read(ctx, reader, RdAssertion.Read, RdAssertion.Write);
      var assertedSlaveText = RdProperty<RdAssertion>.Read(ctx, reader, RdAssertion.Read, RdAssertion.Write);
      return new RdOtState(operation, ack, assertedMasterText, assertedSlaveText).WithId(_id);
    };
    public static CtxReadDelegate<OtOperation> ReadOtOperationNullable = OtOperationSerializer.ReadDelegate.NullableClass();

    public static CtxWriteDelegate<RdOtState> Write = (ctx, writer, value) =>
    {
      value.RdId.Write(writer);
      RdProperty<OtOperation>.Write(ctx, writer, value._Operation);
      RdSignal<RdAck>.Write(ctx, writer, value._Ack);
      RdProperty<RdAssertion>.Write(ctx, writer, value._AssertedMasterText);
      RdProperty<RdAssertion>.Write(ctx, writer, value._AssertedSlaveText);
    };
    public static CtxWriteDelegate<OtOperation> WriteOtOperationNullable = OtOperationSerializer.WriteDelegate.NullableClass();
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("RdOtState (");
      using (printer.IndentCookie()) {
        printer.Print("operation = "); _Operation.PrintEx(printer); printer.Println();
        printer.Print("ack = "); _Ack.PrintEx(printer); printer.Println();
        printer.Print("assertedMasterText = "); _AssertedMasterText.PrintEx(printer); printer.Println();
        printer.Print("assertedSlaveText = "); _AssertedSlaveText.PrintEx(printer); printer.Println();
      }
      printer.Print(")");
    }
    //toString
    public override string ToString()
    {
      var printer = new SingleLinePrettyPrinter();
      Print(printer);
      return printer.ToString();
    }
  }
}