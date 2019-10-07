using JetBrains.Rd.Base;

namespace JetBrains.Rd
{
  public interface IInternRoot : IRdReactive
  {
    bool TryGetInterned(object value, out int result);
    int Intern(object value);
    T UnIntern<T>(int id);
    void Remove(object value);
  }
  
}