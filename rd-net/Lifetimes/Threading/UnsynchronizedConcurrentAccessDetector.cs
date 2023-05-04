using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Diagnostics;

namespace JetBrains.Threading;

public class UnsynchronizedConcurrentAccessDetector
{
  private static readonly ILog ourLog = Log.GetLog<UnsynchronizedConcurrentAccessDetector>();
  
  private List<string>? myList;
  private readonly ThreadLocal<int> myLocalCount = new();
  private int myCount;

  public Cookie CreateCookie() => new(this);
  
  public readonly ref struct Cookie
  {
    private readonly UnsynchronizedConcurrentAccessDetector myDetector;
    private readonly bool myIsExceptionAlreadyCaptured;

    internal Cookie(UnsynchronizedConcurrentAccessDetector detector)
    {
      myDetector = detector;

      lock (detector.myLocalCount)
      {
        var currentThreadCount = detector.myLocalCount.Value++;
        if (currentThreadCount > 0)
        {
          myIsExceptionAlreadyCaptured = false;
          return;
        }

        var participantsCount = myDetector.myCount++;
        if (participantsCount == 0)
        {
          myIsExceptionAlreadyCaptured = false;
          return;
        }

        myDetector.myList ??= new List<string>(1);
        myDetector.myList.Add(GetStackTrace());
        myIsExceptionAlreadyCaptured = true;
      }
    }

    private static string GetStackTrace()
    {
      return $"Thread: {Thread.CurrentThread.ToThreadString()} --> {Environment.NewLine}{Environment.StackTrace}";
    }

    public void Dispose()
    {
      if (myDetector == null)
        return;
      
      lock (myDetector.myLocalCount)
      {
        var currentThreadCount = --myDetector.myLocalCount.Value;
        if (currentThreadCount > 0)
          return;

        var list = myDetector.myList;
        if (!myIsExceptionAlreadyCaptured &&  list != null) 
          list.Add(GetStackTrace());

        var participantsCount = --myDetector.myCount;
        if (participantsCount != 0)
          return;

        if (list != null)
        {
          myDetector.myList = null;
          ourLog.Error($"Unsynchronized access has been detected: {Environment.NewLine}{string.Join($"-------------------------------{Environment.NewLine}", list.ToArray())}");
        }
      }
    }
  }
}

