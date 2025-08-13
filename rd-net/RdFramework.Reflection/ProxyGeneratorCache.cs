﻿using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Reflection;
using System.Reflection.Emit;

namespace JetBrains.Rd.Reflection
{
  public class ProxyGeneratorCache : IProxyGenerator
  {
    private readonly ProxyGenerator myGenerator;
    private readonly ConcurrentDictionary<Type, Type> myTypesCache = new ConcurrentDictionary<Type, Type>();
    private readonly ConcurrentDictionary<Type, SortedList<MethodInfo, DynamicMethod>> myAdaptersCache = new ConcurrentDictionary<Type, SortedList<MethodInfo, DynamicMethod>>();

    private sealed class TokenComparer : IComparer<MethodInfo>
    {
      public static IComparer<MethodInfo> Instance { get; } = new TokenComparer();
      public int Compare(MethodInfo? x, MethodInfo? y) => (x?.MetadataToken ?? -1).CompareTo(y?.MetadataToken ?? -1);
    }

    public ProxyGeneratorCache(ProxyGenerator generator)
    {
      myGenerator = generator;
    }

    public Type CreateType(Type interfaceType)
    {
      return myTypesCache.GetOrAdd(interfaceType, type => myGenerator.CreateType(interfaceType));
    }

    public DynamicMethod CreateAdapter(Type selfType, MethodInfo method)
    {
      var methods = myAdaptersCache.GetOrAdd(selfType, type => new SortedList<MethodInfo, DynamicMethod>(TokenComparer.Instance));
      lock (methods)
      {
        if (methods.TryGetValue(method, out var adapter))
          return adapter;

        adapter = myGenerator.CreateAdapter(selfType, method);
        methods[method] = adapter;
        return adapter;
      }
    }
  }
}