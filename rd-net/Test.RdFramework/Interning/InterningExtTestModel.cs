using System;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Util;

// ReSharper disable RedundantEmptyObjectCreationArgumentList
// ReSharper disable InconsistentNaming
// ReSharper disable RedundantOverflowCheckingContext


namespace Test.RdFramework.Interning
{
  
  
  public class InterningExt : DefaultExtBase
  {
    //fields
    //public fields
    [NotNull] public IViewableProperty<InterningExtRootModel> Root { get { return _Root; }}
    
    //private fields
    [NotNull] private readonly RdProperty<InterningExtRootModel> _Root;
    
    //primary constructor
    private InterningExt(
      [NotNull] RdProperty<InterningExtRootModel> root
    )
    {
      if (root == null) throw new ArgumentNullException("root");
      
      _Root = root;
      BindableChildren.Add(new KeyValuePair<string, object>("root", _Root));
    }
    //secondary constructor
    internal InterningExt (
    ) : this (
      new RdProperty<InterningExtRootModel>(InterningExtRootModel.Read, InterningExtRootModel.Write)
    ) {}
    //statics
    
    
    
    protected override long SerializationHash => -2181600832385335602L;
    
    protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;
    public static void RegisterDeclaredTypesSerializers(ISerializers serializers)
    {
      serializers.Register(InterningExtRootModel.Read, InterningExtRootModel.Write);
      
      serializers.RegisterToplevelOnce(typeof(InterningRoot1), InterningRoot1.RegisterDeclaredTypesSerializers);
    }
    
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("InterningExt (");
      using (printer.IndentCookie()) {
        printer.Print("root = "); _Root.PrintEx(printer); printer.Println();
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
  public static class InterningExtensionHolderInterningExtEx
   {
    public static InterningExt GetInterningExt(this InterningExtensionHolder interningExtensionHolder)
    {
      return interningExtensionHolder.GetOrCreateExtension("interningExt", () => new InterningExt());
    }
  }
  
  
  public class InterningExtRootModel : RdBindableBase
  {
    //fields
    //public fields
    [NotNull] public IViewableProperty<string> InternedLocally { get { return _InternedLocally; }}
    [NotNull] public IViewableProperty<string> InternedExternally { get { return _InternedExternally; }}
    [NotNull] public IViewableProperty<string> InternedInProtocol { get { return _InternedInProtocol; }}
    
    //private fields
    [NotNull] private readonly RdProperty<string> _InternedLocally;
    [NotNull] private readonly RdProperty<string> _InternedExternally;
    [NotNull] private readonly RdProperty<string> _InternedInProtocol;
    
    private SerializationCtx mySerializationContext;
    public override bool TryGetSerializationContext(out SerializationCtx ctx) { ctx = mySerializationContext; return true; }
    //primary constructor
    private InterningExtRootModel(
      [NotNull] RdProperty<string> internedLocally,
      [NotNull] RdProperty<string> internedExternally,
      [NotNull] RdProperty<string> internedInProtocol
    )
    {
      if (internedLocally == null) throw new ArgumentNullException("internedLocally");
      if (internedExternally == null) throw new ArgumentNullException("internedExternally");
      if (internedInProtocol == null) throw new ArgumentNullException("internedInProtocol");
      
      _InternedLocally = internedLocally;
      _InternedExternally = internedExternally;
      _InternedInProtocol = internedInProtocol;
      _InternedLocally.OptimizeNested = true;
      _InternedExternally.OptimizeNested = true;
      _InternedInProtocol.OptimizeNested = true;
      BindableChildren.Add(new KeyValuePair<string, object>("internedLocally", _InternedLocally));
      BindableChildren.Add(new KeyValuePair<string, object>("internedExternally", _InternedExternally));
      BindableChildren.Add(new KeyValuePair<string, object>("internedInProtocol", _InternedInProtocol));
    }
    //secondary constructor
    public InterningExtRootModel (
    ) : this (
      new RdProperty<string>(ReadStringInternedAtInExt, WriteStringInternedAtInExt),
      new RdProperty<string>(ReadStringInternedAtOutOfExt, WriteStringInternedAtOutOfExt),
      new RdProperty<string>(ReadStringInternedAtProtocol, WriteStringInternedAtProtocol)
    ) {}
    //statics
    
    public static CtxReadDelegate<InterningExtRootModel> Read = (ctx, reader) => 
    {
      var _id = RdId.Read(reader);
      var internedLocally = RdProperty<string>.Read(ctx, reader, ReadStringInternedAtInExt, WriteStringInternedAtInExt);
      var internedExternally = RdProperty<string>.Read(ctx, reader, ReadStringInternedAtOutOfExt, WriteStringInternedAtOutOfExt);
      var internedInProtocol = RdProperty<string>.Read(ctx, reader, ReadStringInternedAtProtocol, WriteStringInternedAtProtocol);
      var _result = new InterningExtRootModel(internedLocally, internedExternally, internedInProtocol).WithId(_id);
      _result.mySerializationContext = ctx.WithInternRootsHere(_result, "InExt");
      return _result;
    };
    public static CtxReadDelegate<string> ReadStringInternedAtInExt = Serializers.ReadString.Interned("InExt");
    public static CtxReadDelegate<string> ReadStringInternedAtOutOfExt = Serializers.ReadString.Interned("OutOfExt");
    public static CtxReadDelegate<string> ReadStringInternedAtProtocol = Serializers.ReadString.Interned("Protocol");
    
    public static CtxWriteDelegate<InterningExtRootModel> Write = (ctx, writer, value) => 
    {
      value.RdId.Write(writer);
      RdProperty<string>.Write(ctx, writer, value._InternedLocally);
      RdProperty<string>.Write(ctx, writer, value._InternedExternally);
      RdProperty<string>.Write(ctx, writer, value._InternedInProtocol);
      value.mySerializationContext = ctx.WithInternRootsHere(value, "InExt");
    };
    public static CtxWriteDelegate<string> WriteStringInternedAtInExt = Serializers.WriteString.Interned("InExt");
    public static CtxWriteDelegate<string> WriteStringInternedAtOutOfExt = Serializers.WriteString.Interned("OutOfExt");
    public static CtxWriteDelegate<string> WriteStringInternedAtProtocol = Serializers.WriteString.Interned("Protocol");
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("InterningExtRootModel (");
      using (printer.IndentCookie()) {
        printer.Print("internedLocally = "); _InternedLocally.PrintEx(printer); printer.Println();
        printer.Print("internedExternally = "); _InternedExternally.PrintEx(printer); printer.Println();
        printer.Print("internedInProtocol = "); _InternedInProtocol.PrintEx(printer); printer.Println();
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
