// Licensed to the .NET Foundation under one or more agreements.
// The .NET Foundation licenses this file to you under the MIT license.

// This was copied from https://github.com/dotnet/runtime/blob/57bfe474518ab5b7cfe6bf7424a79ce3af9d6657/src/libraries/System.Private.CoreLib/src/System/Runtime/CompilerServices/InterpolatedStringHandlerAttribute.cs
// and updated to have the scope of the attributes be internal.
namespace System.Runtime.CompilerServices
{
  /// <summary>Indicates the attributed type is to be used as an interpolated string handler.</summary>
  [AttributeUsage(AttributeTargets.Class | AttributeTargets.Struct, AllowMultiple = false, Inherited = false)]
  internal sealed class InterpolatedStringHandlerAttribute : Attribute
  {
    /// <summary>Initializes the <see cref="InterpolatedStringHandlerAttribute"/>.</summary>
    public InterpolatedStringHandlerAttribute() { }
  }
}