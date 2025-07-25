using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Serialization;
using NUnit.Framework;
#if NET472
using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
using JetBrains.Diagnostics;
#endif

namespace Test.Lifetimes.Serialization
{
  public unsafe class UnsafeMarshallersTest : LifetimesTestBase
  {
    [Test]
    public void Test1()
    {
      UnsafeReader reader;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        cookie.Writer.WriteBoolean(false);
        cookie.Writer.WriteBoolean(true);
        cookie.Writer.WriteByte(0);
        cookie.Writer.WriteByte(10);
        cookie.Writer.WriteChar('y');
        cookie.Writer.WriteChar('й');
        cookie.Writer.WriteDecimal(1234.5678m);
        cookie.Writer.WriteDouble(1234.5678d);
        cookie.Writer.WriteInt16(1000);
        cookie.Writer.WriteInt32(1001);
        cookie.Writer.WriteInt64(-1002);

        cookie.Writer.WriteString(null);
        cookie.Writer.WriteString("");
        cookie.Writer.WriteString("abcd = yй");

        cookie.Writer.WriteArray((int[]) (null));
        cookie.Writer.WriteArray(new int[0]);
        cookie.Writer.WriteArray(new[] {1, 2, 3});

        cookie.Writer.WriteCollection(UnsafeWriter.StringDelegate, (string[])null);
        cookie.Writer.WriteCollection(UnsafeWriter.StringDelegate, new string[0]);
        cookie.Writer.WriteCollection(UnsafeWriter.StringDelegate, new[] { "a", "b", "c" });

        cookie.Writer.WriteCollection(UnsafeWriter.StringDelegate, (List<string>)null);
        cookie.Writer.WriteCollection(UnsafeWriter.StringDelegate, new List<string>());
        cookie.Writer.WriteCollection(UnsafeWriter.StringDelegate, new List<string> { "d", "e" });

        reader = UnsafeReader.CreateReader(cookie.Data, cookie.Count);
      }

      Assert.False(reader.ReadBoolean());
      Assert.True(reader.ReadBoolean());
      Assert.AreEqual(0, reader.ReadByte());
      Assert.AreEqual(10, reader.ReadByte());
      Assert.AreEqual('y', reader.ReadChar());
      Assert.AreEqual('й', reader.ReadChar());
      Assert.AreEqual(1234.5678m, reader.ReadDecimal());
      Assert.AreEqual(1234.5678d, reader.ReadDouble(), 1e-6);
      Assert.AreEqual(1000, reader.ReadInt16());
      Assert.AreEqual(1001, reader.ReadInt32());
      Assert.AreEqual(-1002, reader.ReadInt64());

      Assert.Null(reader.ReadString());
      Assert.AreEqual("", reader.ReadString());
      Assert.AreEqual("abcd = yй", reader.ReadString());

      Assert.Null(reader.ReadIntArray());
      Assert.AreEqual(new int[0], reader.ReadIntArray());
      Assert.AreEqual(new[] {1, 2, 3}, reader.ReadIntArray());

      Assert.Null(reader.ReadArray(UnsafeReader.StringDelegate));
      Assert.AreEqual(new string[0], reader.ReadArray(UnsafeReader.StringDelegate));
      Assert.AreEqual(new[] {"a", "b", "c"}, reader.ReadArray(UnsafeReader.StringDelegate));

      Assert.Null(reader.ReadCollection(UnsafeReader.StringDelegate, n => new List<string>(n)));
      CollectionAssert.AreEqual(new List<string>(), reader.ReadCollection(UnsafeReader.StringDelegate, n => new List<string>(n)));
      CollectionAssert.AreEqual(new List<string> {"d", "e"}, reader.ReadCollection(UnsafeReader.StringDelegate, n => new List<string>(n)));
    }

    [Test]
    [TestCaseSource(nameof(GenerateSamplesForUtf8Tests))]
    public void TestUtf8Encoding([CanBeNull] string value)
    {
      if (value == null) return;

      var encoding = Encoding.UTF8;

      byte[] bytes;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        var maxByteCount = encoding.GetMaxByteCount(value.Length);
        var bookmark = cookie.Writer.Alloc(maxByteCount + sizeof(int));

        int bytesWritten;
        fixed (char* sourcePtr = value)
        {
          bytesWritten = encoding.GetBytes(
            sourcePtr, charCount: value.Length, bytes: bookmark.Data + sizeof(int), maxByteCount);

          *((int*)bookmark.Data) = bytesWritten;

          bytesWritten += sizeof(int);
          bookmark.FinishRawWrite(bytesWritten);
        }

        cookie.Writer.WriteUInt32(0xDEADBEEF);
        bytes = cookie.CloneData();

        Assert.AreEqual(bytes.Length, bytesWritten + sizeof(uint));
      }

      fixed (byte* ptr = bytes)
      {
        var reader = UnsafeReader.CreateReader(ptr, bytes.Length);

        var bytesCount = reader.ReadInt32();

#if NET35
        var start = reader.ReadRaw(bytesCount);
        var buffer = new byte[bytesCount];
        System.Runtime.InteropServices.Marshal.Copy((IntPtr) start, buffer, 0, length: bytesCount);
        var value2 = encoding.GetString(buffer);
#else
        var startPtr = reader.ReadRaw(bytesCount);
        var value2 = encoding.GetString(startPtr, bytesCount);
#endif

        var marker = reader.ReadUInt32();
        Assert.AreEqual(marker, 0xDEADBEEF);
        Assert.AreEqual(value, value2);
      }
    }

    [Test]
    [TestCaseSource(nameof(GenerateSamplesForUtf8Tests))]
    public void TestUtf8Encoding2([CanBeNull] string value)
    {
      var encoding = Encoding.UTF8;

      byte[] bytes;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        cookie.Writer.WriteStringUTF8(value);
        cookie.Writer.WriteUInt32(0xDEADBEEF);
        bytes = cookie.CloneData();
      }

      fixed (byte* ptr = bytes)
      {
        var reader = UnsafeReader.CreateReader(ptr, bytes.Length);
        var value2 = reader.ReadStringUTF8();

        var marker = reader.ReadUInt32();
        Assert.AreEqual(marker, 0xDEADBEEF);
        Assert.AreEqual(value, value2);
      }
    }

    [ItemCanBeNull]
    private static string[] GenerateSamplesForUtf8Tests()
    {
      return new[]
      {
        null,
        "",
        " ",
        "x",
        "xx",
        "abc",
        "abc_def",
        "привет",
        "one два three",
        "abra_кадабра",
        new string('a', 100),
        new string('щ', 100),
        new string('b', 200),
        new string('г', 200),
        new string('c', 10000),
        new string('ю', 10000),
      };
    }

    [Test]
    public void TestLargeAllocations()
    {
      TestWithTimeout(() =>
      {
        const int moreThanGb = (1 << 30) | 3;
        using var cookie = UnsafeWriter.NewThreadLocalWriter();
        _ = cookie.Writer.Alloc(moreThanGb);
      });
    }

    private static void TestWithTimeout(Action action)
    {
      var thread = new Thread(() => { action(); })
      {
        IsBackground = true
      };

      thread.Start();
      var timeout = TimeSpan.FromSeconds(15);
      var isCompleted = thread.Join(timeout);
      Assert.IsTrue(isCompleted, "Action didn't complete in specified amount of time.");
    }

#if NET472
    private UnsafeWriter.Cookie myCookie;
    private UnsafeReader myReader;

    [Test, Explicit("Check performance implication of asserts in UnsafeReader")]
    public void Bdn()
    {
      // Some results from my machine
      //
      // on x64
      //   BenchmarkDotNet=v0.12.1, OS=Windows 10.0.19044
      //   AMD Ryzen 7 PRO 5850U with Radeon Graphics, 1 CPU, 16 logical and 8 physical cores
      //     [Host]     : .NET Framework 4.8 (4.8.4515.0), X64 RyuJIT
      //     DefaultJob : .NET Framework 4.8 (4.8.4515.0), X64 RyuJIT
      //
      // Base: asserts are deleted from the source code.
      // | Method |     Mean |    Error |   StdDev |
      // |------- |---------:|---------:|---------:|
      // |      M | 12.52 ns | 0.127 ns | 0.118 ns |
      //
      // Mode.Assertion disabled
      // | Method |     Mean |    Error |   StdDev |
      // |------- |---------:|---------:|---------:|
      // |      M | 13.56 ns | 0.261 ns | 0.231 ns |
      //
      // Mode.Assertion enabled
      //   | Method |     Mean |    Error |   StdDev |
      //   |------- |---------:|---------:|---------:|
      //   |      M | 31.96 ns | 0.156 ns | 0.130 ns |
      //
      // on x86
      //   BenchmarkDotNet=v0.12.1, OS=Windows 10.0.19044
      //   AMD Ryzen 7 PRO 5850U with Radeon Graphics, 1 CPU, 16 logical and 8 physical cores
      //     [Host]     : .NET Framework 4.8 (4.8.4515.0), X86 LegacyJIT
      //     DefaultJob : .NET Framework 4.8 (4.8.4515.0), X86 LegacyJIT
      //
      // Base: asserts are deleted from the source code.
      // | Method |     Mean |    Error |   StdDev |
      // |------- |---------:|---------:|---------:|
      // |      M | 29.00 ns | 0.054 ns | 0.050 ns |
      //
      // Mode.Assertion disabled
      // | Method |     Mean |    Error |   StdDev |
      // |------- |---------:|---------:|---------:|
      // |      M | 29.90 ns | 0.109 ns | 0.097 ns |
      //
      // Mode.Assertion enabled
      // | Method |     Mean |    Error |   StdDev |
      // |------- |---------:|---------:|---------:|
      // |      M | 44.71 ns | 0.033 ns | 0.028 ns |
      BenchmarkRunner.Run<UnsafeMarshallersTest>();
    }

    [GlobalSetup]
    public void Setup()
    {
      // comment to disable asserts
      if (!ModeInitializer.Init(true))
        throw new Exception($"Assertion mode cannot be initialized. (default value was used: {ModeInitializer.GetIsAssertionUndefined()})");

      myCookie = UnsafeWriter.NewThreadLocalWriter();
      myCookie.Writer.WriteBoolean(false);
      myCookie.Writer.WriteBoolean(true);
      myCookie.Writer.WriteByte((byte)0);
      myCookie.Writer.WriteByte((byte)10);
      myCookie.Writer.WriteChar('y');
      myCookie.Writer.WriteChar('й');
      myCookie.Writer.WriteDecimal(1234.5678m);
      myCookie.Writer.WriteDouble(1234.5678d);
      myCookie.Writer.WriteInt16((short)1000);
      myCookie.Writer.WriteInt32((int)1001);
      myCookie.Writer.WriteInt64((long)-1002);
      myCookie.Writer.WriteString("(long)-1002");
      myReader = UnsafeReader.CreateReader(myCookie.Data, myCookie.Count);
    }

    [GlobalCleanup]
    public void Teardown()
    {
      myCookie.Dispose();
    }

    [Benchmark]
    public void M()
    {
      myReader.Reset(myCookie.Data, myCookie.Count);
      var v0 = myReader.ReadBoolean();
      var v1 = myReader.ReadBoolean();
      var v2 = myReader.ReadByte();
      var v3 = myReader.ReadByte();
      var v4 = myReader.ReadChar();
      var v5 = myReader.ReadChar();
      var v6 = myReader.ReadDecimal();
      var v7 = myReader.ReadDouble();
      var v8 = myReader.ReadInt16();
      var v9 = myReader.ReadInt32();
      var v10 = myReader.ReadInt64();
      var v11 = myReader.ReadString();
    }
#endif
  }
}