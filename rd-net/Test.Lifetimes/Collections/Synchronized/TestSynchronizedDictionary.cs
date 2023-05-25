using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections;
using JetBrains.Collections.Synchronized;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Synchronized
{
    [TestFixture]
    public class TestSynchronizedDictionary : LifetimesTestBase
    {
      [Test]
      public void CopyToTest()
      {
        var list = Enumerable.Range(0, 3).Select(x => new KeyValuePair<int, string>(x, x.ToString())).ToList();
        IDictionary<int, string> map = new SynchronizedDictionary<int, string>();
        foreach (var (key, value) in list) 
          map.Add(key, value);
        
        Check(0);
        Check(1);
        Check(2);
        Check(3);
        Check(4);
        
        void Check(int startIndex)
        {
          var array = new KeyValuePair<int, string>[4];
          map.CopyTo(array, startIndex);

          for (var i = 0; i < list.Count; i++)
          {
            var arrayIndex = startIndex + i;
            if (arrayIndex >= array.Length)
              return;
            
            Assert.AreEqual(list[i].Key, array[arrayIndex].Key);
            Assert.AreEqual(list[i].Value, array[arrayIndex].Value);
          }
        }
      }

    }
}