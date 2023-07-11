using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Viewable;

public class ViewablePropertyTest
{
  [TestCase]
  public void TestAdvise()
  {
    var prop = new ViewableProperty<int>(123);
    Lifetime.Using(lt =>
    {
      int? value = null;
      prop.Advise(lt, x => value = x);
      Assert.AreEqual(123, value);
    });
    Lifetime.Using(lt =>
    {
      int? value = null;
      var mapped = prop.Select(x => x + 1);
      mapped.Advise(lt, x => value = x);
      Assert.AreEqual(124, value);

      prop.Value = 125;
      Assert.AreEqual(126, value);
    });
  }
}
