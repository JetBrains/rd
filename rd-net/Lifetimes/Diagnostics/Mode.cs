using System;

namespace JetBrains.Diagnostics;

public static class Mode
{
  /// <summary>
  /// Whether asserts are enabled. Can be configured by setting JET_MODE_ASSERT = true/false in Current App Domain storage.
  /// </summary>
  public static readonly bool Assertion;

  /// <summary>
  /// True if JET_MODE_ASSERT wasn't specified at the moment of static constructor invocation and default value was
  /// used instead.
  /// </summary>
  public static readonly bool UndefinedAssertion;

  static Mode()
  {
    var data = AppDomain.CurrentDomain.GetData("JET_MODE_ASSERT") as bool?;
    Assertion = data == true;
    UndefinedAssertion = !data.HasValue;
  }
}