using System;
using System.Runtime.InteropServices;
using JetBrains.Collections.Viewable;
using JetBrains.Util.Util;
using NUnit.Framework;

namespace Test.Lifetimes.Utils
{
    public class CastToTest
    {
        [Test]
        public void TestCastValueTypes()
        {
            int updateOrdinal = (int) AddUpdateRemove.Update;
            int removeOrdinal = (int) AddUpdateRemove.Remove;
            
            Assert.AreEqual(AddUpdateRemove.Update, CastTo<AddUpdateRemove>.From(updateOrdinal));
            Assert.AreEqual(AddUpdateRemove.Remove, CastTo<AddUpdateRemove>.From(removeOrdinal));
        }


        [Test]
        public void CastEnum32Test()
        {
            int updateOrdinal = (int) AddUpdateRemove.Update;
            int removeOrdinal = (int) AddUpdateRemove.Remove;
            
            Assert.AreEqual(AddUpdateRemove.Update, Cast32BitEnum<AddUpdateRemove>.FromInt(updateOrdinal));
            Assert.AreEqual(AddUpdateRemove.Remove, Cast32BitEnum<AddUpdateRemove>.FromInt(removeOrdinal));
            
            Assert.AreEqual(AddUpdateRemove.Update, Cast32BitEnum<AddUpdateRemove>.FromUInt((uint)updateOrdinal));
            Assert.AreEqual(AddUpdateRemove.Remove, Cast32BitEnum<AddUpdateRemove>.FromUInt((uint)removeOrdinal));
        }
        
        
        
        [Test]
        public void TestCastReferenceTypes()
        {
            var from = new Tuple<ulong>(0x0123456789abcdef);
            var to = new Tuple<uint,uint>(0x89abcdef,0x01234567);

            Assert.Throws<TypeInitializationException>(() => CastTo<Tuple<uint,uint>>.From(@from));
        }
        
        
    }
}