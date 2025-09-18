using System.Runtime.CompilerServices;

namespace JetBrains.Diagnostics.StringInterpolation;

[InterpolatedStringHandler]
public ref struct JetConditionalInterpolatedStringHandler
{
  private JetDefaultInterpolatedStringHandler myHandler;

  public bool IsEnabled { get; }

  public JetConditionalInterpolatedStringHandler(
    int literalLength,
    int formattedCount,
    bool condition,
    out bool isEnabled)
  {
    IsEnabled = isEnabled = !condition;
    myHandler = isEnabled ? new JetDefaultInterpolatedStringHandler(literalLength, formattedCount) : default;
  }

  public override string ToString() => IsEnabled ? myHandler.ToString() : "";
  
  public string ToStringAndClear()
  {
    if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
    return myHandler.ToStringAndClear();
  }

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendLiteral(string value)
  {
    if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
    myHandler.AppendLiteral(value);
  }

  public void AppendFormatted<T>(T value)
  {
    if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
    myHandler.AppendFormatted(value);
  }

  public void AppendFormatted<T>(T value, string? format)
  {
    if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
    myHandler.AppendFormatted(value, format);
  }

  public void AppendFormatted<T>(T value, int alignment)
  {
    if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
    myHandler.AppendFormatted(value, alignment);
  }

  public void AppendFormatted<T>(T value, int alignment, string? format)
  {
    if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
    myHandler.AppendFormatted(value, alignment, format);
  }

  // Commented out, because the compiler will require System.Memory for a project that uses string interpolation 
  // public void AppendFormatted(ReadOnlySpan<char> value)
  // {
  //   if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
  //   myHandler.AppendFormatted(value);
  // }
  //
  // public void AppendFormatted(ReadOnlySpan<char> value, int alignment = 0, string? format = null)
  // {
  //   if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
  //   myHandler.AppendFormatted(value, alignment, format);
  // }

  public void AppendFormatted(string? value)
  {
    if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
    myHandler.AppendFormatted(value);
  }

  // ReSharper disable once MethodOverloadWithOptionalParameter
  public void AppendFormatted(string? value, int alignment = 0, string? format = null)
  {
    if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
    myHandler.AppendFormatted(value, alignment, format);
  }

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendFormatted(object? value, int alignment = 0, string? format = null)
  {
    if (Mode.IsAssertion) Assertion.Assert(IsEnabled);
    myHandler.AppendFormatted(value, alignment, format);
  }
}