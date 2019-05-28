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


namespace demo
{
  
  
  public class DemoModel : RdExtBase
  {
    //fields
    //public fields
    [NotNull] public IViewableProperty<bool> Boolean_property => _Boolean_property;
    [NotNull] public IViewableProperty<bool[]> Boolean_array => _Boolean_array;
    [NotNull] public IViewableProperty<MyScalar> Scalar => _Scalar;
    [NotNull] public IViewableProperty<byte> Ubyte => _Ubyte;
    [NotNull] public IViewableProperty<byte[]> Ubyte_array => _Ubyte_array;
    [NotNull] public IViewableList<int> List => _List;
    [NotNull] public IViewableSet<int> Set => _Set;
    [NotNull] public IViewableMap<long, string> MapLongToString => _MapLongToString;
    [NotNull] public IRdCall<char, string> Call => _Call;
    [NotNull] public RdEndpoint<string, int> Callback => _Callback;
    [NotNull] public IViewableProperty<string> Interned_string => _Interned_string;
    [NotNull] public IViewableProperty<Base> Polymorphic => _Polymorphic;
    [NotNull] public IViewableProperty<MyEnum> Enum => _Enum;
    
    //private fields
    [NotNull] private readonly RdProperty<bool> _Boolean_property;
    [NotNull] private readonly RdProperty<bool[]> _Boolean_array;
    [NotNull] private readonly RdProperty<MyScalar> _Scalar;
    [NotNull] private readonly RdProperty<byte> _Ubyte;
    [NotNull] private readonly RdProperty<byte[]> _Ubyte_array;
    [NotNull] private readonly RdList<int> _List;
    [NotNull] private readonly RdSet<int> _Set;
    [NotNull] private readonly RdMap<long, string> _MapLongToString;
    [NotNull] private readonly RdCall<char, string> _Call;
    [NotNull] private readonly RdEndpoint<string, int> _Callback;
    [NotNull] private readonly RdProperty<string> _Interned_string;
    [NotNull] private readonly RdProperty<Base> _Polymorphic;
    [NotNull] private readonly RdProperty<MyEnum> _Enum;
    
    //primary constructor
    private DemoModel(
      [NotNull] RdProperty<bool> boolean_property,
      [NotNull] RdProperty<bool[]> boolean_array,
      [NotNull] RdProperty<MyScalar> scalar,
      [NotNull] RdProperty<byte> ubyte,
      [NotNull] RdProperty<byte[]> ubyte_array,
      [NotNull] RdList<int> list,
      [NotNull] RdSet<int> set,
      [NotNull] RdMap<long, string> mapLongToString,
      [NotNull] RdCall<char, string> call,
      [NotNull] RdEndpoint<string, int> callback,
      [NotNull] RdProperty<string> interned_string,
      [NotNull] RdProperty<Base> polymorphic,
      [NotNull] RdProperty<MyEnum> @enum
    )
    {
      if (boolean_property == null) throw new ArgumentNullException("boolean_property");
      if (boolean_array == null) throw new ArgumentNullException("boolean_array");
      if (scalar == null) throw new ArgumentNullException("scalar");
      if (ubyte == null) throw new ArgumentNullException("ubyte");
      if (ubyte_array == null) throw new ArgumentNullException("ubyte_array");
      if (list == null) throw new ArgumentNullException("list");
      if (set == null) throw new ArgumentNullException("set");
      if (mapLongToString == null) throw new ArgumentNullException("mapLongToString");
      if (call == null) throw new ArgumentNullException("call");
      if (callback == null) throw new ArgumentNullException("callback");
      if (interned_string == null) throw new ArgumentNullException("interned_string");
      if (polymorphic == null) throw new ArgumentNullException("polymorphic");
      if (@enum == null) throw new ArgumentNullException("enum");
      
      _Boolean_property = boolean_property;
      _Boolean_array = boolean_array;
      _Scalar = scalar;
      _Ubyte = ubyte;
      _Ubyte_array = ubyte_array;
      _List = list;
      _Set = set;
      _MapLongToString = mapLongToString;
      _Call = call;
      _Callback = callback;
      _Interned_string = interned_string;
      _Polymorphic = polymorphic;
      _Enum = @enum;
      _Boolean_property.OptimizeNested = true;
      _Boolean_array.OptimizeNested = true;
      _Scalar.OptimizeNested = true;
      _Ubyte.OptimizeNested = true;
      _Ubyte_array.OptimizeNested = true;
      _List.OptimizeNested = true;
      _Set.OptimizeNested = true;
      _MapLongToString.OptimizeNested = true;
      _Interned_string.OptimizeNested = true;
      _Polymorphic.OptimizeNested = true;
      _Enum.OptimizeNested = true;
      BindableChildren.Add(new KeyValuePair<string, object>("boolean_property", _Boolean_property));
      BindableChildren.Add(new KeyValuePair<string, object>("boolean_array", _Boolean_array));
      BindableChildren.Add(new KeyValuePair<string, object>("scalar", _Scalar));
      BindableChildren.Add(new KeyValuePair<string, object>("ubyte", _Ubyte));
      BindableChildren.Add(new KeyValuePair<string, object>("ubyte_array", _Ubyte_array));
      BindableChildren.Add(new KeyValuePair<string, object>("list", _List));
      BindableChildren.Add(new KeyValuePair<string, object>("set", _Set));
      BindableChildren.Add(new KeyValuePair<string, object>("mapLongToString", _MapLongToString));
      BindableChildren.Add(new KeyValuePair<string, object>("call", _Call));
      BindableChildren.Add(new KeyValuePair<string, object>("callback", _Callback));
      BindableChildren.Add(new KeyValuePair<string, object>("interned_string", _Interned_string));
      BindableChildren.Add(new KeyValuePair<string, object>("polymorphic", _Polymorphic));
      BindableChildren.Add(new KeyValuePair<string, object>("enum", _Enum));
    }
    //secondary constructor
    private DemoModel (
    ) : this (
      new RdProperty<bool>(JetBrains.Rd.Impl.Serializers.ReadBool, JetBrains.Rd.Impl.Serializers.WriteBool),
      new RdProperty<bool[]>(JetBrains.Rd.Impl.Serializers.ReadBoolArray, JetBrains.Rd.Impl.Serializers.WriteBoolArray),
      new RdProperty<MyScalar>(MyScalar.Read, MyScalar.Write),
      new RdProperty<byte>(JetBrains.Rd.Impl.Serializers.ReadUByte, JetBrains.Rd.Impl.Serializers.WriteUByte),
      new RdProperty<byte[]>(ReadUByteArray, WriteUByteArray),
      new RdList<int>(JetBrains.Rd.Impl.Serializers.ReadInt, JetBrains.Rd.Impl.Serializers.WriteInt),
      new RdSet<int>(JetBrains.Rd.Impl.Serializers.ReadInt, JetBrains.Rd.Impl.Serializers.WriteInt),
      new RdMap<long, string>(JetBrains.Rd.Impl.Serializers.ReadLong, JetBrains.Rd.Impl.Serializers.WriteLong, JetBrains.Rd.Impl.Serializers.ReadString, JetBrains.Rd.Impl.Serializers.WriteString),
      new RdCall<char, string>(JetBrains.Rd.Impl.Serializers.ReadChar, JetBrains.Rd.Impl.Serializers.WriteChar, JetBrains.Rd.Impl.Serializers.ReadString, JetBrains.Rd.Impl.Serializers.WriteString),
      new RdEndpoint<string, int>(JetBrains.Rd.Impl.Serializers.ReadString, JetBrains.Rd.Impl.Serializers.WriteString, JetBrains.Rd.Impl.Serializers.ReadInt, JetBrains.Rd.Impl.Serializers.WriteInt),
      new RdProperty<string>(ReadStringInternedAtProtocol, WriteStringInternedAtProtocol),
      new RdProperty<Base>(Base.Read, Base.Write),
      new RdProperty<MyEnum>(ReadMyEnum, WriteMyEnum)
    ) {}
    //statics
    
    public static CtxReadDelegate<byte[]> ReadUByteArray = JetBrains.Rd.Impl.Serializers.ReadUByte.Array();
    public static CtxReadDelegate<string> ReadStringInternedAtProtocol = JetBrains.Rd.Impl.Serializers.ReadString.Interned("Protocol");
    public static CtxReadDelegate<MyEnum> ReadMyEnum = new CtxReadDelegate<MyEnum>(JetBrains.Rd.Impl.Serializers.ReadEnum<MyEnum>);
    
    public static CtxWriteDelegate<byte[]> WriteUByteArray = JetBrains.Rd.Impl.Serializers.WriteUByte.Array();
    public static CtxWriteDelegate<string> WriteStringInternedAtProtocol = JetBrains.Rd.Impl.Serializers.WriteString.Interned("Protocol");
    public static CtxWriteDelegate<MyEnum> WriteMyEnum = new CtxWriteDelegate<MyEnum>(JetBrains.Rd.Impl.Serializers.WriteEnum<MyEnum>);
    
    protected override long SerializationHash => -6563454397007024222L;
    
    protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;
    public static void RegisterDeclaredTypesSerializers(ISerializers serializers)
    {
      serializers.Register(Derived.Read, Derived.Write);
      serializers.Register(Base_Unknown.Read, Base_Unknown.Write);
      
      serializers.RegisterToplevelOnce(typeof(DemoRoot), DemoRoot.RegisterDeclaredTypesSerializers);
    }
    
    public DemoModel(Lifetime lifetime, IProtocol protocol) : this()
    {
      Identify(protocol.Identities, RdId.Root.Mix(GetType().Name));
      Bind(lifetime, protocol, GetType().Name);
      if (Protocol.InitializationLogger.IsTraceEnabled())
        Protocol.InitializationLogger.Trace ("CREATED toplevel object {0}", this.PrintToString());
    }
    
    //constants
    public const bool const_toplevel = true;
    
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("DemoModel (");
      using (printer.IndentCookie()) {
        printer.Print("boolean_property = "); _Boolean_property.PrintEx(printer); printer.Println();
        printer.Print("boolean_array = "); _Boolean_array.PrintEx(printer); printer.Println();
        printer.Print("scalar = "); _Scalar.PrintEx(printer); printer.Println();
        printer.Print("ubyte = "); _Ubyte.PrintEx(printer); printer.Println();
        printer.Print("ubyte_array = "); _Ubyte_array.PrintEx(printer); printer.Println();
        printer.Print("list = "); _List.PrintEx(printer); printer.Println();
        printer.Print("set = "); _Set.PrintEx(printer); printer.Println();
        printer.Print("mapLongToString = "); _MapLongToString.PrintEx(printer); printer.Println();
        printer.Print("call = "); _Call.PrintEx(printer); printer.Println();
        printer.Print("callback = "); _Callback.PrintEx(printer); printer.Println();
        printer.Print("interned_string = "); _Interned_string.PrintEx(printer); printer.Println();
        printer.Print("polymorphic = "); _Polymorphic.PrintEx(printer); printer.Println();
        printer.Print("enum = "); _Enum.PrintEx(printer); printer.Println();
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
  
  
  public abstract class Base{
    //fields
    //public fields
    
    //private fields
    //primary constructor
    //secondary constructor
    //statics
    
    public static CtxReadDelegate<Base> Read = Polymorphic<Base>.ReadAbstract(Base_Unknown.Read);
    
    public static CtxWriteDelegate<Base> Write = Polymorphic<Base>.Write;
    
    //constants
    public const char const_base = 'B';
    
    //custom body
    //equals trait
    //hash code trait
    //pretty print
    //toString
  }
  
  
  public class Base_Unknown : Base
  {
    //fields
    //public fields
    
    //private fields
    //primary constructor
    //secondary constructor
    //statics
    
    public static new CtxReadDelegate<Base_Unknown> Read = (ctx, reader) => 
    {
      var _result = new Base_Unknown();
      return _result;
    };
    
    public static new CtxWriteDelegate<Base_Unknown> Write = (ctx, writer, value) => 
    {
    };
    
    //constants
    
    //custom body
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((Base_Unknown) obj);
    }
    public bool Equals(Base_Unknown other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return true;
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("Base_Unknown (");
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
  
  
  public class Derived : Base
  {
    //fields
    //public fields
    [NotNull] public string String {get; private set;}
    
    //private fields
    //primary constructor
    public Derived(
      [NotNull] string @string
    )
    {
      if (@string == null) throw new ArgumentNullException("string");
      
      String = @string;
    }
    //secondary constructor
    //statics
    
    public static new CtxReadDelegate<Derived> Read = (ctx, reader) => 
    {
      var @string = reader.ReadString();
      var _result = new Derived(@string);
      return _result;
    };
    
    public static new CtxWriteDelegate<Derived> Write = (ctx, writer, value) => 
    {
      writer.Write(value.String);
    };
    
    //constants
    
    //custom body
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((Derived) obj);
    }
    public bool Equals(Derived other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return String == other.String;
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + String.GetHashCode();
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("Derived (");
      using (printer.IndentCookie()) {
        printer.Print("string = "); String.PrintEx(printer); printer.Println();
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
  
  
  public enum MyEnum {
    @default,
    kt,
    net,
    cpp
  }
  
  
  public class MyScalar : IPrintable, IEquatable<MyScalar>
  {
    //fields
    //public fields
    public bool Bool {get; private set;}
    public byte Byte {get; private set;}
    public short Short {get; private set;}
    public int Int {get; private set;}
    public long Long {get; private set;}
    public float Float {get; private set;}
    public double Double {get; private set;}
    [NotNull] public byte Unsigned_byte {get; private set;}
    [NotNull] public ushort Unsigned_short {get; private set;}
    [NotNull] public uint Unsigned_int {get; private set;}
    [NotNull] public ulong Unsigned_long {get; private set;}
    public MyEnum Enum {get; private set;}
    
    //private fields
    //primary constructor
    public MyScalar(
      bool @bool,
      byte @byte,
      short @short,
      int @int,
      long @long,
      float @float,
      double @double,
      [NotNull] byte unsigned_byte,
      [NotNull] ushort unsigned_short,
      [NotNull] uint unsigned_int,
      [NotNull] ulong unsigned_long,
      MyEnum @enum
    )
    {
      if (unsigned_byte == null) throw new ArgumentNullException("unsigned_byte");
      if (unsigned_short == null) throw new ArgumentNullException("unsigned_short");
      if (unsigned_int == null) throw new ArgumentNullException("unsigned_int");
      if (unsigned_long == null) throw new ArgumentNullException("unsigned_long");
      
      Bool = @bool;
      Byte = @byte;
      Short = @short;
      Int = @int;
      Long = @long;
      Float = @float;
      Double = @double;
      Unsigned_byte = unsigned_byte;
      Unsigned_short = unsigned_short;
      Unsigned_int = unsigned_int;
      Unsigned_long = unsigned_long;
      Enum = @enum;
    }
    //secondary constructor
    //statics
    
    public static CtxReadDelegate<MyScalar> Read = (ctx, reader) => 
    {
      var @bool = reader.ReadBool();
      var @byte = reader.ReadByte();
      var @short = reader.ReadShort();
      var @int = reader.ReadInt();
      var @long = reader.ReadLong();
      var @float = reader.ReadFloat();
      var @double = reader.ReadDouble();
      var unsigned_byte = reader.ReadUByte();
      var unsigned_short = reader.ReadUShort();
      var unsigned_int = reader.ReadUInt();
      var unsigned_long = reader.ReadULong();
      var @enum = (MyEnum)reader.ReadInt();
      var _result = new MyScalar(@bool, @byte, @short, @int, @long, @float, @double, unsigned_byte, unsigned_short, unsigned_int, unsigned_long, @enum);
      return _result;
    };
    
    public static CtxWriteDelegate<MyScalar> Write = (ctx, writer, value) => 
    {
      writer.Write(value.Bool);
      writer.Write(value.Byte);
      writer.Write(value.Short);
      writer.Write(value.Int);
      writer.Write(value.Long);
      writer.Write(value.Float);
      writer.Write(value.Double);
      writer.Write(value.Unsigned_byte);
      writer.Write(value.Unsigned_short);
      writer.Write(value.Unsigned_int);
      writer.Write(value.Unsigned_long);
      writer.Write((int)value.Enum);
    };
    
    //constants
    public const int const_int = 0;
    public const string const_string = "const_string_value";
    public const MyEnum const_enum = MyEnum.@default;
    
    //custom body
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((MyScalar) obj);
    }
    public bool Equals(MyScalar other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Bool == other.Bool && Byte == other.Byte && Short == other.Short && Int == other.Int && Long == other.Long && Float == other.Float && Double == other.Double && Unsigned_byte == other.Unsigned_byte && Unsigned_short == other.Unsigned_short && Unsigned_int == other.Unsigned_int && Unsigned_long == other.Unsigned_long && Enum == other.Enum;
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + Bool.GetHashCode();
        hash = hash * 31 + Byte.GetHashCode();
        hash = hash * 31 + Short.GetHashCode();
        hash = hash * 31 + Int.GetHashCode();
        hash = hash * 31 + Long.GetHashCode();
        hash = hash * 31 + Float.GetHashCode();
        hash = hash * 31 + Double.GetHashCode();
        hash = hash * 31 + Unsigned_byte.GetHashCode();
        hash = hash * 31 + Unsigned_short.GetHashCode();
        hash = hash * 31 + Unsigned_int.GetHashCode();
        hash = hash * 31 + Unsigned_long.GetHashCode();
        hash = hash * 31 + (int) Enum;
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("MyScalar (");
      using (printer.IndentCookie()) {
        printer.Print("bool = "); Bool.PrintEx(printer); printer.Println();
        printer.Print("byte = "); Byte.PrintEx(printer); printer.Println();
        printer.Print("short = "); Short.PrintEx(printer); printer.Println();
        printer.Print("int = "); Int.PrintEx(printer); printer.Println();
        printer.Print("long = "); Long.PrintEx(printer); printer.Println();
        printer.Print("float = "); Float.PrintEx(printer); printer.Println();
        printer.Print("double = "); Double.PrintEx(printer); printer.Println();
        printer.Print("unsigned_byte = "); Unsigned_byte.PrintEx(printer); printer.Println();
        printer.Print("unsigned_short = "); Unsigned_short.PrintEx(printer); printer.Println();
        printer.Print("unsigned_int = "); Unsigned_int.PrintEx(printer); printer.Println();
        printer.Print("unsigned_long = "); Unsigned_long.PrintEx(printer); printer.Println();
        printer.Print("enum = "); Enum.PrintEx(printer); printer.Println();
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
