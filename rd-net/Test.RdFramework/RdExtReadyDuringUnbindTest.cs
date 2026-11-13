using System;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Serialization;
using NUnit.Framework;
using Test.RdFramework.Interning;

namespace Test.RdFramework
{
  public class RdExtReadyDuringUnbindTest : RdFrameworkTestBase
  {
    [Test]
    public void TestNoReplyToReadyOnceTheExtLifetimeTerminates()
    {
      ClientWire.AutoTransmitMode = true;
      ServerWire.AutoTransmitMode = true;

      var serverRoot = new InterningRoot1(LifetimeDefinition.Lifetime, ServerProtocol);
      serverRoot.GetOrCreateExtension("test", () => new InterningExt());

      // the ext binds on the lifetime of its root, which is the lifetime the message broker gives to its messages
      var extLifetimeDef = LifetimeDefinition.Lifetime.CreateNested();
      var clientRoot = new InterningRoot1(extLifetimeDef.Lifetime, ClientProtocol);
      var ext = clientRoot.GetOrCreateExtension("test", () => new InterningExt());

      var executingWhileSending = -1;
      ClientWire.OnTransmit.Advise(TestLifetime, _ => executingWhileSending = extLifetimeDef.Lifetime.ExecutingCount);

      Assert.AreNotEqual(0, BytesSentOnReady(ext, extLifetimeDef.Lifetime), "a bound ext answers the counterpart's Ready");
      Assert.AreEqual(1, executingWhileSending, "the reply holds the ext lifetime, so an unbind on another thread waits for it");

      // The wire thread can dispatch Ready while another thread unbinds the ext.
      // This action runs first in the termination, while the ext is still bound.
      var boundDuringTermination = false;
      long bytesSentDuringTermination = -1;
      extLifetimeDef.Lifetime.OnTermination(() =>
      {
        boundDuringTermination = ext.IsBound;
        bytesSentDuringTermination = BytesSentOnReady(ext, extLifetimeDef.Lifetime);
      });
      extLifetimeDef.Terminate();

      Assert.IsTrue(boundDuringTermination, "precondition: the ext is still bound");
      Assert.AreEqual(0, bytesSentDuringTermination, "an ext that is being unbound must not answer Ready");
    }

    private unsafe long BytesSentOnReady(RdExtBase ext, Lifetime lifetime)
    {
      byte[] message;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        cookie.Writer.Write((int) RdExtBase.ExtState.Ready);
        cookie.Writer.Write(ext.GetSerializationHash());
        message = cookie.CloneData();
      }

      var before = ClientWire.BytesSent;
      fixed (byte* data = message)
      {
        // In the race, the wire thread passes the lifetime check of the entry point before the unbind starts,
        // so the test calls the handler behind that check directly
        var reader = UnsafeReader.CreateReader(data, message.Length);
        ext.OnWireReceived(ClientProtocol, ClientProtocol.SerializationContext, reader, new DispatchHelper(ext.RdId, lifetime));
      }
      return ClientWire.BytesSent - before;
    }

    private class DispatchHelper : IRdWireableDispatchHelper
    {
      public DispatchHelper(RdId rdId, Lifetime lifetime)
      {
        RdId = rdId;
        Lifetime = lifetime;
      }

      public RdId RdId { get; }
      public Lifetime Lifetime { get; }

      public void Dispatch(IScheduler scheduler, Action action) => action();
    }
  }
}
