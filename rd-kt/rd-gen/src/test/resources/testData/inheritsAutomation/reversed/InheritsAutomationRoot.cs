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


namespace InheritsAutomationRoot
{
  
  
  /// <summary>
  /// <p>Generated from: InheritsAutomationTest.kt:17</p>
  /// </summary>
  public class InheritsAutomationRoot : RdExtBase
  {
    //fields
    //public fields
    
    //private fields
    //primary constructor
    internal static InheritsAutomationRoot CreateInternal()
    {
      return new InheritsAutomationRoot();
    }
    
    private InheritsAutomationRoot(
    )
    {
    }
    //secondary constructor
    //deconstruct trait
    //statics
    
    
    
    protected override long SerializationHash => -2258665308755484909L;
    
    protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;
    public static void RegisterDeclaredTypesSerializers(ISerializers serializers)
    {
      
      serializers.RegisterToplevelOnce(typeof(InheritsAutomationRoot), InheritsAutomationRoot.RegisterDeclaredTypesSerializers);
      serializers.RegisterToplevelOnce(typeof(InheritsAutomationExtension), InheritsAutomationExtension.RegisterDeclaredTypesSerializers);
    }
    
    public InheritsAutomationRoot(Lifetime lifetime, IProtocol protocol) : this()
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
      printer.Println("InheritsAutomationRoot (");
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
  
  public static class ProtocolInheritsAutomationRootEx
  {
    public static InheritsAutomationRoot GetInheritsAutomationRoot(this IProtocol protocol)
    {
      return protocol.GetOrCreateExtension(() => InheritsAutomationRoot.CreateInternal());
    }
    
  }
}
