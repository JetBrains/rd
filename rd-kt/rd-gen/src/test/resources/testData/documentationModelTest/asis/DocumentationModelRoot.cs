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


namespace org.example
{
  
  
  /// <summary>
  /// This is a documentation test,
  and it is also multiline.
  /// <p>Generated from: ModelWithDocumentation.kt:14</p>
  /// </summary>
  public class DocumentationModelRoot : RdExtBase
  {
    //fields
    //public fields
    
    //private fields
    //primary constructor
    private DocumentationModelRoot(
    )
    {
    }
    //secondary constructor
    //deconstruct trait
    //statics
    
    
    
    protected override long SerializationHash => -7782552752810534509L;
    
    protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;
    public static void RegisterDeclaredTypesSerializers(ISerializers serializers)
    {
      
      serializers.RegisterToplevelOnce(typeof(DocumentationModelRoot), DocumentationModelRoot.RegisterDeclaredTypesSerializers);
    }
    
    public DocumentationModelRoot(Lifetime lifetime, IProtocol protocol) : this()
    {
      Identify(protocol.Identities, RdId.Root.Mix("DocumentationModelRoot"));
      this.BindTopLevel(lifetime, protocol, "DocumentationModelRoot");
    }
    
    //constants
    
    //custom body
    //methods
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("DocumentationModelRoot (");
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
