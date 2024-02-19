using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Util;
using JetBrains.Util.Internal;
// ReSharper disable BuiltInTypeReferenceStyle
// ReSharper disable ArrangeRedundantParentheses

namespace JetBrains.Serialization
{
  /// <summary>
  /// Effective serialization tool. <see cref="UnsafeWriter"/> is thread-bound automatically expandable and shrinkable
  /// byte buffer (with initial size of 1Mb). Entry point is <see cref="Cookie"/> obtained by  <see cref="NewThreadLocalWriter"/>.
  /// It is <see cref="IDisposable"/> so must be used only with (possibly nested) <c>using</c> in stack-like way.
  /// <see cref="Cookie"/> contains start position and length of currently serialized data (start + len = position), so when disposed it reverts writer
  /// position to the cookie's start position.
  ///
  ///
  /// <seealso cref="UnsafeReader"/>
  /// </summary>
  [PublicAPI]
  public sealed unsafe class UnsafeWriter
  {
    private readonly int myInitialAllocSize;

    [PublicAPI]
    public readonly struct Cookie : IDisposable
    {
      private readonly UnsafeWriter myWriter;
      private readonly int myStart;

      public Cookie(UnsafeWriter writer)
      {
        myWriter = writer;
        myWriter.Initialize();
        myStart = myWriter.Count;
      }

      public UnsafeWriter Writer
      {
        [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
        get => myWriter;
      }

      public byte* Data
      {
        [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
        get => myWriter.Data + myStart;
      }

      public int Count
      {
        [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
        get => myWriter.Count - myStart;
      }

      public byte[] CloneData()
      {
        var res = new byte[Count];
        Marshal.Copy((IntPtr)Data, res, 0, Count);
        return res;
      }

      public void CopyTo(byte[] dst, [Optional] int dstOffset, [Optional] int? count)
      {
        CopyTo(dst, dstOffset, count ?? Count);
      }

      public void CopyTo(byte[] dst, int dstOffset, int count)
      {
        CopyTo(dst, 0, dstOffset, count);
      }

      public void CopyTo(byte[] dst, int srcOffset, int dstOffset, int count)
      {
        Marshal.Copy((IntPtr)(Data + srcOffset), dst, dstOffset, count);
      }

      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      public void Dispose()
      {
        // default struct constructor was used for unknown reason
        if (myWriter == null)
          return;

        myWriter.Deinitialize(myStart);
      }
    }

    /// <summary>
    /// A bookmark to the offset in the memory block of the <see cref="UnsafeWriter"/>. Can be used for saving address
    /// in UnsafeWriter for future use. Basically every method in <see cref="UnsafeWriter"/> can cause reallocation of
    /// data. It is important to avoid storing pointers obtained from UnsafeWriter between these calls.
    /// </summary>
    [PublicAPI]
    public readonly struct Bookmark
    {
      private readonly UnsafeWriter myWriter;
      private readonly int myStart;

      public Bookmark(UnsafeWriter writer)
      {
        myWriter = writer;
        myStart = myWriter.Count;
      }

      public byte* Data
      {
        [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
        get => myWriter.Data + myStart;
      }

      /// <summary>
      /// Writes `<see cref="Count"/><c> - sizeof(int)</c>` into the <see cref="Data"/> pointer. Cookie must be prepared by invoking `<see cref="UnsafeWriter.Write(int)"/><c>(0)</c>` as first cookie call.
      /// </summary>
      public void WriteIntLength()
      {
        *((int*)Data) = myWriter.Count - myStart - sizeof(int);
      }

      public void WriteIntLength(int length)
      {
        *((int*)Data) = length;
      }

      public void FinishRawWrite(int bytesWritten)
      {
        if (bytesWritten <= 0)
          throw new ArgumentOutOfRangeException(nameof(bytesWritten));

        var finalPtr = myWriter.myStartPtr + myStart + bytesWritten;
        if (myWriter.myPtr <= finalPtr)
          throw new ArgumentOutOfRangeException(nameof(bytesWritten), "Overflow, allocation is smaller then bytes written");

        myWriter.myPtr = finalPtr;
        myWriter.myCount = myStart + bytesWritten;
      }

      public void Reset()
      {
        FinishRawWrite(myStart - myWriter.Count);
      }
    }

    private const string LogCategory = "UnsafeWriter";

    /// <summary>
    /// Whether <see cref="UnsafeWriter"/> can be cached for the specific thread.
    /// </summary>
    [Obsolete("Don't use")]
    public static bool AllowUnsafeWriterCaching
    {
      get => true;
      // ReSharper disable once ValueParameterNotUsed
      set { }
    }

    /// <summary>
    /// Cached <see cref="UnsafeWriter"/> for reuse
    /// </summary>
    [ThreadStatic] private static UnsafeWriter? ourWriter;

    public static Cookie NewThreadLocalWriter()
    {
      if (ourWriter != null)
        return new Cookie(ourWriter);

      ourWriter = new UnsafeWriter();
      var writer = new Cookie(ourWriter);
      return writer;
    }

    [Obsolete("Use NewThreadLocalWriter()")]
    public static Cookie NewThreadLocalWriterNoCaching()
    {
      return NewThreadLocalWriterImpl(false);
    }

    [Obsolete("Use NewThreadLocalWriter()")]
    private static Cookie NewThreadLocalWriterImpl(bool allowCaching)
    {
      return NewThreadLocalWriter();
    }

    private byte* myStartPtr;
    private int myCurrentAllocSize;

    private byte* myPtr;
    private int myCount;

    /// <summary>
    /// Indicates whether the UnsafeWriter should try to cleanup used memory in <see cref="NativeMemoryPool"/> 
    /// </summary>
    internal int ReleaseResources;

    private UnsafeWriter()
    {
      myInitialAllocSize = NativeMemoryPool.AllocSize;
      myMemory = null;
    }

    /// <summary>
    /// Stores the last used memory holder. Be aware that this holder is not reserved for current unsafe writer only and
    /// in some circumstances may be used and reserved by other consumer (when it's free)
    /// </summary>
    private NativeMemoryPool.ThreadMemoryHolder? myMemory;
    private int myRecursionLevel;

    /// <summary>
    /// Creates a new UnsafeWriter
    /// </summary>
    private void Initialize()
    {
      if (myRecursionLevel++ == 0)
      {
        if (myMemory != null && myMemory.TryReserve())
        {
        }
        else
        {
          var cookie = NativeMemoryPool.ReserveMiss();
          if (Mode.IsAssertion) Assertion.Assert(cookie.IsValid);

          myMemory = cookie.myHolder;
          if (cookie.CausedAllocation)
          {
            ReleaseResources++;
          }
        }

        myCurrentAllocSize = NativeMemoryPool.AllocSize;
        myStartPtr = myPtr = (byte*) myMemory.Data;
        myCount = 0;
      }
    }

    private void Deinitialize(int start)
    {
      if (--myRecursionLevel == 0)
      {
        myMemory!.Free();
        myCurrentAllocSize = 0;
        // Setting current alloc size to zero have a special semantic of making current UnsafeWriter invalid.
        // There is no need to additionally resetting these pointers as write will check available memory and raise an
        // exception for this special case
        // myStartPtr = (byte*) 0;
        // myPtr = (byte*) 0;
      }
      else
      {
        Reset(start);
      }
    }

    ~UnsafeWriter()
    {
      for (var index = 0; index < ReleaseResources; index++)
      {
        NativeMemoryPool.TryFreeMemory();
      }
    }

    private void Reset(int start = 0)
    {
      myPtr = myStartPtr + start;
      myCount = start;
      if (myCurrentAllocSize > myInitialAllocSize)
      {
        if (start == 0) Realloc(myCount);
        else LogLog.Verbose(LogCategory, "Can't realloc, start={0}", start);
      }
    }

    private int Count
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get => myCount;
    }

    private byte* Data
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get => myStartPtr;
    }

    public byte* Ptr
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get => myPtr;
    }

    private void Prepare(int nbytes)
    {
      var newCount = myCount + nbytes;
      if (newCount <= myCurrentAllocSize)
      {
        myCount = newCount;
        return;
      }

      Realloc(newCount);
    }

    private void Realloc(int newCount)
    {
      if (myCurrentAllocSize == 0)
        throw new InvalidOperationException("UnsafeWriter is uninitialized, unable to realloc");

      var reallocSize = myInitialAllocSize;
      while (newCount > reallocSize)
      {
        reallocSize <<= 1;
      }

      try
      {
        LogLog.Verbose(LogCategory, "Realloc UnsafeWriter, current: {0:N0} bytes, new: {1:N0}", myCurrentAllocSize, reallocSize);
        if (myStartPtr != null) //already terminated
        {
          Assertion.AssertNotNull(myMemory);
          myStartPtr = (byte*) myMemory.Realloc(reallocSize);
          myPtr = myStartPtr + myCount;
          myCurrentAllocSize = reallocSize;
          myCount = newCount;
        }
      }
      catch (Exception exception)
      {
        throw new ArgumentException(
          $"Can't allocate more memory for chunk: {reallocSize} bytes, currentlyAllocated={myCurrentAllocSize}, count={myCount}", exception);
      }
    }

    #region Primitive writers

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteBool() instead",
      ReplaceTemplate = "$qualifier$.WriteBoolean($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(bool value) => WriteBoolean(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteByte() instead",
      ReplaceTemplate = "$qualifier$.WriteByte($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(byte value) => WriteByte(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteGuid() instead",
      ReplaceTemplate = "$qualifier$.WriteGuid($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Guid value) => WriteGuid(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteChar() instead",
      ReplaceTemplate = "$qualifier$.WriteChar($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(char value) => WriteChar(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteDecimal() instead",
      ReplaceTemplate = "$qualifier$.WriteDecimal($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(decimal value) => WriteDecimal(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteDouble() instead",
      ReplaceTemplate = "$qualifier$.WriteDouble($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(double value) => WriteDouble(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteFloat() instead",
      ReplaceTemplate = "$qualifier$.WriteFloat($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(float value) => WriteFloat(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteInt16() instead",
      ReplaceTemplate = "$qualifier$.WriteInt16($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Int16 value) => WriteInt16(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteInt32() instead",
      ReplaceTemplate = "$qualifier$.WriteInt32($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Int32 value) => WriteInt32(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteInt64() instead",
      ReplaceTemplate = "$qualifier$.WriteInt64($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Int64 value) => WriteInt64(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteUInt16() instead",
      ReplaceTemplate = "$qualifier$.WriteUInt16($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(UInt16 value) => WriteUInt16(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteUInt32() instead",
      ReplaceTemplate = "$qualifier$.WriteUInt32($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(UInt32 value) => WriteUInt32(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteUInt64() instead",
      ReplaceTemplate = "$qualifier$.WriteUInt64($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(UInt64 value) => WriteUInt64(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteDateTime() instead",
      ReplaceTemplate = "$qualifier$.WriteDateTime($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(DateTime value) => WriteDateTime(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteTimeSpan() instead",
      ReplaceTemplate = "$qualifier$.WriteTimeSpan($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(TimeSpan value) => WriteTimeSpan(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteUri() instead",
      ReplaceTemplate = "$qualifier$.WriteUri($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Uri value) => WriteUri(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteString() instead",
      ReplaceTemplate = "$qualifier$.WriteString($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(string? value) => WriteString(value);

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteBoolean(bool value)
    {
      Prepare(sizeof(byte));
      *(myPtr++) = (byte)(value ? 1 : 0);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteByte(byte value)
    {
      Prepare(sizeof(byte));
      *(myPtr++) = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteSByte(sbyte value)
    {
      Prepare(sizeof(sbyte));
      *(myPtr++) = (byte) value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteGuid(Guid value)
    {
      Write<byte, byte[]>((writer, b) => writer.Write(b), value.ToByteArray());
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteChar(char value)
    {
      Prepare(sizeof(char));
      var x = (char*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteDecimal(decimal value)
    {
      Prepare(sizeof(decimal));
      var x = (decimal*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteDouble(double value)
    {
      Prepare(sizeof(double));
      var x = (double*)myPtr;
      myPtr = (byte*)(x + 1);
#if !NET35
      if (!RuntimeInfo.IsUnalignedAccessAllowed)
      {
        Buffer.MemoryCopy(&value, x, sizeof(double), sizeof(double));
      }
      else
#endif
      {
        *x = value;
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteFloat(float value)
    {
      Prepare(sizeof(float));
      var x = (float*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteInt16(Int16 value)
    {
      Prepare(sizeof(Int16));
      var x = (Int16*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteInt32(Int32 value)
    {
      Prepare(sizeof(Int32));
      var x = (Int32*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    public void Write7BitEncodedInt32(int value)
    {
      // write out an int 7 bits at a time
      var v = (uint)value;

      while (v >= 0b10000000)
      {
        WriteByte((byte)(v | 0b10000000));
        v >>= 7;
      }

      WriteByte((byte)v);
    }

    public void WriteOftenSmallPositiveInt32(int value)
    {
      if (value >= 0 & value < byte.MaxValue)
      {
        WriteByte((byte) value);
      }
      else
      {
        WriteByte(byte.MaxValue);
        WriteInt32(value);
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void WriteInt32ToBytes(Int32 value, byte[] data, int offset)
    {
      fixed (byte* bb = data)
        *(int*)(bb+offset) = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteInt64(Int64 value)
    {
      Prepare(sizeof(Int64));
      var x = (Int64*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [Obsolete("Use 'WriteUInt16' instead (correct casing)")]
    public void WriteUint16(UInt16 value) => WriteUInt16(value);

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteUInt16(UInt16 value)
    {
      Prepare(sizeof(UInt16));
      var x = (UInt16*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteUInt32(UInt32 value)
    {
      Prepare(sizeof(UInt32));
      var x = (UInt32*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [Obsolete("Use 'WriteUInt64' instead (correct casing)")]
    public void WriteUint64(UInt64 value) => WriteUInt64(value);

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteUInt64(UInt64 value)
    {
      Prepare(sizeof(UInt64));
      var x = (UInt64*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteDateTime(DateTime value)
    {
      if (Mode.IsAssertion) Assertion.Assert(value.Kind != DateTimeKind.Local, "Use UTC time");

      Write(value.Ticks);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteTimeSpan(TimeSpan value)
    {
      Write(value.Ticks);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteUri(Uri value)
    {
      Write(Uri.EscapeUriString(value.OriginalString));
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteString(string? value)
    {
      if (value == null) Write(-1);
      else
      {
        Write(value.Length);
        WriteStringContentInternal(this, value, 0, value.Length);
      }
    }

    /// <summary>
    /// Doesn't write length prefix, only string contents. If value == null, does nothing.
    /// </summary>
    /// <param name="value"></param>
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteStringContent(string? value)
    {
      if (value == null) return;
      WriteStringContentInternal(this, value, 0, value.Length);
    }

    /// <summary>
    /// Doesn't write length prefix, only string contents. If value == null, does nothing.
    /// </summary>
    /// <param name="value"></param>
    /// <param name="offset"></param>
    /// <param name="count"></param>
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteStringContent(string? value, int offset, int count)
    {
      if (value == null) return;
      if (offset < 0 || count < 0 || offset + count > value.Length)
        throw new ArgumentException($"string.length={value.Length}, offset={offset}, count={count}");

      WriteStringContentInternal(this, value, offset, count);
    }

    private static readonly bool ourOldMonoFlag = RuntimeInfo.CurrentMonoVersion != null
                                                  && !(RuntimeInfo.CurrentMonoVersion.Major >= 5
                                                       && RuntimeInfo.CurrentMonoVersion.Minor >= 8);

    /*
      It is special method to avoid crash on mono before 5.0
      Additional info details see on GitHub: https://github.com/mono/mono/pull/6020
      of bugzilla: https://bugzilla.xamarin.com/show_bug.cgi?id=60625
      It is shouldn't dropped while we support client mono version before 5.0
     */
    private static void WriteStringContentInternal(UnsafeWriter wrt, string value, int offset, int count)
    {
      if (ourOldMonoFlag)
      {
        WriteStringContentInternalBeforeMono5(wrt, value, offset, count);
      }
      else
      {
        WriteStringContentInternalAfterMono5(wrt, value, offset, count);
      }
    }

    // Mono 5.4 try to inline this method and crash.
    //[MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private static void WriteStringContentInternalAfterMono5(UnsafeWriter wrt, string value, int offset, int count)
    {
      fixed (char* c = value)
      {
        wrt.Write((byte*) (c + offset), count * sizeof(char));
      }
    }

    private static void WriteStringContentInternalBeforeMono5(UnsafeWriter wrt, string value, int offset, int count)
    {
      for (var i = offset; i < offset + count; i++)
      {
        wrt.Write(value[i]);
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(byte* ptr, int size)
    {
      Prepare(size);
      Memory.CopyMemory(ptr, myPtr, size);
      myPtr += size;
    }

    #endregion
    #region Delegates

    public delegate void WriteDelegate<in T>(UnsafeWriter writer, T value);

    public static readonly WriteDelegate<bool> BooleanDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<byte> ByteDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<Guid> GuidDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<char> CharDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<decimal> DecimalDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<double> DoubleDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<float> FloatDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<Int16> Int16Delegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<Int32> Int32Delegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<Int64> Int64Delegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<UInt16> UInt16Delegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<UInt32> UInt32Delegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<UInt64> UInt64Delegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<DateTime> DateTimeDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<Uri> UriDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<string> StringDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<byte[]> ByteArrayDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<int[]> IntArrayDelegate = (writer, x) => writer.Write(x);
    public static readonly WriteDelegate<string[]> StringArrayDelegate = (writer, x) => writer.Write(StringDelegate, x);

    #endregion
    #region Collection writers

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteArray() instead",
      ReplaceTemplate = "$qualifier$.WriteArray($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    public void Write(int[]? value) => WriteArray(value);

    [CodeTemplate(
      searchTemplate: "$member$($arg$)",
      Message = "HINT: Use WriteByteArray() instead",
      ReplaceTemplate = "$qualifier$.WriteByteArray($arg$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    public void Write(byte[]? value) => WriteByteArray(value);

    public void WriteArray(int[]? value)
    {
      if (value == null)
      {
        Write(-1);
      }
      else
      {
        Write(value.Length);
        fixed (int* c = value)
        {
          Write((byte*)c, value.Length * sizeof(int));
        }
      }
    }

    public void WriteByteArray(byte[]? value)
    {
      if (value == null)
      {
        Write(-1);
      }
      else
      {
        var size = value.Length;
        Write(size);
        Prepare(size);
        Marshal.Copy(value, 0, (IntPtr)myPtr, size); // Unlike MemoryUtil::CopyMemory, this is a CLR intrinsic call
        myPtr += size;
      }
    }

    public void WriteRaw(byte[] value)
    {
      if (value == null)
        throw new ArgumentNullException(nameof(value));

      var size = value.Length;
      Prepare(size);
      Marshal.Copy(value, 0, (IntPtr)myPtr, size); // Unlike MemoryUtil::CopyMemory, this is a CLR intrinsic call
      myPtr += size;
    }

    public void WriteRaw(byte[] value, int start, int length)
    {
      if (value == null)
        throw new ArgumentNullException(nameof(value));

      Prepare(length);
      Marshal.Copy(value, start, (IntPtr)myPtr, length); // Unlike MemoryUtil::CopyMemory, this is a CLR intrinsic call
      myPtr += length;
    }

    /// <summary>
    /// Creates <see cref="Bookmark"/> for the current <see cref="UnsafeWriter"/>'s position.
    /// </summary>
    [MustUseReturnValue]
    public Bookmark MakeBookmark()
    {
      return new Bookmark(this);
    }

    /// <summary>
    /// Correctly allocates the number of bytes as if they were written with any func, and advances the pointer past them.
    /// This is useful if you want to use buffer space for direct memory access.
    /// </summary>
    /// <remarks>
    /// Never save the value of <see cref="Ptr" /> before calling <see cref="Alloc" />! This method may cause a reallocation
    /// of data after which the saved pointer became invalid.
    /// </remarks>
    /// <returns><see cref="Bookmark"/> to the allocated buffer</returns>
    public Bookmark Alloc(int length)
    {
      var result = new Bookmark(this);
      Prepare((int)checked((uint)length));
      myPtr += length;
      return result;
    }

    [CodeTemplate(
      searchTemplate: "$member$($args$)",
      Message = "HINT: Use WriteCollection() instead",
      ReplaceTemplate = "$qualifier$.WriteCollection($args$)",
      SuppressionKey = "UnsafeWriter_ExplicitApi")]
    public void Write<T, TCollection>(WriteDelegate<T> writeDelegate, TCollection? value)
      where TCollection : ICollection<T>
      => WriteCollection(writeDelegate, value);

    /// <summary>
    /// Non optimal collection serialization. You can serialize internal structure (eg. array) instead.
    /// </summary>
    public void WriteCollection<T, TCollection>(WriteDelegate<T> writeDelegate, TCollection? value)
      where TCollection : ICollection<T>
    {
      if (value == null)
      {
        Write(-1);
      }
      else
      {
        Write(value.Count);
        foreach (var x in value)
        {
          writeDelegate(this, x);
        }
      }
    }

    public void Write<TK, TV, TDictionary>(WriteDelegate<TK> writeKeyDelegate, WriteDelegate<TV> writeValueDelegate, TDictionary? value)
      where TDictionary : IDictionary<TK, TV>
    {
      if (value == null)
      {
        Write(-1);
      }
      else
      {
        Write(value.Count);
        foreach (var kv in value)
        {
          writeKeyDelegate(this, kv.Key);
          writeValueDelegate(this, kv.Value);
        }
      }
    }

    #endregion

    [ContractAnnotation("null=>false")]
    public bool WriteNullness<T>([NotNullWhen(true)] T? value) where T : struct
    {
      var res = value != null;
      Write(res);
      return res;
    }

    [ContractAnnotation("null=>false")]
    public bool WriteNullness<T>([NotNullWhen(true)] T? value) where T : class
    {
      var res = value != null;
      Write(res);
      return res;
    }
  }
}