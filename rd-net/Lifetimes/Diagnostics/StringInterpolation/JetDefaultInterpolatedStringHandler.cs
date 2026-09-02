using System;
using System.Runtime.CompilerServices;

namespace JetBrains.Diagnostics.StringInterpolation;

internal ref struct JetDefaultInterpolatedStringHandler
{
  private DefaultInterpolatedStringHandler myHandler;

  public JetDefaultInterpolatedStringHandler(int literalLength, int formattedCount)
  {
    myHandler = new DefaultInterpolatedStringHandler(literalLength, formattedCount);
  }

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public string ToStringAndClear() => myHandler.ToStringAndClear();

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendLiteral(string value) => myHandler.AppendLiteral(value);

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendFormatted<T>(T value, string? format) => myHandler.AppendFormatted(value, format);

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendFormatted<T>(T value, int alignment) => myHandler.AppendFormatted(value, alignment);

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendFormatted<T>(T value) => myHandler.AppendFormatted(value);

  public override string ToString() => myHandler.ToString();

  public void AppendFormatted<T>(T value, int alignment, string? format) => myHandler.AppendFormatted(value, alignment, format);
}