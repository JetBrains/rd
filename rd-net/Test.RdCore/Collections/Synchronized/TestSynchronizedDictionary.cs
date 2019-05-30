using JetBrains.Collections.Synchronized;
using NUnit.Framework;

namespace Test.RdCore.Collections.Synchronized
{
    [TestFixture]
    public class TestSynchronizedDictionary : RdCoreTestBase
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