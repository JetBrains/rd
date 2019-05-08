namespace JetBrains.Collections.Viewable
{
  public interface ISignal<T> : ISource<T>
  {
    IScheduler Scheduler { get; set; }
    void Fire(T value);
  }
}