namespace JetBrains.Collections.Viewable
{
  
  /// <summary>
  /// Extension of <see cref="ISource{T}"/> that can <see cref="Fire"/> messages.
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public interface ISignal<T> : ISource<T>
  {
    /// <summary>
    /// Currently all subscribers gets events on the same thread that <see cref="Fire"/> happened.
    /// This scheduler is reserved for future use.
    /// </summary>
    IScheduler Scheduler { get; set; }
    
    /// <summary>
    /// Fires value that will be seen by all entities who subscribed by <see cref="ISource{T}.Advise"/>
    /// </summary>
    /// <param name="value"></param>
    void Fire(T value);
  }
}