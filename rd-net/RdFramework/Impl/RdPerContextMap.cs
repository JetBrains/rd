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
    public class RdPerContextMap<K, V> : RdReactiveBase, IPerContextMap<K, V> where V : RdBindableBase
    {
        public RdContextKey<K> Key => myKey;
        private readonly RdContextKey<K> myKey;
        private readonly Func<Boolean, V> myValueFactory;
        private readonly IViewableMap<K, V> myMap;


        public RdPerContextMap(RdContextKey<K> key, Func<bool, V> valueFactory)
        {
            myValueFactory = valueFactory;
            myKey = key;
            myMap = new ViewableMap<K, V>();
        }

        public bool IsMaster;

        public override void OnWireReceived(UnsafeReader reader)
        {
            // this entity does not receive messages
            Assertion.Fail(nameof(RdPerContextMap<K, V>) + " may not receive messages");
        }

        protected override void Init(Lifetime lifetime)
        {
            base.Init(lifetime);
            Proto.ContextHandler.GetProtocolValueSet(myKey).View(lifetime, (contextValueLifetime, contextValue) =>
            {
                var value = myValueFactory(IsMaster).WithId(RdId.Mix(contextValue.ToString()));
                value.Bind(contextValueLifetime, this, $"[{contextValue.ToString()}]");
                myMap.Add(contextValue, value);
                contextValueLifetime.OnTermination(() => { myMap.Remove(contextValue); });
            });
        }
        
        public V GetForCurrentContext()
        {
          var currentId = myKey.Value;
          Assertion.Assert(currentId != null, "No value set for key {0}", myKey.Key);
          if (TryGetValue(currentId, out var value))
            return value;
          Assertion.Fail("{0} has no value for ContextKey {1} = {2}", Location, myKey.Key, currentId);
          return default;
        }
        

        public void View(Lifetime lifetime, Action<Lifetime, KeyValuePair<K, V>> handler)
        {
          Proto.ContextHandler.GetValueSet(myKey).View(lifetime,
            (keyLt, key) => handler(keyLt, new KeyValuePair<K, V>(key, this[key])));
        }
        
        public void View(Lifetime lifetime, Action<Lifetime, K, V> handler)
        {
          Proto.ContextHandler.GetValueSet(myKey).View(lifetime, (keyLt, key) => handler(keyLt, key, this[key]));
        }

        public V this[K key]
        {
            get => myMap[Proto.ContextHandler.GetHandlerForKey(myKey).TransformToProtocol(key)];
        }
        public bool TryGetValue(K key, out V value) => myMap.TryGetValue(Proto.ContextHandler.GetHandlerForKey(myKey).TransformToProtocol(key), out value);
        
        
        public static void Write(SerializationCtx context, UnsafeWriter writer, RdPerContextMap<K, V> value)
        {
            RdId.Write(writer, value.RdId);
        }

        public static RdPerContextMap<K, V> Read(SerializationCtx context, UnsafeReader reader, RdContextKey<K> key, Func<bool, V> func)
        {
            var id = RdId.Read(reader);
            return new RdPerContextMap<K, V>(key, func).WithId(id);
        }
    }
}