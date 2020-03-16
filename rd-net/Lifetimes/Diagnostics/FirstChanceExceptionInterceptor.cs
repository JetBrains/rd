using System;
using System.Collections;
using JetBrains.Annotations;
using JetBrains.Util;

namespace JetBrains.Diagnostics
{
  /// <summary>
  /// Facilitates runtime diagnostics when exception arises. In a current thread scope, put diagnostic information in exception if it arises somewhere in call stack.
  /// All information put will be available with ',' separator under <see cref="ExceptionDataKey"/> when you print exception
  /// 
  /// Usage:
  /// <code>
  /// using (new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo(your_diagnostic_information))
  /// {
  ///     you_arbitrary_code
  /// }
  /// </code>    
  /// </summary>
  public static class FirstChanceExceptionInterceptor
  {
    [ThreadStatic] private static Stack ourThreadLocalDebugInfo;

    public const string ExceptionDataKey = "ThreadLocalDebugInfo"; 
    
    static FirstChanceExceptionInterceptor()
    {
      
#if !NET35
      AppDomain.CurrentDomain.FirstChanceException += (sender, args) =>
      {
        var info = string.Join("\n -> ", GetThreadLocalDebugInfo());
        if (!string.IsNullOrEmpty(info) && !args.Exception.Data.Contains(ExceptionDataKey))
        {
          args.Exception.Data[ExceptionDataKey] = info;
        }
      };
#endif
    }
    
    public struct ThreadLocalDebugInfo : IDisposable
    {      
      public ThreadLocalDebugInfo(object debugInfo)
      {
        if (ourThreadLocalDebugInfo == null)
          ourThreadLocalDebugInfo = new Stack();
        
        ourThreadLocalDebugInfo.Push(debugInfo);
      }

      public void Dispose()
      {
        ourThreadLocalDebugInfo?.Pop();
      }
    }

    [NotNull]
    private static object[] GetThreadLocalDebugInfo()
    {
      var info = ourThreadLocalDebugInfo;
      if (info == null || info.Count == 0) 
        return EmptyArray.GetInstance<object>();
      
      return info.ToArray();
    }
  }
  
}