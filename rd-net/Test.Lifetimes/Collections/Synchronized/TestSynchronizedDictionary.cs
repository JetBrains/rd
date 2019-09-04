using JetBrains.Collections.Synchronized;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Synchronized
{
    [TestFixture]
    public class TestSynchronizedDictionary : LifetimesTestBase
    {
        public void TestLiveFiltering()
        {
            var dict = new SynchronizedDictionary<int, string>
            {
                {1, "1"},
                {2, "2"}
            };
            
            
        }
    }
}