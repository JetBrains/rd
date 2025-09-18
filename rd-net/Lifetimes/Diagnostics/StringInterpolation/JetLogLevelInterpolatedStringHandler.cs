using System.Runtime.CompilerServices;

namespace JetBrains.Diagnostics.StringInterpolation;

[InterpolatedStringHandler]
public ref struct JetLogLevelInterpolatedStringHandler
{
  private JetConditionalInterpolatedStringHandler myHandler;

  public bool IsEnabled => myHandler.IsEnabled;

  public JetLogLevelInterpolatedStringHandler(
    int literalLength,
    int formattedCount,
    ILog logger,
    LoggingLevel logLevel,
    out bool isEnabled)
  {
    myHandler = new JetConditionalInterpolatedStringHandler(literalLength, formattedCount, !logger.IsEnabled(logLevel), out isEnabled);
  }

  public override string ToString() => IsEnabled ? myHandler.ToString() : "";
  
  public string ToStringAndClear() => myHandler.ToStringAndClear();

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendLiteral(string value) => myHandler.AppendLiteral(value);
  
  public void AppendFormatted<T>(T value) => myHandler.AppendFormatted(value);
  public void AppendFormatted<T>(T value, string? format) => myHandler.AppendFormatted(value, format);
  public void AppendFormatted<T>(T value, int alignment) => myHandler.AppendFormatted(value, alignment);
  public void AppendFormatted<T>(T value, int alignment, string? format) => myHandler.AppendFormatted(value, alignment, format);

  // Commented out, because the compiler will require System.Memory for a project that uses string interpolation 
  // public void AppendFormatted(ReadOnlySpan<char> value) => myHandler.AppendFormatted(value);
  // public void AppendFormatted(ReadOnlySpan<char> value, int alignment = 0, string? format = null) => myHandler.AppendFormatted(value, alignment, format);

  public void AppendFormatted(string? value) => myHandler.AppendFormatted(value);
  public void AppendFormatted(string? value, int alignment = 0, string? format = null) => myHandler.AppendFormatted(value, alignment, format);

  public void AppendFormatted(object? value, int alignment = 0, string? format = null) => myHandler.AppendFormatted(value, alignment, format);
}