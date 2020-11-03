using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using JetBrains.Annotations;

namespace JetBrains.Serialization
{
  public static class UnsafeWriterStatistics
  {
    public static int ReportAllocationOnNonCachedThreadThreshold = 1 << 20;
    public static int ReportAccessCounterThreshold = 1000;
    public static int ReportOnOfN = 1000;
    public static bool ReportReentrancy = false;

    // thread data
    [ThreadStatic] private static bool ourThreadIsUsed = false;
    [ThreadStatic] private static int ourThreadReentrancyCounter = 0;
    [ThreadStatic] private static IList<string> ourThreadReentrancyStacks = null;
    [ThreadStatic] private static int ourThreadAccessCounter = 0;
    [ThreadStatic] private static int ourThreadMaxAllocatedSize = 0;

    private static readonly IList<Event> ourEvents = new List<Event>();
    private static int ourThreadCount = 0;
    private static volatile int ourMaxAllocatedSize = 0;

    public static IList<Event> GetEvents()
    {
      lock (ourEvents)
        return ourEvents.ToArray();
    }

    public static int GetThreadCount() => ourThreadCount;
    public static int GetMaxAllocatedSize() => ourMaxAllocatedSize;

    public static void AddEvent(Event @event)
    {
      lock(ourEvents)
        ourEvents.Add(@event);
    }

    public static void ClearEvents()
    {
      lock(ourEvents)
        ourEvents.Clear();
    }

    public class Event
    {
      public EventType Type { get; }
      [CanBeNull] public string Message { get; }
      [NotNull] public IList<string> Stacktraces { get; }

      public Event(EventType type, [CanBeNull] string message, IList<string> stacktraces)
      {
        Type = type;
        Message = message;
        Stacktraces = stacktraces;
      }
    }

    public enum EventType
    {
      REENTRANCY,
    }


    private static void ReportEvent(EventType type, [CanBeNull] string message, IList<string> stacktraces = null)
    {
      var @event = new Event(type, message, stacktraces ?? new []{Environment.StackTrace});
      AddEvent(@event);
    }

    public static void OnCookieCreated()
    {
      if (!ourThreadIsUsed)
      {
        Interlocked.Increment(ref ourThreadCount);
        ourThreadIsUsed = true;
      }

      ourThreadReentrancyCounter++;
      if (ourThreadReentrancyCounter > 1 && ReportReentrancy)
      {
        if (ourThreadReentrancyStacks == null)
          ourThreadReentrancyStacks = new List<string>();
      }

      ourThreadAccessCounter++;
    }

    public static void OnCookieDisposing(int currentSize)
    {
      ourThreadReentrancyCounter--;
      if (ourThreadReentrancyStacks != null)
      {
        ourThreadReentrancyStacks.Add(Environment.StackTrace);
        if (ourThreadReentrancyCounter == 0)
        {
          ReportEvent(EventType.REENTRANCY, null, ourThreadReentrancyStacks);
          ourThreadReentrancyStacks = null;
        }
      }

      if (currentSize > ourThreadMaxAllocatedSize)
      {
        ourThreadMaxAllocatedSize = currentSize;
        while (ourThreadMaxAllocatedSize > ourMaxAllocatedSize)
        {
          Thread.MemoryBarrier();
          Interlocked.CompareExchange(ref ourMaxAllocatedSize, ourThreadMaxAllocatedSize, ourMaxAllocatedSize);
        }
      }
    }
  }
}