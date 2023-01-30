#if !NET35
using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Text.Impl.Intrinsics;
using JetBrains.Rd.Text.Intrinsics;

namespace JetBrains.Rd.Text.Impl
{
  public class RdTextBuffer : RdDelegateBase<RdTextBufferState>, ITextBufferWithTypingSession
  {
    public bool IsMaster { get; }
    private readonly RdChangeOrigin myLocalOrigin;

    private readonly List<RdTextBufferChange> myChangesToConfirmOrRollback;
    private readonly IViewableProperty<RdTextChange> myTextChanged;

    private TextBufferTypingSession? myActiveSession;

    public bool IsCommitting => myActiveSession != null && myActiveSession.IsCommitting;

    public TextBufferVersion BufferVersion { get; private set; }

    /// <summary>
    /// Slave of the text buffer supports a list of changes that were introduced locally and can be rolled back when master buffer reports incompatible change
    /// </summary>
    public RdTextBuffer() : this(new RdTextBufferState())
    {
    }

    public RdTextBuffer(RdTextBufferState state, bool isMaster = false) : base(state)
    {
      IsMaster = isMaster;
      myTextChanged = new ViewableProperty<RdTextChange>();
      myChangesToConfirmOrRollback = new List<RdTextBufferChange>();
      BufferVersion = TextBufferVersion.InitVersion;
      myLocalOrigin = IsMaster ? RdChangeOrigin.Master : RdChangeOrigin.Slave;

      // disabling mastering, text buffer must resolve conflicts by itself
      ((RdProperty<RdTextBufferChange>) Delegate.Changes).IsMaster = false;
    }

    public override void Bind(Lifetime lf, IRdDynamic parent, string name)
    {
      base.Bind(lf, parent, name);
      Delegate.Changes.AdviseNotNull(lf, change =>
      {
        if (change.Origin == myLocalOrigin) return;
        if (myActiveSession != null && myActiveSession.TryPushRemoteChange(change))
        {
          return;
        }

        ReceiveChange(change);
      });

      Delegate.AssertedMasterText.Compose(lf, Delegate.AssertedSlaveText, Tuple.Create).Advise(lf, tuple =>
      {
        var m = tuple.Item1;
        var s = tuple.Item2;
        if (m.MasterVersion == s.MasterVersion
            && m.SlaveVersion == s.SlaveVersion
            && m.Text != s.Text)
        {
          throw new Exception($"Master and Slave texts are different.\nMaster:\n{m.Text}\nSlave:\n{s.Text}");
        }
      });
    }

    private void ReceiveChange(RdTextBufferChange rdTextBufferChange)
    {
      var newVersion = rdTextBufferChange.Version;
      var change = rdTextBufferChange.Change;
      var side = rdTextBufferChange.Origin;
      if (Mode.IsAssertion) Assertion.Assert(side != myLocalOrigin, "side != mySide");

      var masterVersionRemote = newVersion.Master;
      var slaveVersionRemote = newVersion.Slave;

      if (change.Kind == RdTextChangeKind.Reset)
      {
        ClearState();
      }
      else if (change.Kind == RdTextChangeKind.PromoteVersion)
      {
        if (Mode.IsAssertion) Assertion.Assert(!IsMaster);
        BufferVersion = newVersion;
        return;
      }
      else
      {
        if (IsMaster)
        {
          if (Mode.IsAssertion) Assertion.Assert(myChangesToConfirmOrRollback.Count == 0);
          if (masterVersionRemote != BufferVersion.Master)
          {
            // reject the change. we've already sent overriding change.
            return;
          }
        }
        else
        {
          if (slaveVersionRemote != BufferVersion.Slave)
          {
            // rollback the changes and notify external subscribers
            // don't need to update history here - all reverted changes were already stored before on 'fire' stage
            foreach (var ch in Enumerable.Reverse(myChangesToConfirmOrRollback))
            {
              if (ch.Version.Slave <= slaveVersionRemote)
                break;
              myTextChanged.SetValue(ch.Change.Reverse());
            }
            myChangesToConfirmOrRollback.Clear();
          }
          else
          {
            // confirm the changes queue.
            myChangesToConfirmOrRollback.Clear();
          }
        }
      }

      BufferVersion = newVersion;

      if (!IsMaster || myActiveSession == null || !myActiveSession.IsCommitting)
      {
        myTextChanged.SetValue(change);
      }
    }

    private void ClearState()
    {
      myChangesToConfirmOrRollback.Clear();
    }

    public IScheduler? Scheduler { get; set; }

    public void Fire(RdTextChange change)
    {
      if (Mode.IsAssertion) Assertion.Assert(Delegate.IsBound || BufferVersion == TextBufferVersion.InitVersion);
      if (Delegate.IsBound) Proto.Scheduler.AssertThread();

      if (IsMaster && myActiveSession != null && myActiveSession.IsCommitting)
      {
        return;
      }

      IncrementBufferVersion();
      var bufferChange = new RdTextBufferChange(BufferVersion, myLocalOrigin, change);
      if (change.Kind == RdTextChangeKind.Reset)
      {
        ClearState();
      }
      else if (!IsMaster)
      {
        myChangesToConfirmOrRollback.Add(bufferChange);
      }

      myActiveSession?.TryPushLocalChange(change);

      Delegate.Changes.SetValue(bufferChange);
    }

    private void IncrementBufferVersion()
    {
      BufferVersion = IsMaster ? BufferVersion.IncrementMaster() : BufferVersion.IncrementSlave();
    }

    public void Advise(Lifetime lifetime, Action<RdTextChange> change)
    {
      Assertion.Assert(Delegate.IsBound);
      Proto.Scheduler.AssertThread();
      
      myTextChanged.Advise(lifetime, change);
    }

    public void Reset(string text)
    {
      Fire(new RdTextChange(RdTextChangeKind.Reset, 0, "", text, text.Length));
    }

    public void AssertState(string allText)
    {
      var assertion = new RdAssertion(BufferVersion.Master, BufferVersion.Slave, allText);
      if (IsMaster)
        Delegate.AssertedMasterText.SetValue(assertion);
      else
        Delegate.AssertedSlaveText.SetValue(assertion);
    }

    public ITypingSession StartTypingSession(Lifetime lifetime)
    {
      Assertion.Assert(myActiveSession == null);
      Assertion.Assert(lifetime.IsAlive);

      myActiveSession = new TextBufferTypingSession(this);
      lifetime.OnTermination(() => myActiveSession = null);
      return myActiveSession;
    }

    public class TextBufferTypingSession : ITypingSession
    {
      private enum State
      {
        Opened,
        Committing
      }

      private readonly RdTextBuffer myBuffer;
      private readonly List<RdTextBufferChange> myRemoteChanges = new List<RdTextBufferChange>();
      private readonly List<RdTextChange> myLocalChanges = new List<RdTextChange>();
      private readonly TextBufferVersion myVersionBeforeOpening;
      private readonly TextBufferVersion myInitialBufferVersion;
      private State myState = State.Opened;

      public readonly Signal<RdTextChange> OnLocalChange = new Signal<RdTextChange>();
      public readonly Signal<RdTextChange> OnRemoteChange = new Signal<RdTextChange>();

      public TextBufferTypingSession(RdTextBuffer buffer)
      {
        myBuffer = buffer;
        myInitialBufferVersion = myBuffer.BufferVersion;

        if (buffer.IsMaster)
        {
          myVersionBeforeOpening = myBuffer.BufferVersion;
          myBuffer.Delegate.VersionBeforeTypingSession.Value = myInitialBufferVersion;
        }
        else
        {
          myVersionBeforeOpening = buffer.Delegate.VersionBeforeTypingSession.Value;
        }
      }

      public bool IsCommitting => myState == State.Committing;

      public bool TryPushLocalChange(RdTextChange change)
      {
        if (myState != State.Opened) return false;

        OnLocalChange.Fire(change);
        myLocalChanges.Add(change);
        return true;
      }

      private static int CompareVersions(TextBufferVersion first, TextBufferVersion second)
      {
        if (first.Master != second.Master)
          return first.Master - second.Master;
        return first.Slave - second.Slave;
      }

      public bool TryPushRemoteChange(RdTextBufferChange change)
      {
        if (myState != State.Opened) return false;
        if (!myBuffer.IsMaster && change.Version.Master <= myVersionBeforeOpening.Master) return false;
        if (myBuffer.IsMaster && CompareVersions(change.Version, myVersionBeforeOpening) <= 0) return false;

        OnRemoteChange.Fire(change.Change);
        myRemoteChanges.Add(change);
        return true;
      }

      public void CommitRemoteChanges()
      {
        StartCommitRemoteVersion();
        FinishCommitRemoteVersion();
      }

      public void FinishCommitRemoteVersion()
      {
        if (!myBuffer.IsMaster)
        {
          for (var i = myLocalChanges.Count - 1; i >= 0; i--)
          {
            var change = myLocalChanges[i];
            myBuffer.myTextChanged.SetValue(change.Reverse());
          }
        }

        myBuffer.BufferVersion = myInitialBufferVersion;

        foreach (var bufferChange in myRemoteChanges)
        {
          myBuffer.ReceiveChange(bufferChange);
        }
      }

      public void StartCommitRemoteVersion()
      {
        Assertion.Assert(myState == State.Opened);
        myState = State.Committing;
      }
    }
  }
}
#endif