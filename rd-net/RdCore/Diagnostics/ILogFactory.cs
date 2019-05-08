using JetBrains.Annotations;

namespace JetBrains.Diagnostics
{
  public interface ILogFactory
  {
    [NotNull]
    ILog GetLog(string category);
  }
}