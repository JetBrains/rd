namespace JetBrains.Lifetimes
{
  public interface ITerminationHandler
  {
    void OnTermination(Lifetime lifetime);
  }
}