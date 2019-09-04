using System;
using System.Threading;

namespace JetBrains.Threading
{
  #if !NET35
  public struct ScopedAsyncLocal<T> : IDisposable 
  {
    private readonly AsyncLocal<T> myAsyncLocal;
    private readonly T myOldValue;

    public ScopedAsyncLocal(AsyncLocal<T> asyncLocal, T value)
    {
      myAsyncLocal = asyncLocal;
      myOldValue = asyncLocal.Value;
      asyncLocal.Value = value;
    }

    public void Dispose()
    {
      myAsyncLocal.Value = myOldValue;
    }
  }
  #endif
}