using JetBrains.Annotations;

namespace JetBrains.Diagnostics
{
  /// <summary>
  /// Logger's factory frontend. This class could
  /// be bound to any logger's backend (say log4net) used in your solution. <see cref="Log"/>
  /// <seealso cref="ILog"/>
  /// </summary>
  public interface ILogFactory
  {
    [NotNull]
    ILog GetLog(string category);
  }
}