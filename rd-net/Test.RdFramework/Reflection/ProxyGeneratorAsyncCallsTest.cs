using System;
using System.IO;
using System.Linq;
using System.Linq.Expressions;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using JetBrains.Threading;
using NUnit.Framework;

#if NET35

#endif

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ProxyGeneratorAsyncCallsTest : ProxyGeneratorTestBase
  {
    protected override bool IsAsync => true;

    [RdExt]
    internal class AsyncCallsTest : RdExtReflectionBindableBase, IAsyncCallsTest
    {
      public IViewableMap<short, IViewableSet<short>> SyncMoments { get; }

      public Task<string> GetStringAsync()
      {
        return Task.FromResult("result");
      }

      public Task RunSomething()
      {
        return Task.CompletedTask;
      }

      public Task<int> iSum(int a, int b) => Task.FromResult(a + b);
      public Task<uint> uiSum(uint a, uint b) => Task.FromResult(a + b);
      public Task<long> lSum(long a, long b) => Task.FromResult(a + b);
      public Task<ulong> ulSum(ulong a, ulong b) => Task.FromResult(a + b);
      public Task<short> sSum(short a, short b) => Task.FromResult(unchecked((short)(a + b)));
      public Task<ushort> usSum(ushort a, ushort b) => Task.FromResult(unchecked((ushort)(a + b)));
      public Task<byte> bSum(byte a, byte b) => Task.FromResult(unchecked((byte)(a + b)));
    }

    public class AsyncTestFixture<T>
    {
      public virtual int Rounds => 1;

      public virtual void Client(int round) { }
      public virtual void Server(int round) { }
    }

#if NET35
    private static TaskHack Task = new TaskHack();
#endif

    [RdRpc]
    public interface IAsyncCallsTest
    {
      IViewableMap<short, IViewableSet<short>> SyncMoments { get; }

      Task<string> GetStringAsync();
      Task RunSomething();

      Task<int> iSum(int a, int b);
      Task<ulong> ulSum(ulong a, ulong b);
      Task<short> sSum(short a, short b);
      Task<ushort> usSum(ushort a, ushort b);
      Task<byte> bSum(byte a, byte b);
      Task<uint> uiSum(uint a, uint b);
      Task<long> lSum(long a, long b);
    }

    [RdRpc]
    public interface IAsyncModelsTestDifferentName
    {
      Task<AColor> QueryColor();
      void SetPath(AsyncModelsTest.FileSystemPath animal);
    }

    [RdExt]
    public class AsyncModelsTest : RdExtReflectionBindableBase, IAsyncModelsTestDifferentName
    {
      public async Task<AColor> QueryColor()
      {
        return await Task.FromResult(new AColor(10, 10, 10));
      }

      public void SetPath(FileSystemPath animal)
      {
        Assert.True(!animal.Path.Any(char.IsUpper), "!animal.Path.Any(char.IsUpper)");
      }

      [RdScalar]
      public class FileSystemPath
      {
        private string myPath;

        public string Path => myPath;

        public FileSystemPath(string path)
        {
          myPath = path;
        }

        public static FileSystemPath Read(SerializationCtx ctx, UnsafeReader reader)
        {
          return new FileSystemPath(reader.ReadString());
        }

        public static void Write(SerializationCtx ctx, UnsafeWriter writer, FileSystemPath value)
        {
          writer.WriteString(value.myPath.ToLowerInvariant());
        }
      }
    }

    [RdScalar] // not required
    public class AColor
    {
      public AColor(int r, int g, int b)
      {
        R = r;
        G = g;
        B = b;
      }
      public int R;
      public int G;
      public int B;
    }

    [RdRpc]
    public interface ISyncCallsTest
    {
      RdList<string> History { get; }
      string Concat(string a, string b, string c);
    }

    [RdExt]
    public class SyncCallsTest : RdExtReflectionBindableBase, ISyncCallsTest
    {
      public RdList<string> History { get; }

      public string Concat(string a, string b, string c)
      {
        var result = string.Concat(a, b, c);
        History.Add(result);
        return result;
      }
    }

    [Test]
    public async Task TestAsync()
    {
      string result = null;
      await TestAsyncCalls(async model =>
      {
        result = await model.GetStringAsync();
      });

#if NET35
      // it seems like ExecuteSynchronously does not work in NET35
      SpinWaitEx.SpinUntil(TimeSpan.FromSeconds(1), () => result != null);
#endif
      Assert.AreEqual(result, "result");
    }

    [Test]
    public async Task TestAsyncVoid()
    {
      // todo: really check long running task result
      await TestAsyncCalls(async model => await model.RunSomething());
    }

    [Test]
    public async Task TestAsyncModels()
    {
      await TestTemplate<AsyncModelsTest, IAsyncModelsTestDifferentName>(async proxy =>
      {
        proxy.SetPath(new AsyncModelsTest.FileSystemPath("C:\\hello"));
        var queryColor = await proxy.QueryColor();
        Assert.AreEqual(queryColor.R, 10);
        Assert.AreEqual(queryColor.G, 10);
        Assert.AreEqual(queryColor.B, 10);
      });
    }

    [Test] public async Task TestAsyncSum1() => await TestAsyncCalls(async model => Assert.AreEqual(await model.iSum(100, -150), -50));
    [Test] public async Task TestAsyncSum2() => await TestAsyncCalls(async model => Assert.AreEqual(await model.uiSum(uint.MaxValue, 0), uint.MaxValue));
    [Test] public async Task TestAsyncSum3() => await TestAsyncCalls(async model => Assert.AreEqual(await model.sSum(100, -150), -50));
    [Test] public async Task TestAsyncSum4() => await TestAsyncCalls(async model => Assert.AreEqual(await model.usSum(ushort.MaxValue, 1), 0));
    [Test] public async Task TestAsyncSum5() => await TestAsyncCalls(async model => Assert.AreEqual(await model.lSum(long.MaxValue, 0), long.MaxValue));
    [Test] public async Task TestAsyncSum6() => await TestAsyncCalls(async model => Assert.AreEqual(await model.ulSum(ulong.MaxValue, 0), ulong.MaxValue));
    [Test] public async Task TestAsyncSum7() => await TestAsyncCalls(async model => Assert.AreEqual(await model.bSum(byte.MaxValue, 1), 0));

    [Test, Description("Sync call in and asynchonous enviroment")]
    public async Task TestSyncCall()
    {
      await TestSyncCalls(async m =>
      {
        CollectionAssert.IsEmpty(m.History);
        m.Concat("1", "2", "3");
        await Wait();
        CollectionAssert.AreEqual(new[] {"123"}, m.History);
      });
    }

    [Test]
    public async Task TestPrimitiveComposition()
    {
      await TestAsyncCalls(model =>
      {
        var vs = CFacade.Activator.Activate<IViewableSet<short>>();
        vs.Add(123);
        model.SyncMoments.Add(123, vs);
        return Task.CompletedTask;
      });
      
      Assert.AreEqual(true, ((IAsyncCallsTest)myClient).SyncMoments.First().Value.Contains(123));
    }

    private async Task TestAsyncCalls(Func<IAsyncCallsTest, Task> run) => await TestTemplate<AsyncCallsTest, IAsyncCallsTest>(run);
    private async Task TestSyncCalls(Func<ISyncCallsTest, Task> run) => await TestTemplate<SyncCallsTest, ISyncCallsTest>(run);
  }

}