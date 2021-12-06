// Licensed to the .NET Foundation under one or more agreements.
// The .NET Foundation licenses this file to you under the MIT license.

// This was copied from https://github.com/dotnet/runtime/blob/fedf64f5688246b73505537c4dd81fc0afb57ddb/src/libraries/System.Private.CoreLib/src/System/Runtime/CompilerServices/CallerArgumentExpressionAttribute.cs
// and updated to have the scope of the attributes be internal.

// ReSharper disable once CheckNamespace
namespace System.Runtime.CompilerServices
{
  [AttributeUsage(AttributeTargets.Parameter)]
  internal sealed class CallerArgumentExpressionAttribute : Attribute
  {
    public CallerArgumentExpressionAttribute(string parameterName)
    {
      ParameterName = parameterName;
    }

    public string ParameterName { get; }
  }
}