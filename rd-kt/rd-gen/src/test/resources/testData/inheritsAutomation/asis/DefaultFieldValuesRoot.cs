//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a RdGen v1.13.
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------
using System;
using System.Linq;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using JetBrains.Annotations;

using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Serialization;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Rd.Util;
using JetBrains.Rd.Text;


// ReSharper disable RedundantEmptyObjectCreationArgumentList
// ReSharper disable InconsistentNaming
// ReSharper disable RedundantOverflowCheckingContext


namespace DefaultFieldValuesRoot
{
  
  
  /// <summary>
  /// <p>Generated from: DefaultFieldValuesTest.kt:17</p>
  /// </summary>
  public class DefaultFieldValuesRoot : RdExtBase
  {
    //fields
    //public fields
    
    //private fields
    //primary constructor
    internal static DefaultFieldValuesRoot CreateInternal()
    {
      return new DefaultFieldValuesRoot();
    }
    
    private DefaultFieldValuesRoot(
    )
    {
    }
    //secondary constructor
    //deconstruct trait
    //statics
    
    
    
    protected override long SerializationHash => 3367454536443547292L;
    
    protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;
    public static void RegisterDeclaredTypesSerializers(ISerializers serializers)
    {
      
      serializers.RegisterToplevelOnce(typeof(DefaultFieldValuesRoot), DefaultFieldValuesRoot.RegisterDeclaredTypesSerializers);
    }
    
    public DefaultFieldValuesRoot(Lifetime lifetime, IProtocol protocol) : this()
    {
      var ext = protocol.GetOrCreateExtension(() => this);
      if (!ReferenceEquals(ext, this))
        throw new InvalidOperationException($"Returned ext: {ext} is not equal to {this}");
    }
    
    //constants
    
    //custom body
    //methods
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("DefaultFieldValuesRoot (");
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
  
  public static class ProtocolDefaultFieldValuesRootEx
  {
    public static DefaultFieldValuesRoot GetDefaultFieldValuesRoot(this IProtocol protocol)
    {
      return protocol.GetOrCreateExtension(() => DefaultFieldValuesRoot.CreateInternal());
    }
  }
  
  
  /// <summary>
  /// <p>Generated from: DefaultFieldValuesTest.kt:25</p>
  /// </summary>
  public sealed class TestClass : RdBindableBase
  {
    //fields
    //public fields
    public int CHasNoDefaultValue {get; private set;}
    public int CHasDefaultValue {get; private set;}
    [CanBeNull] public int? COptional {get; private set;}
    public int CHasNoDefaultValueEither {get; private set;}
    [NotNull] public string CHasDefaultValueToo {get; private set;}
    [NotNull] public IViewableMap<int, int> Foo => _Foo;
    
    //private fields
    [NotNull] private readonly RdMap<int, int> _Foo;
    
    //primary constructor
    private TestClass(
      int cHasNoDefaultValue,
      [Optional] [DefaultParameterValue(0)] int cHasDefaultValue,
      [CanBeNull] [Optional] int? cOptional,
      int cHasNoDefaultValueEither,
      [NotNull] [Optional] [DefaultParameterValue("too")] string cHasDefaultValueToo,
      [NotNull] RdMap<int, int> foo
    )
    {
      if (cHasDefaultValueToo == null) throw new ArgumentNullException("cHasDefaultValueToo");
      if (foo == null) throw new ArgumentNullException("foo");
      
      CHasNoDefaultValue = cHasNoDefaultValue;
      CHasDefaultValue = cHasDefaultValue;
      COptional = cOptional;
      CHasNoDefaultValueEither = cHasNoDefaultValueEither;
      CHasDefaultValueToo = cHasDefaultValueToo;
      _Foo = foo;
      _Foo.OptimizeNested = true;
      BindableChildren.Add(new KeyValuePair<string, object>("foo", _Foo));
    }
    //secondary constructor
    public TestClass (
      int cHasNoDefaultValue,
      [Optional] [DefaultParameterValue(0)] int cHasDefaultValue,
      [CanBeNull] [Optional] int? cOptional,
      int cHasNoDefaultValueEither,
      [NotNull] string cHasDefaultValueToo = "too"
    ) : this (
      cHasNoDefaultValue,
      cHasDefaultValue,
      cOptional,
      cHasNoDefaultValueEither,
      cHasDefaultValueToo,
      new RdMap<int, int>(JetBrains.Rd.Impl.Serializers.ReadInt, JetBrains.Rd.Impl.Serializers.WriteInt, JetBrains.Rd.Impl.Serializers.ReadInt, JetBrains.Rd.Impl.Serializers.WriteInt)
    ) {}
    //deconstruct trait
    //statics
    
    public static CtxReadDelegate<TestClass> Read = (ctx, reader) => 
    {
      var _id = RdId.Read(reader);
      var cHasNoDefaultValue = reader.ReadInt();
      var cHasDefaultValue = reader.ReadInt();
      var cOptional = ReadIntNullable(ctx, reader);
      var cHasNoDefaultValueEither = reader.ReadInt();
      var cHasDefaultValueToo = reader.ReadString();
      var foo = RdMap<int, int>.Read(ctx, reader, JetBrains.Rd.Impl.Serializers.ReadInt, JetBrains.Rd.Impl.Serializers.WriteInt, JetBrains.Rd.Impl.Serializers.ReadInt, JetBrains.Rd.Impl.Serializers.WriteInt);
      var _result = new TestClass(cHasNoDefaultValue, cHasDefaultValue, cOptional, cHasNoDefaultValueEither, cHasDefaultValueToo, foo).WithId(_id);
      return _result;
    };
    public static CtxReadDelegate<int?> ReadIntNullable = JetBrains.Rd.Impl.Serializers.ReadInt.NullableStruct();
    
    public static CtxWriteDelegate<TestClass> Write = (ctx, writer, value) => 
    {
      value.RdId.Write(writer);
      writer.Write(value.CHasNoDefaultValue);
      writer.Write(value.CHasDefaultValue);
      WriteIntNullable(ctx, writer, value.COptional);
      writer.Write(value.CHasNoDefaultValueEither);
      writer.Write(value.CHasDefaultValueToo);
      RdMap<int, int>.Write(ctx, writer, value._Foo);
    };
    public static  CtxWriteDelegate<int?> WriteIntNullable = JetBrains.Rd.Impl.Serializers.WriteInt.NullableStruct();
    
    //constants
    
    //custom body
    //methods
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("TestClass (");
      using (printer.IndentCookie()) {
        printer.Print("cHasNoDefaultValue = "); CHasNoDefaultValue.PrintEx(printer); printer.Println();
        printer.Print("cHasDefaultValue = "); CHasDefaultValue.PrintEx(printer); printer.Println();
        printer.Print("cOptional = "); COptional.PrintEx(printer); printer.Println();
        printer.Print("cHasNoDefaultValueEither = "); CHasNoDefaultValueEither.PrintEx(printer); printer.Println();
        printer.Print("cHasDefaultValueToo = "); CHasDefaultValueToo.PrintEx(printer); printer.Println();
        printer.Print("foo = "); _Foo.PrintEx(printer); printer.Println();
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
  
  
  /// <summary>
  /// <p>Generated from: DefaultFieldValuesTest.kt:18</p>
  /// </summary>
  public sealed class TestStruct : IPrintable, IEquatable<TestStruct>
  {
    //fields
    //public fields
    public int SHasNoDefaultValue {get; private set;}
    public int SHasDefaultValue {get; private set;}
    [CanBeNull] public int? SOptional {get; private set;}
    public int SHasNoDefaultValueEither {get; private set;}
    [NotNull] public string SHasDefaultValueToo {get; private set;}
    
    //private fields
    //primary constructor
    public TestStruct(
      int sHasNoDefaultValue,
      [Optional] [DefaultParameterValue(0)] int sHasDefaultValue,
      [CanBeNull] [Optional] int? sOptional,
      int sHasNoDefaultValueEither,
      [NotNull] string sHasDefaultValueToo = "too"
    )
    {
      if (sHasDefaultValueToo == null) throw new ArgumentNullException("sHasDefaultValueToo");
      
      SHasNoDefaultValue = sHasNoDefaultValue;
      SHasDefaultValue = sHasDefaultValue;
      SOptional = sOptional;
      SHasNoDefaultValueEither = sHasNoDefaultValueEither;
      SHasDefaultValueToo = sHasDefaultValueToo;
    }
    //secondary constructor
    //deconstruct trait
    public void Deconstruct(out int sHasNoDefaultValue, out int sHasDefaultValue, [CanBeNull] out int? sOptional, out int sHasNoDefaultValueEither, [NotNull] out string sHasDefaultValueToo)
    {
      sHasNoDefaultValue = SHasNoDefaultValue;
      sHasDefaultValue = SHasDefaultValue;
      sOptional = SOptional;
      sHasNoDefaultValueEither = SHasNoDefaultValueEither;
      sHasDefaultValueToo = SHasDefaultValueToo;
    }
    //statics
    
    public static CtxReadDelegate<TestStruct> Read = (ctx, reader) => 
    {
      var sHasNoDefaultValue = reader.ReadInt();
      var sHasDefaultValue = reader.ReadInt();
      var sOptional = ReadIntNullable(ctx, reader);
      var sHasNoDefaultValueEither = reader.ReadInt();
      var sHasDefaultValueToo = reader.ReadString();
      var _result = new TestStruct(sHasNoDefaultValue, sHasDefaultValue, sOptional, sHasNoDefaultValueEither, sHasDefaultValueToo);
      return _result;
    };
    public static CtxReadDelegate<int?> ReadIntNullable = JetBrains.Rd.Impl.Serializers.ReadInt.NullableStruct();
    
    public static CtxWriteDelegate<TestStruct> Write = (ctx, writer, value) => 
    {
      writer.Write(value.SHasNoDefaultValue);
      writer.Write(value.SHasDefaultValue);
      WriteIntNullable(ctx, writer, value.SOptional);
      writer.Write(value.SHasNoDefaultValueEither);
      writer.Write(value.SHasDefaultValueToo);
    };
    public static  CtxWriteDelegate<int?> WriteIntNullable = JetBrains.Rd.Impl.Serializers.WriteInt.NullableStruct();
    
    //constants
    
    //custom body
    //methods
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((TestStruct) obj);
    }
    public bool Equals(TestStruct other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return SHasNoDefaultValue == other.SHasNoDefaultValue && SHasDefaultValue == other.SHasDefaultValue && Equals(SOptional, other.SOptional) && SHasNoDefaultValueEither == other.SHasNoDefaultValueEither && SHasDefaultValueToo == other.SHasDefaultValueToo;
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + SHasNoDefaultValue.GetHashCode();
        hash = hash * 31 + SHasDefaultValue.GetHashCode();
        hash = hash * 31 + (SOptional != null ? SOptional.GetHashCode() : 0);
        hash = hash * 31 + SHasNoDefaultValueEither.GetHashCode();
        hash = hash * 31 + SHasDefaultValueToo.GetHashCode();
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("TestStruct (");
      using (printer.IndentCookie()) {
        printer.Print("sHasNoDefaultValue = "); SHasNoDefaultValue.PrintEx(printer); printer.Println();
        printer.Print("sHasDefaultValue = "); SHasDefaultValue.PrintEx(printer); printer.Println();
        printer.Print("sOptional = "); SOptional.PrintEx(printer); printer.Println();
        printer.Print("sHasNoDefaultValueEither = "); SHasNoDefaultValueEither.PrintEx(printer); printer.Println();
        printer.Print("sHasDefaultValueToo = "); SHasDefaultValueToo.PrintEx(printer); printer.Println();
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
