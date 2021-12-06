// Licensed to the .NET Foundation under one or more agreements.
// The .NET Foundation licenses this file to you under the MIT license.

// This was copied from https://github.com/dotnet/runtime/blob/57bfe474518ab5b7cfe6bf7424a79ce3af9d6657/src/libraries/System.Private.CoreLib/src/System/Runtime/CompilerServices/InterpolatedStringHandlerArgumentAttribute.cs
// and updated to have the scope of the attributes be internal.

namespace System.Runtime.CompilerServices
{
  /// <summary>Indicates which arguments to a method involving an interpolated string handler should be passed to that handler.</summary>
  [AttributeUsage(AttributeTargets.Parameter, AllowMultiple = false, Inherited = false)]
  internal sealed class InterpolatedStringHandlerArgumentAttribute : Attribute
  {
    /// <summary>Initializes a new instance of the <see cref="InterpolatedStringHandlerArgumentAttribute"/> class.</summary>
    /// <param name="argument">The name of the argument that should be passed to the handler.</param>
    /// <remarks><see langword="null"/> may be used as the name of the receiver in an instance method.</remarks>
    public InterpolatedStringHandlerArgumentAttribute(string argument) => Arguments = new string[] { argument };

    /// <summary>Initializes a new instance of the <see cref="InterpolatedStringHandlerArgumentAttribute"/> class.</summary>
    /// <param name="arguments">The names of the arguments that should be passed to the handler.</param>
    /// <remarks><see langword="null"/> may be used as the name of the receiver in an instance method.</remarks>
    public InterpolatedStringHandlerArgumentAttribute(params string[] arguments) => Arguments = arguments;

    /// <summary>Gets the names of the arguments that should be passed to the handler.</summary>
    /// <remarks><see langword="null"/> may be used as the name of the receiver in an instance method.</remarks>
    public string[] Arguments { get; }
  }
}