using System;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Util;
using Lifetime = JetBrains.Lifetimes.Lifetime;

// ReSharper disable RedundantEmptyObjectCreationArgumentList
// ReSharper disable InconsistentNaming
// ReSharper disable RedundantOverflowCheckingContext

namespace Test.RdFramework.Interning
{
  
  
  public class InterningRoot1 : RdExtBase
  {
    //fields
    //public fields
    
    //private fields
    //primary constructor
    private InterningRoot1(
    )
    {
    }
    //secondary constructor
    //statics
    
    
    
    protected override long SerializationHash => 2016272947314984652L;
    
    protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;
    public static void RegisterDeclaredTypesSerializers(ISerializers serializers)
    {
      serializers.Register(InterningTestModel.Read, InterningTestModel.Write);
      serializers.Register(InterningNestedTestModel.Read, InterningNestedTestModel.Write);
      serializers.Register(InterningNestedTestStringModel.Read, InterningNestedTestStringModel.Write);
      serializers.Register(InterningProtocolLevelModel.Read, InterningProtocolLevelModel.Write);
      serializers.Register(InterningMtModel.Read, InterningMtModel.Write);
      serializers.Register(InterningExtensionHolder.Read, InterningExtensionHolder.Write);
      serializers.Register(WrappedStringModel.Read, WrappedStringModel.Write);
      serializers.Register(ProtocolWrappedStringModel.Read, ProtocolWrappedStringModel.Write);
      
      serializers.RegisterToplevelOnce(typeof(InterningRoot1), InterningRoot1.RegisterDeclaredTypesSerializers);
      serializers.RegisterToplevelOnce(typeof(InterningExt), InterningExt.RegisterDeclaredTypesSerializers);
    }
    
    public InterningRoot1(Lifetime lifetime, IProtocol protocol) : this()
    {
      Identify(protocol.Identities, protocol.Identities.Mix(RdId.Root, GetType().Name));
      this.BindTopLevel(lifetime, protocol, GetType().Name);
      Protocol.InitTrace?.Log ($"CREATED toplevel object {this.PrintToString()}");
    }
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("InterningRoot1 (");
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
  
  
  public class InterningExtensionHolder : RdBindableBase
  {
    //fields
    //public fields
    
    //private fields
    private SerializationCtx mySerializationContext;
    public override bool TryGetSerializationContext(out SerializationCtx ctx) { ctx = mySerializationContext; return true; }
    //primary constructor
    //secondary constructor
    //statics
    
    public static CtxReadDelegate<InterningExtensionHolder> Read = (ctx, reader) => 
    {
      var _id = RdId.Read(reader);
      var _result = new InterningExtensionHolder().WithId(_id);
      _result.mySerializationContext = ctx.WithInternRootsHere(_result, "OutOfExt");
      return _result;
    };
    
    public static CtxWriteDelegate<InterningExtensionHolder> Write = (ctx, writer, value) => 
    {
      value.RdId.Write(writer);
      value.mySerializationContext = ctx.WithInternRootsHere(value, "OutOfExt");
    };
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("InterningExtensionHolder (");
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
  
  
  public class InterningMtModel : RdBindableBase
  {
    //fields
    //public fields
    [NotNull] public string SearchLabel {get; private set;}
    [NotNull] public ISignal<string> Signaller { get { return _Signaller; }}
    
    //private fields
    [NotNull] private readonly RdSignal<string> _Signaller;
    
    private SerializationCtx mySerializationContext;
    public override bool TryGetSerializationContext(out SerializationCtx ctx) { ctx = mySerializationContext; return true; }
    //primary constructor
    private InterningMtModel(
      [NotNull] string searchLabel,
      [NotNull] RdSignal<string> signaller
    )
    {
      if (searchLabel == null) throw new ArgumentNullException("searchLabel");
      if (signaller == null) throw new ArgumentNullException("signaller");
      
      SearchLabel = searchLabel;
      _Signaller = signaller;
      _Signaller.Async = true;
      BindableChildren.Add(new KeyValuePair<string, object>("signaller", _Signaller));
    }
    //secondary constructor
    public InterningMtModel (
      [NotNull] string searchLabel
    ) : this (
      searchLabel,
      new RdSignal<string>(ReadStringInternedAtTest, WriteStringInternedAtTest)
    ) {}
    //statics
    
    public static CtxReadDelegate<InterningMtModel> Read = (ctx, reader) => 
    {
      var _id = RdId.Read(reader);
      var searchLabel = reader.ReadString();
      var signaller = RdSignal<string>.Read(ctx, reader, ReadStringInternedAtTest, WriteStringInternedAtTest);
      var _result = new InterningMtModel(searchLabel, signaller).WithId(_id);
      _result.mySerializationContext = ctx.WithInternRootsHere(_result, "Test");
      return _result;
    };
    public static CtxReadDelegate<string> ReadStringInternedAtTest = Serializers.ReadString.Interned("Test");
    
    public static CtxWriteDelegate<InterningMtModel> Write = (ctx, writer, value) => 
    {
      value.RdId.Write(writer);
      writer.WriteString(value.SearchLabel);
      RdSignal<string>.Write(ctx, writer, value._Signaller);
      value.mySerializationContext = ctx.WithInternRootsHere(value, "Test");
    };
    public static CtxWriteDelegate<string> WriteStringInternedAtTest = Serializers.WriteString.Interned("Test");
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("InterningMtModel (");
      using (printer.IndentCookie()) {
        printer.Print("searchLabel = "); SearchLabel.PrintEx(printer); printer.Println();
        printer.Print("signaller = "); _Signaller.PrintEx(printer); printer.Println();
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
  
  
  public class InterningNestedTestModel : IPrintable, IEquatable<InterningNestedTestModel>
  {
    //fields
    //public fields
    [NotNull] public string Value {get; private set;}
    [CanBeNull] public InterningNestedTestModel Inner {get; private set;}
    
    //private fields
    //primary constructor
    public InterningNestedTestModel(
      [NotNull] string value,
      [CanBeNull] InterningNestedTestModel inner
    )
    {
      if (value == null) throw new ArgumentNullException("value");
      
      Value = value;
      Inner = inner;
    }
    //secondary constructor
    //statics
    
    public static CtxReadDelegate<InterningNestedTestModel> Read = (ctx, reader) => 
    {
      var value = reader.ReadString();
      var inner = ReadInterningNestedTestModelInternedNullable(ctx, reader);
      var _result = new InterningNestedTestModel(value, inner);
      return _result;
    };
    public static CtxReadDelegate<InterningNestedTestModel> ReadInterningNestedTestModelInternedNullable = InterningNestedTestModel.Read.Interned("Test").NullableClass();
    
    public static CtxWriteDelegate<InterningNestedTestModel> Write = (ctx, writer, value) => 
    {
      writer.WriteString(value.Value);
      WriteInterningNestedTestModelInternedNullable(ctx, writer, value.Inner);
    };
    public static CtxWriteDelegate<InterningNestedTestModel> WriteInterningNestedTestModelInternedNullable = InterningNestedTestModel.Write.Interned("Test").NullableClass();
    //custom body
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((InterningNestedTestModel) obj);
    }
    public bool Equals(InterningNestedTestModel other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Value == other.Value && Equals(Inner, other.Inner);
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + Value.GetHashCode();
        hash = hash * 31 + (Inner != null ?Inner.GetHashCode() : 0);
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("InterningNestedTestModel (");
      using (printer.IndentCookie()) {
        printer.Print("value = "); Value.PrintEx(printer); printer.Println();
        printer.Print("inner = "); Inner.PrintEx(printer); printer.Println();
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
  
  
  public class InterningNestedTestStringModel : IPrintable, IEquatable<InterningNestedTestStringModel>
  {
    //fields
    //public fields
    [NotNull] public string Value {get; private set;}
    [CanBeNull] public InterningNestedTestStringModel Inner {get; private set;}
    
    //private fields
    //primary constructor
    public InterningNestedTestStringModel(
      [NotNull] string value,
      [CanBeNull] InterningNestedTestStringModel inner
    )
    {
      if (value == null) throw new ArgumentNullException("value");
      
      Value = value;
      Inner = inner;
    }
    //secondary constructor
    //statics
    
    public static CtxReadDelegate<InterningNestedTestStringModel> Read = (ctx, reader) => 
    {
      var value = ctx.ReadInterned(reader, "Test", Serializers.ReadString);
      var inner = ReadInterningNestedTestStringModelNullable(ctx, reader);
      var _result = new InterningNestedTestStringModel(value, inner);
      return _result;
    };
    public static CtxReadDelegate<string> ReadStringInternedAtTest = Serializers.ReadString.Interned("Test");
    public static CtxReadDelegate<InterningNestedTestStringModel> ReadInterningNestedTestStringModelNullable = InterningNestedTestStringModel.Read.NullableClass();
    
    public static CtxWriteDelegate<InterningNestedTestStringModel> Write = (ctx, writer, value) => 
    {
      ctx.WriteInterned(writer, value.Value, "Test", Serializers.WriteString);
      WriteInterningNestedTestStringModelNullable(ctx, writer, value.Inner);
    };
    public static CtxWriteDelegate<string> WriteStringInternedAtTest = Serializers.WriteString.Interned("Test");
    public static CtxWriteDelegate<InterningNestedTestStringModel> WriteInterningNestedTestStringModelNullable = InterningNestedTestStringModel.Write.NullableClass();
    //custom body
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((InterningNestedTestStringModel) obj);
    }
    public bool Equals(InterningNestedTestStringModel other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Equals(Value, other.Value) && Equals(Inner, other.Inner);
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + Value.GetHashCode();
        hash = hash * 31 + (Inner != null ?Inner.GetHashCode() : 0);
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("InterningNestedTestStringModel (");
      using (printer.IndentCookie()) {
        printer.Print("value = "); Value.PrintEx(printer); printer.Println();
        printer.Print("inner = "); Inner.PrintEx(printer); printer.Println();
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
  
  
  public class InterningProtocolLevelModel : RdBindableBase
  {
    //fields
    //public fields
    [NotNull] public string SearchLabel {get; private set;}
    [NotNull] public IViewableMap<int, ProtocolWrappedStringModel> Issues { get { return _Issues; }}
    
    //private fields
    [NotNull] private readonly RdMap<int, ProtocolWrappedStringModel> _Issues;
    
    //primary constructor
    private InterningProtocolLevelModel(
      [NotNull] string searchLabel,
      [NotNull] RdMap<int, ProtocolWrappedStringModel> issues
    )
    {
      if (searchLabel == null) throw new ArgumentNullException("searchLabel");
      if (issues == null) throw new ArgumentNullException("issues");
      
      SearchLabel = searchLabel;
      _Issues = issues;
      _Issues.OptimizeNested = true;
      BindableChildren.Add(new KeyValuePair<string, object>("issues", _Issues));
    }
    //secondary constructor
    public InterningProtocolLevelModel (
      [NotNull] string searchLabel
    ) : this (
      searchLabel,
      new RdMap<int, ProtocolWrappedStringModel>(Serializers.ReadInt, Serializers.WriteInt, ProtocolWrappedStringModel.Read, ProtocolWrappedStringModel.Write)
    ) {}
    //statics
    
    public static CtxReadDelegate<InterningProtocolLevelModel> Read = (ctx, reader) => 
    {
      var _id = RdId.Read(reader);
      var searchLabel = reader.ReadString();
      var issues = RdMap<int, ProtocolWrappedStringModel>.Read(ctx, reader, Serializers.ReadInt, Serializers.WriteInt, ProtocolWrappedStringModel.Read, ProtocolWrappedStringModel.Write);
      var _result = new InterningProtocolLevelModel(searchLabel, issues).WithId(_id);
      return _result;
    };
    
    public static CtxWriteDelegate<InterningProtocolLevelModel> Write = (ctx, writer, value) => 
    {
      value.RdId.Write(writer);
      writer.WriteString(value.SearchLabel);
      RdMap<int, ProtocolWrappedStringModel>.Write(ctx, writer, value._Issues);
    };
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("InterningProtocolLevelModel (");
      using (printer.IndentCookie()) {
        printer.Print("searchLabel = "); SearchLabel.PrintEx(printer); printer.Println();
        printer.Print("issues = "); _Issues.PrintEx(printer); printer.Println();
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
  
  
  public class InterningTestModel : RdBindableBase
  {
    //fields
    //public fields
    [NotNull] public string SearchLabel {get; private set;}
    [NotNull] public IViewableMap<int, WrappedStringModel> Issues { get { return _Issues; }}
    
    //private fields
    [NotNull] private readonly RdMap<int, WrappedStringModel> _Issues;
    
    private SerializationCtx mySerializationContext;
    public override bool TryGetSerializationContext(out SerializationCtx ctx) { ctx = mySerializationContext; return true; }
    //primary constructor
    private InterningTestModel(
      [NotNull] string searchLabel,
      [NotNull] RdMap<int, WrappedStringModel> issues
    )
    {
      if (searchLabel == null) throw new ArgumentNullException("searchLabel");
      if (issues == null) throw new ArgumentNullException("issues");
      
      SearchLabel = searchLabel;
      _Issues = issues;
      _Issues.OptimizeNested = true;
      BindableChildren.Add(new KeyValuePair<string, object>("issues", _Issues));
    }
    //secondary constructor
    public InterningTestModel (
      [NotNull] string searchLabel
    ) : this (
      searchLabel,
      new RdMap<int, WrappedStringModel>(Serializers.ReadInt, Serializers.WriteInt, WrappedStringModel.Read, WrappedStringModel.Write)
    ) {}
    //statics
    
    public static CtxReadDelegate<InterningTestModel> Read = (ctx, reader) => 
    {
      var _id = RdId.Read(reader);
      var searchLabel = reader.ReadString();
      var issues = RdMap<int, WrappedStringModel>.Read(ctx, reader, Serializers.ReadInt, Serializers.WriteInt, WrappedStringModel.Read, WrappedStringModel.Write);
      var _result = new InterningTestModel(searchLabel, issues).WithId(_id);
      _result.mySerializationContext = ctx.WithInternRootsHere(_result, "Test");
      return _result;
    };
    
    public static CtxWriteDelegate<InterningTestModel> Write = (ctx, writer, value) => 
    {
      value.RdId.Write(writer);
      writer.WriteString(value.SearchLabel);
      RdMap<int, WrappedStringModel>.Write(ctx, writer, value._Issues);
      value.mySerializationContext = ctx.WithInternRootsHere(value, "Test");
    };
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("InterningTestModel (");
      using (printer.IndentCookie()) {
        printer.Print("searchLabel = "); SearchLabel.PrintEx(printer); printer.Println();
        printer.Print("issues = "); _Issues.PrintEx(printer); printer.Println();
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
  
  
  public class ProtocolWrappedStringModel : IPrintable, IEquatable<ProtocolWrappedStringModel>
  {
    //fields
    //public fields
    [NotNull] public string Text {get; private set;}
    
    //private fields
    //primary constructor
    public ProtocolWrappedStringModel(
      [NotNull] string text
    )
    {
      if (text == null) throw new ArgumentNullException("text");
      
      Text = text;
    }
    //secondary constructor
    //statics
    
    public static CtxReadDelegate<ProtocolWrappedStringModel> Read = (ctx, reader) => 
    {
      var text = ctx.ReadInterned(reader, "Protocol", Serializers.ReadString);
      var _result = new ProtocolWrappedStringModel(text);
      return _result;
    };
    public static CtxReadDelegate<string> ReadStringInternedAtProtocol = Serializers.ReadString.Interned("Protocol");
    
    public static CtxWriteDelegate<ProtocolWrappedStringModel> Write = (ctx, writer, value) => 
    {
      ctx.WriteInterned(writer, value.Text, "Protocol", Serializers.WriteString);
    };
    public static CtxWriteDelegate<string> WriteStringInternedAtProtocol = Serializers.WriteString.Interned("Protocol");
    //custom body
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((ProtocolWrappedStringModel) obj);
    }
    public bool Equals(ProtocolWrappedStringModel other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Equals(Text, other.Text);
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + Text.GetHashCode();
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("ProtocolWrappedStringModel (");
      using (printer.IndentCookie()) {
        printer.Print("text = "); Text.PrintEx(printer); printer.Println();
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
  
  
  public class WrappedStringModel : IPrintable, IEquatable<WrappedStringModel>
  {
    //fields
    //public fields
    [NotNull] public string Text {get; private set;}
    
    //private fields
    //primary constructor
    public WrappedStringModel(
      [NotNull] string text
    )
    {
      if (text == null) throw new ArgumentNullException("text");
      
      Text = text;
    }
    //secondary constructor
    //statics
    
    public static CtxReadDelegate<WrappedStringModel> Read = (ctx, reader) => 
    {
      var text = ctx.ReadInterned(reader, "Test", Serializers.ReadString);
      var _result = new WrappedStringModel(text);
      return _result;
    };
    public static CtxReadDelegate<string> ReadStringInternedAtTest = Serializers.ReadString.Interned("Test");
    
    public static CtxWriteDelegate<WrappedStringModel> Write = (ctx, writer, value) => 
    {
      ctx.WriteInterned(writer, value.Text, "Test", Serializers.WriteString);
    };
    public static CtxWriteDelegate<string> WriteStringInternedAtTest = Serializers.WriteString.Interned("Test");
    //custom body
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((WrappedStringModel) obj);
    }
    public bool Equals(WrappedStringModel other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Equals(Text, other.Text);
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + Text.GetHashCode();
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("WrappedStringModel (");
      using (printer.IndentCookie()) {
        printer.Print("text = "); Text.PrintEx(printer); printer.Println();
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
