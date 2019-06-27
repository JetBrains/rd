namespace JetBrains.Lifetimes
{
  /// <summary>
  /// Alternative to <see cref="Lifetime.OnTermination(System.Action)"/>.
  /// Special interface for <see cref="Lifetime"/>'s termination participants to implement to avoid memory traffic on lambda creation.
  /// </summary>
  public interface ITerminationHandler
  {
    void OnTermination(Lifetime lifetime);
  }
}