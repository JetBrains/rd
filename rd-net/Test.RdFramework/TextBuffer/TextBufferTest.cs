using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Text;
using JetBrains.Rd.Text.Impl.Intrinsics;
using JetBrains.Rd.Text.Impl.Ot;
using JetBrains.Rd.Text.Intrinsics;
using NUnit.Framework;

namespace Test.RdFramework.TextBuffer
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class TextBufferTest : RdFrameworkTestBase
  {
    // auto-generated test data
    public static IEnumerable RandomChanges
    {
      get
      {
        yield return new List<TextBufferCommand> {
          new TextBufferCommand(new RdTextChange(RdTextChangeKind.Reset, 0, "", "abcde", 5), RdChangeOrigin.Master, true),
          new TextBufferCommand(new RdTextChange(RdTextChangeKind.Insert, 0, "", "   ", 8), RdChangeOrigin.Slave, false),
          new TextBufferCommand(new RdTextChange(RdTextChangeKind.Insert, 0, "", "#", 6), RdChangeOrigin.Master, false)
        };
      }
    }

    [Test, TestCaseSource(nameof(RandomChanges))]
    public void TestOtBasedText(List<TextBufferCommand> track)
    {
      var master = CreateTextBuffer(true);
      var slave = CreateTextBuffer(false);
      var masterText = "";
      var slaveText = "";

      master.Advise(TestLifetime, change =>
      {
        masterText = PlayChange(masterText, change);
        master.AssertState(masterText);
      });
      slave.Advise(TestLifetime, change =>
      {
        slaveText = PlayChange(slaveText, change);
        slave.AssertState(slaveText);
      });

      for (var i = 0; i < track.Count; i++)
      {
        var command = track[i];

        Console.WriteLine($@"#{i}: {command}");
        var change = command.Change;
        var origin = command.Origin;
        var shouldPump = command.DeliverImmediately;
        switch (origin)
        {
          case RdChangeOrigin.Slave:
          {
            slaveText = PlayChange(slaveText, change);
            slave.Fire(change);
            slave.AssertState(slaveText);
            if (shouldPump)
              PumpMessagesOnce_Slave();
            break;
          }
          case RdChangeOrigin.Master:
          {
            masterText = PlayChange(masterText, change);
            master.Fire(change);
            master.AssertState(masterText);
            if (shouldPump)
              PumpMessagesOnce_Master();
            break;
          }
          default:
            throw new ArgumentOutOfRangeException();
        }
      }

      // pumping twice because of acknowledge messages
      PumpMessagesOnce_Slave();
      PumpMessagesOnce_Master();
      PumpMessagesOnce_Slave();
      PumpMessagesOnce_Master();

      Assert.AreEqual(masterText, slaveText);
    }

    private static void Top(IRdBindable rdBindable, Lifetime lifetime, IProtocol protocol)
    {
      var name = rdBindable.GetType().Name;
      rdBindable.Identify(protocol.Identities, RdId.Nil.Mix(name));
      rdBindable.Bind(lifetime, protocol, name);
    }

    private static string PlayChange(string initText, RdTextChange change)
    {
      var x0 = change.StartOffset;
      var x1 = change.StartOffset + change.Old.Length;
      string newText;
      switch (change.Kind)
      {
        case RdTextChangeKind.Insert:
        {
          newText = initText.Substring(0, x0) + change.New + initText.Substring(x0);
          break;
        }
        case RdTextChangeKind.Remove:
        {
          newText = initText.Substring(0, x0) + initText.Substring(x1);
          break;
        }
        case RdTextChangeKind.Replace:
        {
          newText = initText.Substring(0, x0) + change.New + initText.Substring(x1);
          break;
        }
        case RdTextChangeKind.Reset:
        {
          newText = change.New;
          break;
        }
        default:
          throw new ArgumentOutOfRangeException();
      }
      Assert.AreEqual(change.FullTextLength, newText.Length);

      return newText;
    }

    private void PumpMessagesOnce_Slave() => ServerWire.TransmitAllMessages();
    private void PumpMessagesOnce_Master() => ClientWire.TransmitAllMessages();

    private ITextBuffer CreateTextBuffer(bool master)
    {
      var rdText = new RdOtBasedText {IsMaster = master};
      Top(rdText, TestLifetime, master ? ClientProtocol : ServerProtocol);
      return rdText;
    }
  }
}