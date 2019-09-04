using JetBrains.Rd.Text.Impl.Intrinsics;
using JetBrains.Rd.Text.Intrinsics;

namespace Test.RdFramework.TextBuffer
{
  public struct TextBufferCommand
  {
    public RdTextChange Change { get; }
    public RdChangeOrigin Origin { get; }
    public bool DeliverImmediately { get; }

    public TextBufferCommand(RdTextChange change, RdChangeOrigin origin, bool deliverImmediately)
    {
      Change = change;
      Origin = origin;
      DeliverImmediately = deliverImmediately;
    }

    public override string ToString() { return $"TextBufferCommand(change={Change}, origin={Origin})"; }
  }
}