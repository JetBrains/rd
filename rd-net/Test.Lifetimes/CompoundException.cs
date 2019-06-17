using System;
using System.Collections.Generic;
using System.Linq;

namespace Test.Lifetimes
{
    public class CompoundException : Exception
    {
        private readonly List<Exception> myExceptions;
        
        public IEnumerable<Exception> Exceptions => myExceptions;

        public CompoundException(IEnumerable<Exception> exceptions)
        {
            myExceptions = exceptions.ToList();
        }

        public override string Message
        {
            get
            {
                switch (myExceptions.Count)
                {
                    case 0: return "There were no exceptions";
                    case 1: return myExceptions.Single().Message;
                    default: return $"There were {myExceptions.Count} exceptions";
                }
            }
        }
    }
}