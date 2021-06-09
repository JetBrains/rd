using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
    public class RdPerContextMap<K, V> : RdReactiveBase, IPerContextMap<K, V> where V : RdBindableBase
    {
        public RdContext<K> Context { get; }
        private readonly Func<Boolean, V> myValueFactory;
        private readonly IViewableMap<K, V> myMap;

        public RdPerContextMap(RdContext<K> context, Func<bool, V> valueFactory)
        {
            myValueFactory = valueFactory;
            Context = context;
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
            var protocolValueSet = Proto.Contexts.GetValueSet(Context);
            myMap.Keys.Where(it => !protocolValueSet.Contains(it)).ToList().ForEach(it => myMap.Remove(it));
            protocolValueSet.View(lifetime, (contextValueLifetime, contextValue) =>
            {
                myMap.TryGetValue(contextValue, out var oldValue);
                var value = (oldValue ?? myValueFactory(IsMaster)).WithId(RdId.Mix(contextValue.ToString()));
                value.Bind(contextValueLifetime, this, $"[{contextValue.ToString()}]");
                if (oldValue == null)
                  myMap.Add(contextValue, value);
                contextValueLifetime.OnTermination(() =>
                {
                  value.RdId = RdId.Nil;
                });
            });
        }
        
        public V GetForCurrentContext()
        {
          var currentId = Context.ValueForPerContextEntity;
          Assertion.Assert(currentId != null, "No value set for key {0}", Context.Key);
          if (TryGetValue(currentId, out var value))
            return value;
          Assertion.Fail("{0} has no value for Context {1} = {2}", Location, Context.Key, currentId);
          return default;
        }
        

        public void View(Lifetime lifetime, Action<Lifetime, KeyValuePair<K, V>> handler) => myMap.View(lifetime, handler);

        public void View(Lifetime lifetime, Action<Lifetime, K, V> handler) => myMap.View(lifetime, handler);

        public V this[K key]
        {
          get
          {
            if (!IsBound)
            {
              if (!myMap.ContainsKey(key))
                myMap[key] = myValueFactory(IsMaster);
            }
            return myMap[key];
          }
        }

        public bool TryGetValue(K key, out V value)
        {
          if (!IsBound)
          {
            if (!myMap.ContainsKey(key))
              myMap[key] = myValueFactory(IsMaster);
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