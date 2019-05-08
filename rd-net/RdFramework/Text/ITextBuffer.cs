using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd.Text.Intrinsics;

namespace JetBrains.Rd.Text
{
  public interface ITextBuffer : ISignal<RdTextChange>
  {
    TextBufferVersion BufferVersion { get; }
    bool IsMaster { get; }

    void Reset(string text);
    void AssertState(string allText);
  }

  public interface ITypingSession
  {
    void CommitRemoteChanges();
  }

  public interface ITextBufferWithTypingSession : ITextBuffer
  {
    ITypingSession StartTypingSession(Lifetime lifetime);
  }
}