using System.Threading.Tasks;

namespace JetBrains.Collections.Viewable
{
  public sealed class DefaultScheduler : TaskSchedulerWrapper
  {
    public static DefaultScheduler Instance => new DefaultScheduler();
    
    private DefaultScheduler() : base(TaskScheduler.Default, true)
    {
    }
  }
}