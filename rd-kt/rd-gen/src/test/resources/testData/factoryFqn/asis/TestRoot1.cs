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


namespace Org.TestRoot1
{
  
  
  /// <summary>
  /// <p>Generated from: FactoryFqnTestDataModel.kt:16</p>
  /// </summary>
  public class TestRoot1 : DefaultExtBase
  {
    //fields
    //public fields
    
    //private fields
    //primary constructor
    internal static TestRoot1 CreateInternal()
    {
      return new TestRoot1();
    }
    
    private TestRoot1(
    )
    {
    }
    //secondary constructor
    //deconstruct trait
    //statics
    
    
    
    protected override long SerializationHash => 474298181957578587L;
    
    protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;
    public static void RegisterDeclaredTypesSerializers(ISerializers serializers)
    {
      
      serializers.RegisterToplevelOnce(typeof(TestRoot1), TestRoot1.RegisterDeclaredTypesSerializers);
      serializers.RegisterToplevelOnce(typeof(Solution2), Solution2.RegisterDeclaredTypesSerializers);
    }
    
    public TestRoot1(Lifetime lifetime, IProtocol protocol) : this()
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
      printer.Println("TestRoot1 (");
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
  
  public static class ProtocolTestRoot1Ex
  {
    public static TestRoot1 GetTestRoot1(this IProtocol protocol)
    {
      return protocol.GetOrCreateExtension(() => TestRoot1.CreateInternal());
    }
  }
}
