using System;
using JetBrains.Collections.Viewable;
using JetBrains.Util.Util;
using NUnit.Framework;

namespace Test.Lifetimes.Utils
{
    public class CastToTest
    {
        [Test]
        public void TestReinterpretCastValueTypes()
        {
            int updateOrdinal = (int) AddUpdateRemove.Update;
            int removeOrdinal = (int) AddUpdateRemove.Remove;
            
            Assert.AreEqual(AddUpdateRemove.Update, CastTo<AddUpdateRemove>.ReinterpretFrom(updateOrdinal));
            Assert.AreEqual(AddUpdateRemove.Remove, CastTo<AddUpdateRemove>.ReinterpretFrom(removeOrdinal));
        }
        
        
        [Test]
        public void TestCastValueTypes()
        {
            int updateOrdinal = (int) AddUpdateRemove.Update;
            int removeOrdinal = (int) AddUpdateRemove.Remove;
            
            Assert.AreEqual(AddUpdateRemove.Update, CastTo<AddUpdateRemove>.From(updateOrdinal));
            Assert.AreEqual(AddUpdateRemove.Remove, CastTo<AddUpdateRemove>.From(removeOrdinal));
        }
        
        
#if !NET35 
        
        [Test]
        public void TestReinterpretCastReferenceTypes()
        {
            var from = new Tuple<ulong>(0x0123456789abcdef);
            var to = new Tuple<uint,uint>(0x89abcdef,0x01234567);

            var reinterpreted = CastTo<Tuple<uint,uint>>.ReinterpretFrom(@from);

            Assert.AreEqual(to.Item1, reinterpreted.Item1);
            Assert.AreEqual(to.Item2, reinterpreted.Item2);
            
            Assert.AreNotEqual(to, reinterpreted);
            Assert.AreEqual(from, reinterpreted);
        }
        
        
        [Test]
        public void TestCastReferenceTypes()
        {
            var from = new Tuple<ulong>(0x0123456789abcdef);
            var to = new Tuple<uint,uint>(0x89abcdef,0x01234567);

            Assert.Throws<TypeInitializationException>(() => CastTo<Tuple<uint,uint>>.From(@from));
        }
        
        
#endif
    }
}