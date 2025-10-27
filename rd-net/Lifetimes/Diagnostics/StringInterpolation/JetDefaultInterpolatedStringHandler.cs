using System;
using System.Runtime.CompilerServices;
using System.Text;

namespace JetBrains.Diagnostics.StringInterpolation;

internal struct JetDefaultInterpolatedStringHandler
{
  private StringBuilder? myBuffer;

  public JetDefaultInterpolatedStringHandler(int literalLength, int formattedCount)
  {
    myBuffer = new StringBuilder(literalLength + formattedCount * 11);
  }

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public string ToStringAndClear()
  {
    var result = ToString();
    myBuffer = null;
    return result;
  }

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendLiteral(string? value)
  {
    myBuffer ??= new StringBuilder();

    myBuffer.Append(value);
  }

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  private string? ToStringFormatted<T>(T value, string? format)
  {
    // ReSharper disable once MergeCastWithTypeCheck
    return value is IFormattable
      ? ((IFormattable)value).ToString(format, null) // explicit cast is required to prevent boxing
      : value?.ToString();
  }

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendFormatted<T>(T value, string? format)
  {
    myBuffer ??= new StringBuilder();

    string? s = ToStringFormatted(value, format);
    if (s is not null)
    {
      AppendLiteral(s);
    }
  }

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendFormatted<T>(T value, int alignment)
  {
    myBuffer ??= new StringBuilder();

    AppendFormatted(value, alignment, format: null);
  }

  [MethodImpl(MethodImplOptions.AggressiveInlining)]
  public void AppendFormatted<T>(T value)
  {
    myBuffer ??= new StringBuilder();

    AppendFormatted(value, format: null);
  }

  public override string ToString()
  {
    return myBuffer?.ToString() ?? "";
  }

  public void AppendFormatted<T>(T value, int alignment, string? format)
  {
    myBuffer ??= new StringBuilder();

    if (alignment == 0)
    {
      AppendFormatted(value, format);
      return;
    }

    if (alignment < 0)
    {
      alignment = -alignment;
      
      var oldPos = myBuffer.Length;
      AppendFormatted(value, format);
      var newPos = myBuffer.Length;
      
      var written = newPos - oldPos;
      var padding = alignment - written;
      if (padding > 0)
        myBuffer.Append(' ', padding);
    }
    else
    {
      var val = ToStringFormatted(value, format);
      if (val == null)
      {
        myBuffer.Append(' ', alignment);
        return;
      }

      var padding = alignment - val.Length;
      if (padding > 0)
        myBuffer.Append(' ', padding);

      myBuffer.Append(val);
    }
  }
}