using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;

namespace JetBrains.Rd
{
    public static class PerClientIdEx
    {
        public static T GetForCurrentClientId<T>(this RdPerClientIdMap<T> map) where T : RdBindableBase
        {
            var currentId = ClientId.CurrentOrNull;
            Assertion.Assert(currentId.HasValue, "ClientId != null");
            if (map.TryGetValue(currentId.Value, out var value))
                return value;
            Assertion.Fail("{0} has no value for ClientId {1}", map.Location, currentId.Value.Value);
            return default;
        }
    }
}