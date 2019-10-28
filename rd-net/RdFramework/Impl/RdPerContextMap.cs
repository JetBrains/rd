using System;
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
        public RdContext<K> Key => myKey;
        private readonly RdContext<K> myKey;
        private readonly Func<Boolean, V> myValueFactory;
        private readonly IViewableMap<K, V> myMap;

        private readonly IViewableSet<K> myLocalValueSet = new ViewableSet<K>();
        private readonly SwitchingViewableSet<K> mySwitchingValueSet;
        private readonly SequentialLifetimes myUnboundLifetimes = new SequentialLifetimes(Lifetime.Eternal);
        private readonly IDictionary<K, V> myUnboundValues = new Dictionary<K, V>();


        public RdPerContextMap(RdContext<K> key, Func<bool, V> valueFactory)
        {
            myValueFactory = valueFactory;
            myKey = key;
            myMap = new ViewableMap<K, V>();

            mySwitchingValueSet = new SwitchingViewableSet<K>(Lifetime.Eternal, myLocalValueSet);

            BindToUnbounds();
        }

        private void BindToUnbounds()
        {
          var unboundLt = myUnboundLifetimes.Next();
          using (Signal.PriorityAdviseCookie.Create())
          {
            myLocalValueSet.View(unboundLt, (localKeyLt, localKey) =>
            {
              myUnboundValues[localKey] = myValueFactory(IsMaster);
              localKeyLt.OnTermination(() => myUnboundValues.Remove(localKey));
            });
          }
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
            var protocolValueSet = Proto.Contexts.GetValueSet(myKey);
            protocolValueSet.View(lifetime, (contextValueLifetime, contextValue) =>
            {
                myUnboundValues.TryGetValue(contextValue, out var previousUnboundValue);
                var value = (previousUnboundValue ?? myValueFactory(IsMaster)).WithId(RdId.Mix(contextValue.ToString()));
                value.Bind(contextValueLifetime, this, $"[{contextValue.ToString()}]");
                myMap.Add(contextValue, value);
                contextValueLifetime.OnTermination(() => { myMap.Remove(contextValue); });
            });
            mySwitchingValueSet.ChangeBackingSet(Proto.Contexts.GetValueSet(myKey), true);
            myUnboundLifetimes.TerminateCurrent();
            lifetime.OnTermination(() =>
            {
              mySwitchingValueSet.ChangeBackingSet(myLocalValueSet, false);
              BindToUnbounds();
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
          mySwitchingValueSet.View(lifetime,
            (keyLt, key) => handler(keyLt, new KeyValuePair<K, V>(key, this[key])));
        }
        
        public void View(Lifetime lifetime, Action<Lifetime, K, V> handler)
        {
          mySwitchingValueSet.View(lifetime, (keyLt, key) => handler(keyLt, key, this[key]));
        }

        public V this[K key]
        {
          get
          {
            if (!IsBound)
            {
              if(!myLocalValueSet.Contains(key))
                myLocalValueSet.Add(key);

              return myUnboundValues[key];
            }
            return myMap[key];
          }
        }

        public bool TryGetValue(K key, out V value)
        {
          if (!IsBound)
          {
            if(!myLocalValueSet.Contains(key))
              myLocalValueSet.Add(key); // todo: should we force creation here?

            return myUnboundValues.TryGetValue(key, out value);
          }
          return myMap.TryGetValue(key, out value);
        }


        public static void Write(SerializationCtx context, UnsafeWriter writer, RdPerContextMap<K, V> value)
        {
            RdId.Write(writer, value.RdId);
        }

        public static RdPerContextMap<K, V> Read(SerializationCtx context, UnsafeReader reader, RdContext<K> key, Func<bool, V> func)
        {
            var id = RdId.Read(reader);
            return new RdPerContextMap<K, V>(key, func).WithId(id);
        }
    }
}