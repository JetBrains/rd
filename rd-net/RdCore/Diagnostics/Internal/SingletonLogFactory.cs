namespace JetBrains.Diagnostics.Internal
{
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