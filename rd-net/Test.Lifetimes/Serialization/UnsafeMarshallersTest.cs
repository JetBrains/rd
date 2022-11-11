using System.Collections.Generic;
using JetBrains.Diagnostics;
#if NET461
using System;
using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
#endif
using JetBrains.Serialization;
using NUnit.Framework;

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
        cookie.Writer.Write(false);
        cookie.Writer.Write(true);
        cookie.Writer.Write((byte) 0);
        cookie.Writer.Write((byte) 10);
        cookie.Writer.Write('y');
        cookie.Writer.Write('й');
        cookie.Writer.Write(1234.5678m);
        cookie.Writer.Write(1234.5678d);
        cookie.Writer.Write((short) 1000);
        cookie.Writer.Write((int) 1001);
        cookie.Writer.Write((long) -1002);


        cookie.Writer.Write((string) null);
        cookie.Writer.Write("");
        cookie.Writer.Write("abcd = yй");

        cookie.Writer.Write((int[]) (null));
        cookie.Writer.Write(new int[0]);
        cookie.Writer.Write(new[] {1, 2, 3});

        cookie.Writer.Write(UnsafeWriter.StringDelegate, (string[]) null);
        cookie.Writer.Write(UnsafeWriter.StringDelegate, new string[0]);
        cookie.Writer.Write(UnsafeWriter.StringDelegate, new[] {"a", "b", "c"});

        cookie.Writer.Write(UnsafeWriter.StringDelegate, (List<string>) null);
        cookie.Writer.Write(UnsafeWriter.StringDelegate, new List<string>());
        cookie.Writer.Write(UnsafeWriter.StringDelegate, new List<string> {"d", "e"});

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

#if NET461
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
      myCookie.Writer.Write(false);
      myCookie.Writer.Write(true);
      myCookie.Writer.Write((byte)0);
      myCookie.Writer.Write((byte)10);
      myCookie.Writer.Write('y');
      myCookie.Writer.Write('й');
      myCookie.Writer.Write(1234.5678m);
      myCookie.Writer.Write(1234.5678d);
      myCookie.Writer.Write((short)1000);
      myCookie.Writer.Write((int)1001);
      myCookie.Writer.Write((long)-1002);
      myCookie.Writer.Write("(long)-1002");
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