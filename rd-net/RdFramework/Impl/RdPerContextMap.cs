using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using JetBrains.Collections.Synchronized;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
    public class RdPerContextMap<K, V> : RdReactiveBase, IPerContextMap<K, V> where V : RdBindableBase where K: notnull
    {
        public RdContext<K> Context { get; }
        private readonly Func<Boolean, V> myValueFactory;
        private readonly IViewableMap<K, V> myMap;
        private ReactiveQueue<KeyValuePair<K, V>>? myQueue;

        public RdPerContextMap(RdContext<K> context, Func<bool, V> valueFactory)
        {
            myValueFactory = valueFactory;
            Context = context;
            myMap = new ViewableMap<K, V>(new SynchronizedDictionary<K, V>());
        }

        public bool IsMaster;

        public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader stream, IRdWireableDispatchHelper dispatchHelper)
        {
            // this entity does not receive messages
            var message = nameof(RdPerContextMap<K, V>) + " may not receive messages";
            Assertion.Fail(message);
        }

        protected override void PreInit(Lifetime lifetime, IProtocol proto)
        {
            base.PreInit(lifetime, proto);
            
            var queue = new ReactiveQueue<KeyValuePair<K, V>>();
            myQueue = queue;

            var protocolValueSet = proto.Contexts.GetValueSet(Context);
            proto.Scheduler.InvokeOrQueue(() =>
            {
              foreach (var k in myMap.Keys.Where(key => !protocolValueSet.Contains(key)).ToList()) 
                myMap.Remove(k);
            });
            
            protocolValueSet.View(lifetime, (contextValueLifetime, contextValue) =>
            {
              myMap.TryGetValue(contextValue, out var oldValue);

              var value = oldValue ?? myValueFactory(IsMaster);

              using (var cookie = contextValueLifetime.UsingExecuteIfAlive())
              {
                if (!cookie.Succeed)
                  return;
                
                value.WithId(proto.Identities.Mix(RdId, contextValue.ToString()));
                value.PreBind(contextValueLifetime, this, $"[{contextValue.ToString()}]");  
              }

              queue.Enqueue(new KeyValuePair<K, V>(contextValue, value));
            });
        }
        
        protected override void Init(Lifetime lifetime, IProtocol proto, SerializationCtx ctx)
        {
          base.Init(lifetime, proto, ctx);

          var queue = myQueue;
          if (queue == null)
          {
            if (proto.Lifetime.IsNotAlive)
              return;
            
            Assertion.Fail($"{nameof(myQueue)} must not be null");
          }

          queue.View(lifetime, pair =>
          {
            using var cookie = lifetime.UsingExecuteIfAlive();
            if (!cookie.Succeed)
              return;

            pair.Value.Bind();

            if (!myMap.ContainsKey(pair.Key))
            {
              proto.Scheduler.InvokeOrQueue(() =>
              {
                if (!myMap.ContainsKey(pair.Key))
                  myMap[pair.Key] = pair.Value;
              });
            }
          });
        }
        
        public V GetForCurrentContext()
        {
          var currentId = Context.ValueForPerContextEntity;
          if (Mode.IsAssertion) Assertion.AssertNotNull(currentId, "No value set for key {0}", Context.Key);
          if (TryGetValue(currentId, out var value))
            return value;
          Assertion.Fail("{0} has no value for Context {1} = {2}", Location, Context.Key, currentId);
          return default;
        }
        

        public void View(Lifetime lifetime, Action<Lifetime, KeyValuePair<K, V>> handler)
        {
          AssertThreading();
          myMap.View(lifetime, handler);
        }

        public void View(Lifetime lifetime, Action<Lifetime, K, V> handler)
        {
          AssertThreading();
          myMap.View(lifetime, handler);
        }

        public V this[K key]
        {
          get
          {
            if (!IsBound)
            {
              if (!myMap.ContainsKey(key))
                myMap[key] = myValueFactory(IsMaster);
            }

            AssertThreading();
            return myMap[key];
          }
        }

        public bool TryGetValue(K key, [MaybeNullWhen(false)] out V value)
        {
          if (!IsBound)
          {
            if (!myMap.ContainsKey(key))
              myMap[key] = myValueFactory(IsMaster);
          }
          
          AssertThreading();
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
        
        private class ReactiveQueue<T>
        {
          private Action<T>? myListener;
          private readonly Queue<T> myQueue = new();
          
          public void Enqueue(T value)
          {
            Action<T> action;
            lock (myQueue)
            {
              if (myListener is { } listener)
              {
                Assertion.Assert(myQueue.Count == 0);
                action = listener;
              }
              else
              {
                myQueue.Enqueue(value);
                return;
              }
            }

            action.Invoke(value);
          }

          public void View(Lifetime lifetime, Action<T> action)
          {
            while (lifetime.IsAlive)
            {
              T value;
              lock (myQueue)
              {
                if (myQueue.Count > 0)
                {
                  if (lifetime.IsNotAlive)
                    return;
                  
                  value = myQueue.Dequeue();
                }
                else
                {
                  lifetime.TryBracket(() =>
                  {
                    Assertion.Assert(myListener == null);
                    myListener = action;
                  }, () =>
                  {
                    lock (myQueue)
                      myListener = null;
                  });
                  
                  return;
                }
              }

              action(value);
            }
          } 
        }
    }
}