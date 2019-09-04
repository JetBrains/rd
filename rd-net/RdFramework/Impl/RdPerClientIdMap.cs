using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
    public class RdPerClientIdMap<V> : RdReactiveBase, IViewableMap<ClientId, V> where V : RdBindableBase
    {
        private readonly Func<Boolean, V> myValueFactory;
        private readonly IViewableMap<ClientId, V> myMap;

        public RdPerClientIdMap(Func<bool, V> valueFactory)
        {
            myValueFactory = valueFactory;
            myMap = new ViewableMap<ClientId, V>();
        }

        public bool IsMaster;

        public override void OnWireReceived(UnsafeReader reader)
        {
            // this entity does not receive messages
            Assertion.Fail(nameof(RdPerClientIdMap<V>) + " may not receive messages");
        }

        protected override void Init(Lifetime lifetime)
        {
            base.Init(lifetime);
            Proto.ClientIdSet.View(lifetime, (clientIdLt, clientId) =>
            {
                var value = myValueFactory(IsMaster).WithId(RdId.Mix(clientId.Value));
                value.Bind(clientIdLt, this, $"[{clientId.Value}]");
                myMap.Add(clientId, value);
                clientIdLt.OnTermination(() => myMap.Remove(clientId));
            });
        }
        
        
        #region IViewableMap implementation

        IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
        public IEnumerator<KeyValuePair<ClientId, V>> GetEnumerator() => myMap.GetEnumerator();
        public void Add(KeyValuePair<ClientId, V> item) => throw new InvalidOperationException("May not modify RdPerClientIdMap directly");
        public void Clear() => throw new InvalidOperationException("May not modify RdPerClientIdMap directly");
        public bool Contains(KeyValuePair<ClientId, V> item) => myMap.Contains(item);
        public void CopyTo(KeyValuePair<ClientId, V>[] array, int arrayIndex) => myMap.CopyTo(array, arrayIndex);
        public bool Remove(KeyValuePair<ClientId, V> item) => throw new InvalidOperationException("May not modify RdPerClientIdMap directly");
        public bool IsReadOnly => true;
        public void Add(ClientId key, V value) => throw new InvalidOperationException("May not modify RdPerClientIdMap directly");
        public bool Remove(ClientId key) => throw new InvalidOperationException("May not modify RdPerClientIdMap directly");
        public void Advise(Lifetime lifetime, Action<MapEvent<ClientId, V>> handler) => myMap.Advise(lifetime, handler);
        public ISource<MapEvent<ClientId, V>> Change => myMap.Change;
        public int Count => myMap.Count;
        public ICollection<ClientId> Keys => myMap.Keys;
        public ICollection<V> Values => myMap.Values;
        public bool ContainsKey(ClientId key) => myMap.ContainsKey(key);
        public V this[ClientId key]
        {
            get => myMap[key];
            set => throw new InvalidOperationException("May not modify RdPerClientIdMap directly");
        }
        public bool TryGetValue(ClientId key, out V value) => myMap.TryGetValue(key, out value);
        
        #endregion

        
        public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdPerClientIdMap<V> value)
        {
            RdId.Write(writer, value.RdId);
        }

        public static RdPerClientIdMap<V> Read(SerializationCtx ctx, UnsafeReader reader, Func<bool, V> func)
        {
            var id = RdId.Read(reader);
            return new RdPerClientIdMap<V>(func).WithId(id);
        }
    }
}