//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a RdGen v1.11.
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


namespace org.example
{
  
  
  /// <summary>
  /// <p>Generated from: AsyncPrimitives.kt:13</p>
  /// </summary>
  public class AsyncPrimitivesExt : RdExtBase
  {
    //fields
    //public fields
    [NotNull] public IAsyncProperty<string> AsyncProperty => _AsyncProperty;
    [NotNull] public IAsyncProperty<string> AsyncPropertyNullable => _AsyncPropertyNullable;
    
    //private fields
    [NotNull] private readonly AsyncRdProperty<string> _AsyncProperty;
    [NotNull] private readonly AsyncRdProperty<string> _AsyncPropertyNullable;
    
    //primary constructor
    private AsyncPrimitivesExt(
      [NotNull] AsyncRdProperty<string> asyncProperty,
      [NotNull] AsyncRdProperty<string> asyncPropertyNullable
    )
    {
      if (asyncProperty == null) throw new ArgumentNullException("asyncProperty");
      if (asyncPropertyNullable == null) throw new ArgumentNullException("asyncPropertyNullable");
      
      _AsyncProperty = asyncProperty;
      _AsyncPropertyNullable = asyncPropertyNullable;
      _AsyncProperty.OptimizeNested = true;
      _AsyncPropertyNullable.OptimizeNested = true;
      _AsyncPropertyNullable.ValueCanBeNull = true;
      BindableChildren.Add(new KeyValuePair<string, object>("asyncProperty", _AsyncProperty));
      BindableChildren.Add(new KeyValuePair<string, object>("asyncPropertyNullable", _AsyncPropertyNullable));
    }
    //secondary constructor
    private AsyncPrimitivesExt (
    ) : this (
      new AsyncRdProperty<string>(JetBrains.Rd.Impl.Serializers.ReadString, JetBrains.Rd.Impl.Serializers.WriteString),
      new AsyncRdProperty<string>(ReadStringNullable, WriteStringNullable)
    ) {}
    //deconstruct trait
    //statics
    
    public static CtxReadDelegate<string> ReadStringNullable = JetBrains.Rd.Impl.Serializers.ReadString.NullableClass();
    
    public static  CtxWriteDelegate<string> WriteStringNullable = JetBrains.Rd.Impl.Serializers.WriteString.NullableClass();
    
    protected override long SerializationHash => 656430842423377943L;
    
    protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;
    public static void RegisterDeclaredTypesSerializers(ISerializers serializers)
    {
      
      serializers.RegisterToplevelOnce(typeof(AsyncPrimitivesRoot), AsyncPrimitivesRoot.RegisterDeclaredTypesSerializers);
    }
    
    public AsyncPrimitivesExt(Lifetime lifetime, IProtocol protocol) : this()
    {
      Identify(protocol.Identities, RdId.Root.Mix("AsyncPrimitivesExt"));
      this.BindTopLevel(lifetime, protocol, "AsyncPrimitivesExt");
    }
    
    //constants
    
    //custom body
    //methods
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("AsyncPrimitivesExt (");
      using (printer.IndentCookie()) {
        printer.Print("asyncProperty = "); _AsyncProperty.PrintEx(printer); printer.Println();
        printer.Print("asyncPropertyNullable = "); _AsyncPropertyNullable.PrintEx(printer); printer.Println();
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