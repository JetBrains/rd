using System.Threading;
using JetBrains.Annotations;

namespace JetBrains.Threading
{
  public static class ThreadEx
  {
    public static string ToThreadString([CanBeNull] this Thread thread)
    {
      return thread == null ? "<NULL>" : $"{thread.Name ?? ""}:{thread.ManagedThreadId}";
    }
  }
}