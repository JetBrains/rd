using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Util;
using JetBrains.Util.Internal;

namespace JetBrains.Serialization
{
  /// <summary>
  /// Deserialize data from byte buffer that was initially serialized by <see cref="UnsafeWriter"/>
  /// <seealso cref="UnsafeReader"/>
  /// 
  /// </summary>
  //Can't be struct because internal state must change during methods invocation
  public unsafe class UnsafeReader
  {    
    private byte* myPtr;

    private byte* myInitialPtr;
    private int myMaxlen;


    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static UnsafeReader CreateReader(byte* ptr, int len)
    {
      UnsafeReader reader = new UnsafeReader();
      reader.Reset(ptr, len);
      return reader;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void With(byte[] data, Action<UnsafeReader> action)
    {
      fixed (byte* resptr = data)
        action(CreateReader(resptr, data.Length));
    }

    //allows to reuse this instance (and get rid of boxing)
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public UnsafeReader Reset(byte* ptr, int len)
    {
      myInitialPtr = ptr;
      myPtr = ptr;
      myMaxlen = len;
      return this;
    }

    public int Position => (int) (myPtr - myInitialPtr);

    public void Skip(int bytes)
    {
      AssertLength(bytes);
      myPtr += bytes;
    }

    [Conditional("JET_MODE_ASSERT")]
    // ReSharper disable once ParameterOnlyUsedForPreconditionCheck.Local
    private void AssertLength(int size)
    {
      var alreadyRead = (int)(myPtr - myInitialPtr);
      Assertion.Assert(alreadyRead + size <= myMaxlen, "Can't read from unsafe reader: alreadyRead={0} size={1} maxlen={2}. " +
                                                 "Usually this happens when you change serialization format and forget to clear previous entries from disk. " +
                                                 "For example, you forgot to advance persistent caches version - do this in 'SolutionCaches' and 'ShellCaches' classes)."
                                                 , alreadyRead, size, myMaxlen);
    }



    #region Primitive readers

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public byte* ReadRaw(int count)
    {
      AssertLength(count);
      var res = myPtr;
      myPtr += count;
      return res;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public bool ReadBoolean()
    {
      AssertLength(sizeof(byte));

      return *(myPtr++) != 0;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public byte ReadByte()
    {
      AssertLength(sizeof(byte));

      return *(myPtr++);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public Guid ReadGuid()
    {
      var array = ReadArray(reader => reader.ReadByte());
      return new Guid(array);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public char ReadChar()
    {
      AssertLength(sizeof(char));

      var x = (char*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public decimal ReadDecimal()
    {
      AssertLength(sizeof(decimal));

      var x = (decimal*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public double ReadDouble()
    {
      AssertLength(sizeof(double));

      var x = (double*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public float ReadFloat()
    {
      AssertLength(sizeof(float));

      var x = (float*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public Int16 ReadInt16()
    {
      AssertLength(sizeof(Int16));

      var x = (Int16*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public Int16 ReadShort() //alias
    {
      return ReadInt16();
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public Int32 ReadInt32()
    {
      AssertLength(sizeof(Int32));

      var x = (Int32*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }
    
    public static UInt16 ReadUInt16FromBytes(byte[] bytes)
    {
      fixed (byte* bb = bytes)
        return *(UInt16*) bb;
    }

    public static Int32 ReadInt32FromBytes(byte[] bytes, int offset = 0)
    {
      fixed (byte* bb = bytes)
        return *(Int32*) (bb + offset);
    }
    
    public static Int64 ReadInt64FromBytes(byte[] bytes, int offset = 0)
    {
      fixed (byte* bb = bytes)
        return *(Int64*) (bb + offset);
    }
    
    public static UInt64 ReadUInt64FromBytes(byte[] bytes)
    {
      fixed (byte* bb = bytes)
        return *(UInt64*) bb;
    }
    

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public Int64 ReadInt64()
    {
      AssertLength(sizeof(Int64));

      var x = (Int64*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public int ReadInt()
    {
      return ReadInt32();
    }


    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public long ReadLong()
    {
      return ReadInt64();
    }


    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public UInt16 ReadUInt16()
    {
      AssertLength(sizeof(UInt16));

      var x = (UInt16*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public UInt32 ReadUInt32()
    {
      AssertLength(sizeof(UInt32));

      var x = (UInt32*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public UInt64 ReadUInt64()
    {
      AssertLength(sizeof(UInt64));

      var x = (UInt64*)myPtr;
      myPtr = (byte*)(x + 1);
      return *x;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public DateTime ReadDateTime()
    {
      return new DateTime(ReadLong(), DateTimeKind.Utc);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public Uri ReadUri()
    {
      return new Uri(Uri.UnescapeDataString(ReadString()), UriKind.RelativeOrAbsolute);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public string? ReadString()
    {
      int len = ReadInt32();

      if (len < 0) return null;
      if (len == 0) return string.Empty;      
      
      var raw = (char*) ReadRaw(len * sizeof(char));
      if (raw == (char*)0)
        throw new InvalidOperationException($"Bad memory (null) after reading string of size {len:N}");
        
      var res = new string(raw, 0, len);
      return res;
    }
    #endregion

    #region Intern
    public interface IRawStringIntern
    {
      string Intern(RawString raw);
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct RawString
    {
      public int Length;
      public char* Data;

      public string GetString()
      {
        return new string(Data, 0, Length);
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public string? ReadStringInterned(IRawStringIntern intern)
    {
      int len = ReadInt32();

      if (len < 0) return null;
      if (len == 0) return string.Empty;
      
      var raw = (char*) ReadRaw(len * sizeof(char));
      if (raw == (char*)0)
        throw new InvalidOperationException($"Bad memory (null) after reading string of size {len:N}");

      var res = intern.Intern(new RawString {Length = len, Data = raw});
      return res;
    }
    #endregion


    #region Delegates
    public delegate T ReadDelegate<out T>(UnsafeReader reader);
    public static readonly ReadDelegate<bool> BoolDelegate = reader => reader.ReadBoolean();
    public static readonly ReadDelegate<bool> BooleanDelegate = reader => reader.ReadBoolean(); //alias
    public static readonly ReadDelegate<byte> ByteDelegate = reader => reader.ReadByte();
    public static readonly ReadDelegate<Guid> GuidDelegate = reader => reader.ReadGuid();
    public static readonly ReadDelegate<char> CharDelegate = reader => reader.ReadChar();
    public static readonly ReadDelegate<decimal> DecimalDelegate = reader => reader.ReadDecimal();
    public static readonly ReadDelegate<double> DoubleDelegate = reader => reader.ReadDouble();
    public static readonly ReadDelegate<float> FloatDelegate = reader => reader.ReadFloat();
    public static readonly ReadDelegate<Int16> Int16Delegate = reader => reader.ReadInt16();
    public static readonly ReadDelegate<short> ShortDelegate = reader => reader.ReadInt16(); //alias
    public static readonly ReadDelegate<Int32> Int32Delegate = reader => reader.ReadInt32();
    public static readonly ReadDelegate<int> IntDelegate = reader => reader.ReadInt32(); //alias
    public static readonly ReadDelegate<Int64> Int64Delegate = reader => reader.ReadInt64();
    public static readonly ReadDelegate<long> LongDelegate = reader => reader.ReadInt64(); //alias
    public static readonly ReadDelegate<UInt16> UInt16Delegate = reader => reader.ReadUInt16();
    public static readonly ReadDelegate<UInt32> UInt32Delegate = reader => reader.ReadUInt32();
    public static readonly ReadDelegate<UInt64> UInt64Delegate = reader => reader.ReadUInt64();
    public static readonly ReadDelegate<DateTime> DateTimeDelegate = reader => reader.ReadDateTime();
    public static readonly ReadDelegate<Uri> UriDelegate = reader => reader.ReadUri();
    public static readonly ReadDelegate<string?> StringDelegate = reader => reader.ReadString();
    public static readonly ReadDelegate<byte[]?> ByteArrayDelegate = reader => reader.ReadByteArray();
    public static readonly ReadDelegate<bool[]?> BoolArrayDelegate = reader => reader.ReadArray(BooleanDelegate);
    public static readonly ReadDelegate<int[]?> IntArrayDelegate = reader => reader.ReadIntArray();
    public static readonly ReadDelegate<string?[]?> StringArrayDelegate = reader => reader.ReadArray(StringDelegate);

    #endregion


    #region Collection readers

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public T?[]? ReadArray<T>(ReadDelegate<T?> readDelegate)
    {
      int len = ReadInt32();
      if (len < 0) return null;
      if (len == 0) return EmptyArray<T>.Instance;

      var res = new T?[len];
      for (int i = 0; i < len; i++) res[i] = readDelegate(this);
      return res;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public int[]? ReadIntArray()
    {
      int len = ReadInt32();
      if (len < 0) return null;
      if (len == 0) return EmptyArray<int>.Instance;

      var res = new int[len];
      fixed (int* mem = res)
      {
        var size = len * sizeof(int);
        Memory.CopyMemory(ReadRaw(size), (byte*)mem, size);
      }
      return res;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public byte[]? ReadByteArray()
    {
      int len = ReadInt32();
      if (len < 0) return null;
      if (len == 0) return EmptyArray<byte>.Instance;

      var res = new byte[len];
      fixed (byte* mem = res)
      {
        Memory.CopyMemory(ReadRaw(len), mem, len);
      }
      return res;
    }


    /// <summary>
    /// Non optimal collection serialization. One can serialize internal structure (eg. array) instead.
    /// </summary>
    /// <typeparam name="T"></typeparam>
    /// <typeparam name="TCol"></typeparam>
    /// <param name="readDelegate"></param>
    /// <param name="constructor"></param>
    /// <returns></returns>
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public TCol? ReadCollection<T, TCol>(ReadDelegate<T> readDelegate, Func<int, TCol> constructor) where TCol : ICollection<T>
    {
      int count = ReadInt32();
      if (count < 0) return default(TCol);

      TCol col = constructor(count);
      for (int i = 0; i < count; i++)
      {
        col.Add(readDelegate(this));
      }
      return col;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public TDict? ReadDictionary<TK, TV, TDict>(ReadDelegate<TK> readKeyDelegate, ReadDelegate<TV> readValueDelegate,
      Func<int, TDict> constructor) where TDict : IDictionary<TK, TV>
    {
      int count = ReadInt32();
      if (count < 0) return default(TDict);

      TDict dict = constructor(count);
      for (int i = 0; i < count; i++)
      {
        dict[readKeyDelegate(this)] = readValueDelegate(this);
      }
      return dict;
    }

    #endregion

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public bool ReadNullness()
    {
      return ReadBoolean();
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public bool ReadBool()
    {
      return ReadBoolean();
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public byte ReadUByte()
    {
      return ReadByte();
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public ushort ReadUShort()
    {
      return ReadUInt16();
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public uint ReadUInt()
    {
      return ReadUInt32();
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public ulong ReadULong()
    {
      return ReadUInt64();
    }
  }
}