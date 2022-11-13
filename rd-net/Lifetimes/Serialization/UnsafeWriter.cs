using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Util;
using JetBrains.Util.Internal;

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
  public sealed unsafe class UnsafeWriter
  {
    private readonly int myInitialAllocSize;

    public readonly struct Cookie : IDisposable
    {
      private readonly UnsafeWriter myWriter;
      private readonly int myStart;
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
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
        *(int*) Data = myWriter.Count - myStart - sizeof(int);
      }

      public void WriteIntLength(int length)
      {
        *(int*) Data = length;
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
      set
      {
      }
    }

    /// <summary>
    /// Cached <see cref="UnsafeWriter"/> for reuse
    /// </summary>
    [ThreadStatic] private static UnsafeWriter? ourWriter;


    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static Cookie NewThreadLocalWriter()
    {
      if (ourWriter != null)
        return new Cookie(ourWriter);

      ourWriter = new UnsafeWriter();
      var writer = new Cookie(ourWriter);
      return writer;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    [Obsolete("Use NewThreadLocalWriter()")]
    public static Cookie NewThreadLocalWriterNoCaching()
    {
      return NewThreadLocalWriterImpl(false);
    }

    [Obsolete("Use NewThreadLocalWriter()")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
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
    private int myRecursionLevel = 0;

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
          Assertion.Assert(cookie.IsValid, "memoryCookie.IsValid");
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

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
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
      for (int i = 0; i < ReleaseResources; i++)
        NativeMemoryPool.TryFreeMemory();
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
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
      get
      {
        return myCount;
      }
    }

    private byte* Data
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get { return myStartPtr; }
    }

    public byte* Ptr
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get { return myPtr; }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
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
          Assertion.Assert(myMemory != null, "myMemory != null");
          myStartPtr = (byte*) myMemory.Realloc(reallocSize);
          myPtr = myStartPtr + myCount;
          myCurrentAllocSize = reallocSize;
          myCount = newCount;
        }
      }
      catch (Exception e)
      {
        throw new ArgumentException(
          string.Format("Can't allocate more memory for chunk: {0} bytes, currentlyAllocated={1}, count={2}", reallocSize,
            myCurrentAllocSize, myCount), e);
      }
    }


    #region Primitive writers
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(bool value)
    {
      Prepare(sizeof(byte));
      *(myPtr++) = (byte)(value ? 1 : 0);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(byte value)
    {
      Prepare(sizeof(byte));
      *(myPtr++) = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Guid value)
    {
      Write<byte, byte[]>((writer, b) => writer.Write(b), value.ToByteArray());
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(char value)
    {
      Prepare(sizeof(char));
      var x = (char*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(decimal value)
    {
      Prepare(sizeof(decimal));
      var x = (decimal*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(double value)
    {
      Prepare(sizeof(double));
      var x = (double*)myPtr;
      myPtr = (byte*)(x + 1);
#if !NET35
      if (!RuntimeInfo.IsUnalignedAccessAllowed)
        Buffer.MemoryCopy(&value, x, sizeof(double), sizeof(double));
      else
#endif
      {
        *x = value;
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(float value)
    {
      Prepare(sizeof(float));
      var x = (float*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Int16 value)
    {
      Prepare(sizeof(Int16));
      var x = (Int16*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Int32 value)
    {
      Prepare(sizeof(Int32));
      var x = (Int32*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void WriteInt32ToBytes(Int32 value, byte[] data, int offset)
    {
      fixed (byte* bb = data)
        *(int*)(bb+offset) = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Int64 value)
    {
      Prepare(sizeof(Int64));
      var x = (Int64*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(UInt16 value)
    {
      Prepare(sizeof(UInt16));
      var x = (UInt16*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(UInt32 value)
    {
      Prepare(sizeof(UInt32));
      var x = (UInt32*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(UInt64 value)
    {
      Prepare(sizeof(UInt64));
      var x = (UInt64*)myPtr;
      myPtr = (byte*)(x + 1);
      *x = value;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(DateTime value)
    {
      Assertion.Assert(value.Kind != DateTimeKind.Local, "Use UTC time");
      Write(value.Ticks);
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(TimeSpan value)
    {
      Write(value.Ticks);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(Uri value)
    {
      Write(Uri.EscapeUriString(value.OriginalString));
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(string? value)
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
      if (offset < 0 || count < 0 || offset + count > value.Length) throw new ArgumentException(string.Format("string.length={0}, offset={1}, count={2}", value.Length, offset, count));

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
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
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

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
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
    /*public void Write<T>(WriteDelegate<T> writeDelegate, T[] value)
    {
      if (value == null) Write(-1);
      else
      {
        Write(value.Length);
       foreach (var t in value)
        {
          writeDelegate(this, t);
        }
      }
    }*/

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(int[]? value)
    {
      if (value == null) Write(-1);
      else
      {
        Write(value.Length);
        fixed (int* c = value)
        {
          Write((byte*)c, value.Length * sizeof(int));
        }
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write(byte[]? value)
    {
      if(value == null)
        Write(-1);
      else
      {
        int size = value.Length;
        Write(size);
        Prepare(size);
        Marshal.Copy(value, 0, (IntPtr)myPtr, size); // Unlike MemoryUtil::CopyMemory, this is a CLR intrinsic call
        myPtr += size;
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteRaw(byte[] value)
    {
      if(value == null)
        throw new ArgumentNullException("value");
      int size = value.Length;
      Prepare(size);
      Marshal.Copy(value, 0, (IntPtr)myPtr, size); // Unlike MemoryUtil::CopyMemory, this is a CLR intrinsic call
      myPtr += size;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void WriteRaw(byte[] value, int start, int length)
    {
      if(value == null)
        throw new ArgumentNullException("value");
      Prepare(length);
      Marshal.Copy(value, start, (IntPtr)myPtr, length); // Unlike MemoryUtil::CopyMemory, this is a CLR intrinsic call
      myPtr += length;
    }

    /// <summary>
    /// Creates <see cref="Bookmark"/> for the current <see cref="UnsafeWriter"/>'s position.
    /// </summary>
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
    /// <returns><see cref="Bookmark"/> to the allocated buffer </returns>
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public Bookmark Alloc(int length)
    {
      var result = new Bookmark(this);
      Prepare((int)checked((uint)length));
      myPtr += length;
      return result;
    }

    /// <summary>
    /// Non optimal collection serialization. You can serialize internal structure (eg. array) instead.
    /// </summary>
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write<T, TCol>(WriteDelegate<T> writeDelegate, TCol? value) where TCol : ICollection<T>
    {
      if (value == null) Write(-1);
      else
      {
        Write(value.Count);
        foreach (var x in value)
        {
          writeDelegate(this, x);
        }
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void Write<TK, TV, TDict>(WriteDelegate<TK> writeKeyDelegate, WriteDelegate<TV> writeValueDelegate, TDict value) where TDict : IDictionary<TK, TV>
    {
      if (value == null) Write(-1);
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
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public bool WriteNullness<T>([NotNullWhen(true)] T? value) where T : struct
    {
      var res = value != null;
      Write(res);
      return res;
    }

    [ContractAnnotation("null=>false")]
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public bool WriteNullness<T>([NotNullWhen(true)] T? value) where T : class
    {
      var res = value != null;
      Write(res);
      return res;
    }

  }
}