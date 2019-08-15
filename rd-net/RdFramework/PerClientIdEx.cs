using System;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;

namespace JetBrains.Rd
{
    public static class PerClientIdEx
    {
        public static T GetForCurrentClientId<T>(this RdMap<ClientId, T> map)
        {
            var currentId = ClientId.CurrentOrNull;
            Assertion.Assert(currentId.HasValue, "ClientId != null");
            if (map.TryGetValue(currentId.Value, out var value))
                return value;
            Assertion.Fail("{0} has no value for ClientId {1}", map.Location, currentId.Value.Id);
            return default;
        }

        public static void AdviseForProtocolClientIds<T>(this RdMap<ClientId, T> map, Lifetime lifetime, Func<T> valueFactory)
        {
            map.Proto.ClientIdSet.View(lifetime, (clientIdLifetime, clientId) =>
            {
                map.Add(clientId, valueFactory());
                clientIdLifetime.OnTermination(() => map.Remove(clientId));
            });
        }
    }
}