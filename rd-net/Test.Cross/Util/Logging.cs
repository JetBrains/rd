using System;

namespace Test.RdCross.Util
{
  public static class Logging
  {
    public static void LogWithTime(string message)
    {
      Console.WriteLine($"At {DateTime.Now:G} {message}");
    }

    public static T TrackAction<T>(string message, Func<T> action)
    {
      using (new LoggingCookie(message))
      {
        return action();
      }
    }
    
    public static void TrackAction(string message, Action action)
    {
      using (new LoggingCookie(message))
      {
        action();
      }
    }
  }

  class LoggingCookie : IDisposable
  {
    private readonly string myAction;

    public LoggingCookie(string action)
    {
      myAction = action;
      Logging.LogWithTime($"{myAction} started");
    }

    public void Dispose()
    {
      Logging.LogWithTime($"{myAction} finished");
    }
  }
}