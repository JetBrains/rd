using System;
using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Text.Impl.Intrinsics;
using JetBrains.Rd.Text.Impl.Ot.Intrinsics;
using JetBrains.Rd.Text.Intrinsics;

namespace JetBrains.Rd.Text.Impl.Ot
{
  public class RdOtBasedText : RdDelegateBase<RdOtState>, ITextBuffer
  {
    public bool IsMaster { get; set; } = false;

    private bool myIsComplexChange;

    private readonly IViewableProperty<RdTextChange> myTextChanged;

    private List<OtOperation> myDiff;

    public TextBufferVersion BufferVersion { get; private set; }

    private RdChangeOrigin LocalOrigin => IsMaster ? RdChangeOrigin.Master : RdChangeOrigin.Slave;

    public RdOtBasedText() : this(new RdOtState())
    {
    }

    public RdOtBasedText(RdOtState @delegate) : base(@delegate)
    {
      myTextChanged = new ViewableProperty<RdTextChange>();
      myDiff = new List<OtOperation>();

      // disabling mastering, text buffer must resolve conflicts by itself
      ((RdProperty<OtOperation>) Delegate.Operation).IsMaster = false;
    }

    class MasterSlave
    {
      public RdAssertion Master { get; }
      public RdAssertion Slave { get; }

      public MasterSlave(RdAssertion master, RdAssertion slave)
      {
        Master = master;
        Slave = slave;
      }
    }
    
    public override void Bind(Lifetime lf, IRdDynamic parent, string name)
    {
      base.Bind(lf, parent, name);

      Delegate.Operation.AdviseNotNull(lf, op =>
      {
        if (op.Origin != LocalOrigin) ReceiveOperation(op);
      });

      Delegate.Ack.Advise(lf, ack =>
      {
        if (ack.Origin != LocalOrigin) UpdateHistory(ack);
      });

      Delegate.AssertedMasterText.Compose(lf, Delegate.AssertedSlaveText, (m, s) => new MasterSlave(m, s)).Advise(lf, ms =>
      {
        var m = ms.Master;
        var s = ms.Slave;
        if (m.MasterVersion == s.MasterVersion
            && m.SlaveVersion == s.SlaveVersion
            && m.Text != s.Text)
        {
          throw new Exception($"Master and Slave texts are different.\nMaster:\n{m.Text}\nSlave:\n{s.Text}");
        }
      });
    }

    private void UpdateHistory(RdAck ack)
    {
      var ts = ack.Timestamp;
      myDiff.RemoveAll(op => op.Timestamp == ts);
    }

    private void ReceiveOperation(OtOperation operation)
    {
      var remoteOrigin = operation.Origin;
      var transformedOp = operation;
      switch (operation.Kind)
      {
        case OtOperationKind.Normal:
        {
          var newDiff = new List<OtOperation>();
          foreach (var localChange in myDiff)
          {
            var result = OtFramework.Transform(localChange, transformedOp);
            newDiff.Add(result.NewLocalDiff);
            transformedOp = result.LocalizedApplyToDocument;
          }

          myDiff = newDiff;

          break;
        }
        case OtOperationKind.Reset:
        {
          myDiff = new List<OtOperation>();
          break;
        }
        default:
          throw new ArgumentOutOfRangeException();
      }

      BufferVersion = remoteOrigin == RdChangeOrigin.Master
        ? BufferVersion.IncrementMaster()
        : BufferVersion.IncrementSlave();

      var timestamp = operation.Timestamp;
      Assertion.Assert(remoteOrigin == RdChangeOrigin.Master
        ? BufferVersion.Master == timestamp
        : BufferVersion.Slave == timestamp, "operation.Timestamp == BufferVersion");

      var changes = transformedOp.ToRdTextChanges();

      for (var i = 0; i < changes.Count; i++)
      {
        using (new ComplexChangeCookie(this, i < changes.Count - 1))
        {
          var change = changes[i];
          myTextChanged.SetValue(change);
        }
      }

      if (operation.Kind == OtOperationKind.Normal)
        SendAck(timestamp);
    }

    private void SendAck(int timestamp)
    {
      Delegate.Ack.Fire(new RdAck(timestamp, LocalOrigin));
    }

    public IScheduler Scheduler { get; set; }

    public void Fire(RdTextChange value)
    {
      Assertion.Assert(Delegate.IsBound || BufferVersion == TextBufferVersion.InitVersion, "Delegate.IsBound || BufferVersion == TextBufferVersion.InitVersion");
      if (Delegate.IsBound) Proto.Scheduler.AssertThread();

      BufferVersion = IsMaster ? BufferVersion.IncrementMaster() : BufferVersion.IncrementSlave();
      var ts = GetCurrentTs();
      var operation = value.ToOperation(LocalOrigin, ts);

      switch (operation.Kind)
      {
        case OtOperationKind.Normal:
          myDiff.Add(operation);
          break;
        case OtOperationKind.Reset:
          myDiff = new List<OtOperation>();
          break;
        default:
          throw new ArgumentOutOfRangeException();
      }

      SendOperation(operation);
    }

    private void SendOperation(OtOperation operation)
    {
      Delegate.Operation.SetValue(operation);
    }

    private int GetCurrentTs() => IsMaster ? BufferVersion.Master : BufferVersion.Slave;

    public void Advise(Lifetime lifetime, Action<RdTextChange> handler)
    {
      Assertion.Assert(Delegate.IsBound, "Delegate.IsBound");
      Proto.Scheduler.AssertThread();

      myTextChanged.Advise(lifetime, handler);
    }

    public void Reset(string text)
    {
      Fire(new RdTextChange(RdTextChangeKind.Reset, 0, "", text, text.Length));
    }

    public void AssertState(string allText)
    {
      if (myIsComplexChange) return;
      var assertion = new RdAssertion(BufferVersion.Master, BufferVersion.Slave, allText);
      if (IsMaster)
        Delegate.AssertedMasterText.SetValue(assertion);
      else
        Delegate.AssertedSlaveText.SetValue(assertion);
    }

    private struct ComplexChangeCookie : IDisposable
    {
      private readonly RdOtBasedText myOwner;
      private readonly bool myOldValue;

      public ComplexChangeCookie(RdOtBasedText owner, bool changeFlag = true)
      {
        myOwner = owner;
        myOldValue = owner.myIsComplexChange;
        if (changeFlag)
          owner.myIsComplexChange = true;
      }

      public void Dispose()
      {
        myOwner.myIsComplexChange = myOldValue;
      }
    }
  }
}