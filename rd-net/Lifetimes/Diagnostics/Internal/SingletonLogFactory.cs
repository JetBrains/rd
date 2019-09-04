namespace JetBrains.Diagnostics.Internal
{
  /// <summary>
  /// This factory always create one instance of <see cref="ILog"/> for any category
  /// </summary>
  public class SingletonLogFactory : ILogFactory
  {
    private readonly ILog myLog;

    public SingletonLogFactory(ILog log)
    {
      myLog = log;
    }

    public ILog GetLog(string category)
    {
      return myLog;
    }
  }
}