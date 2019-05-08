using System;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Text.Intrinsics;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Text.Impl.Intrinsics
{
  public class RdTextBufferState : RdBindableBase
  {
    //fields
    //public fields
    [NotNull] public IViewableProperty<RdTextBufferChange> Changes { get { return _Changes; }}
    [NotNull] public IViewableProperty<TextBufferVersion> VersionBeforeTypingSession { get { return _VersionBeforeTypingSession; }}
    [NotNull] public IViewableProperty<RdAssertion> AssertedMasterText { get { return _AssertedMasterText; }}
    [NotNull] public IViewableProperty<RdAssertion> AssertedSlaveText { get { return _AssertedSlaveText; }}

    //private fields
    [NotNull] private readonly RdProperty<RdTextBufferChange> _Changes;
    [NotNull] private readonly RdProperty<TextBufferVersion> _VersionBeforeTypingSession;
    [NotNull] private readonly RdProperty<RdAssertion> _AssertedMasterText;
    [NotNull] private readonly RdProperty<RdAssertion> _AssertedSlaveText;

    //primary constructor
    private RdTextBufferState(
      [NotNull] RdProperty<RdTextBufferChange> changes,
      [NotNull] RdProperty<TextBufferVersion> versionBeforeTypingSession,
      [NotNull] RdProperty<RdAssertion> assertedMasterText,
      [NotNull] RdProperty<RdAssertion> assertedSlaveText
    )
    {
      if (changes == null) throw new ArgumentNullException("changes");
      if (versionBeforeTypingSession == null) throw new ArgumentNullException("versionBeforeTypingSession");
      if (assertedMasterText == null) throw new ArgumentNullException("assertedMasterText");
      if (assertedSlaveText == null) throw new ArgumentNullException("assertedSlaveText");

      _Changes = changes;
      _VersionBeforeTypingSession = versionBeforeTypingSession;
      _AssertedMasterText = assertedMasterText;
      _AssertedSlaveText = assertedSlaveText;
      _Changes.OptimizeNested = true;
      _VersionBeforeTypingSession.OptimizeNested = true;
      _AssertedMasterText.OptimizeNested = true;
      _AssertedSlaveText.OptimizeNested = true;
      _Changes.ValueCanBeNull = true;
      BindableChildren.Add(new KeyValuePair<string, object>("changes", _Changes));
      BindableChildren.Add(new KeyValuePair<string, object>("versionBeforeTypingSession", _VersionBeforeTypingSession));
      BindableChildren.Add(new KeyValuePair<string, object>("assertedMasterText", _AssertedMasterText));
      BindableChildren.Add(new KeyValuePair<string, object>("assertedSlaveText", _AssertedSlaveText));
    }
    //secondary constructor
    public RdTextBufferState (
    ) : this (
      new RdProperty<RdTextBufferChange>(ReadRdTextBufferChangeNullable, WriteRdTextBufferChangeNullable),
      new RdProperty<TextBufferVersion>(TextBufferVersionSerializer.ReadDelegate, TextBufferVersionSerializer.WriteDelegate),
      new RdProperty<RdAssertion>(RdAssertion.Read, RdAssertion.Write),
      new RdProperty<RdAssertion>(RdAssertion.Read, RdAssertion.Write)
    ) {}
    //statics

    public static CtxReadDelegate<RdTextBufferState> Read = (ctx, reader) =>
    {
      var _id = RdId.Read(reader);
      var changes = RdProperty<RdTextBufferChange>.Read(ctx, reader, ReadRdTextBufferChangeNullable, WriteRdTextBufferChangeNullable);
      var versionBeforeTypingSession = RdProperty<TextBufferVersion>.Read(ctx, reader, TextBufferVersionSerializer.ReadDelegate, TextBufferVersionSerializer.WriteDelegate);
      var assertedMasterText = RdProperty<RdAssertion>.Read(ctx, reader, RdAssertion.Read, RdAssertion.Write);
      var assertedSlaveText = RdProperty<RdAssertion>.Read(ctx, reader, RdAssertion.Read, RdAssertion.Write);
      return new RdTextBufferState(changes, versionBeforeTypingSession, assertedMasterText, assertedSlaveText).WithId(_id);
    };
    public static CtxReadDelegate<RdTextBufferChange> ReadRdTextBufferChangeNullable = RdTextBufferChange.ReadDelegate.NullableClass();

    public static CtxWriteDelegate<RdTextBufferState> Write = (ctx, writer, value) =>
    {
      value.RdId.Write(writer);
      RdProperty<RdTextBufferChange>.Write(ctx, writer, value._Changes);
      RdProperty<TextBufferVersion>.Write(ctx, writer, value._VersionBeforeTypingSession);
      RdProperty<RdAssertion>.Write(ctx, writer, value._AssertedMasterText);
      RdProperty<RdAssertion>.Write(ctx, writer, value._AssertedSlaveText);
    };
    public static CtxWriteDelegate<RdTextBufferChange> WriteRdTextBufferChangeNullable = RdTextBufferChange.WriteDelegate.NullableClass();
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("RdTextBufferState (");
      using (printer.IndentCookie()) {
        printer.Print("changes = "); _Changes.PrintEx(printer); printer.Println();
        printer.Print("versionBeforeTypingSession = "); _VersionBeforeTypingSession.PrintEx(printer); printer.Println();
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