#if NETCOREAPP
using System;
using System.Threading;

namespace Test.RdFramework
{
    public class ApartmentAttribute : Attribute
    {
        // ReSharper disable once UnusedParameter.Local
        public ApartmentAttribute(ApartmentState state)
        {
        }
    }
}
#endif