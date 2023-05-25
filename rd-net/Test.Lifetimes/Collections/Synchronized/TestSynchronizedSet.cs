using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections.Synchronized;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Synchronized;

[TestFixture]
public class TestSynchronizedSet : LifetimesTestBase
{
  [Test]
  public void CopyToTest()
  {
    var list = Enumerable.Range(0, 3).ToList();
    ICollection<int> map = new SynchronizedSet<int>();
    foreach (var value in list)
      map.Add(value);

    Check(0);
    Check(1);
    Check(2);
    Check(3);
    Check(4);

    void Check(int startIndex)
    {
      var array = new int[4];
      map.CopyTo(array, startIndex);

      for (var i = 0; i < list.Count; i++)
      {
        var arrayIndex = startIndex + i;
        if (arrayIndex >= array.Length)
          return;

        Assert.AreEqual(list[i], array[arrayIndex]);
      }
    }
  }

}