using System;
using System.Runtime.CompilerServices;

#pragma warning disable CheckNamespace

namespace JetBrains.Util
{
//  [Obsolete("Use the intrinsic constant MethodImplOptions::AggressiveInlining directly.")]
  public static class MethodImplAdvancedOptions
  {
    /// <summary>
    /// Corresponds to <code>MethodImplOptions.AggressiveInlining</code> value in 4.5 framework. This value is just swallowed in lower frameworks
    /// </summary>
//    [Obsolete("Use the intrinsic constant MethodImplOptions::AggressiveInlining directly.")]
    public const MethodImplOptions AggressiveInlining = (MethodImplOptions)256;
  }
}