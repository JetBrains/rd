using System;
using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Serialization;

namespace Test.RdFramework.Components
{
  public class TestWire : WireBase
  {
    private readonly IScheduler myScheduler;
    private readonly string myName;
    private readonly bool myIsMaster;
    private readonly Queue<Message> myOutgoingMessages;

    public long BytesSent { get; private set; }

    public TestWire(IScheduler scheduler, string name, bool isMaster) : base(scheduler)
    {
      myScheduler = scheduler;
      myName = name;
      myIsMaster = isMaster;
      myOutgoingMessages = new Queue<Message>();
    }

    public TestWire Connection { get; set; }

    public bool IsMaster
    {
      get { return myIsMaster; }
    }

    public bool HasMessages
    {
      get { return myOutgoingMessages.Count > 0; }
    }

    public void TransmitOneMessage()
    {
      myScheduler.InvokeOrQueue(() =>
      {
        var message = myOutgoingMessages.Dequeue();
        Connection.Receive(message.Data);
      });
    }

    public void TransmitAllMessages()
    {
      myScheduler.InvokeOrQueue(() =>
      {
        while (myOutgoingMessages.Count > 0)
          TransmitOneMessage();
      });
    }

    public void MissOneMessage()
    {
      myOutgoingMessages.Dequeue();
    }

    protected override void SendPkg(UnsafeWriter.Cookie cookie)
    {
      var pkg = cookie.CloneData();

      BytesSent += pkg.Length;

      //strip length
      var data = new byte[pkg.Length - sizeof(int)];
      Array.Copy(pkg, sizeof(int), data, 0, data.Length);

      myOutgoingMessages.Enqueue(new Message { Data = data });
      if (AutoTransmitMode) TransmitAllMessages();
    }

    public override string ToString()
    {
      return myName;
    }

    public bool AutoTransmitMode { get; set; }

    private class Message
    {
      public byte[] Data { get; set; }
    }
  }
}