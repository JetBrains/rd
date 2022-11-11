using System;
using System.Runtime.CompilerServices;

namespace JetBrains.Diagnostics;

public static class Mode
{
  /// <summary>
  /// Whether asserts are enabled. Can be configured by setting JET_MODE_ASSERT = true/false in Current App Domain storage.
  /// </summary>
  public static readonly bool IsAssertion;

  /// <summary>
  /// True if JET_MODE_ASSERT wasn't specified at the moment of static constructor invocation and default value was
  /// used instead.
  /// </summary>
  public static readonly bool IsAssertionUndefined;

  static Mode()
  {
    var data = AppDomain.CurrentDomain.GetData(ModeInitializer.AssertionEnabledVariableName) as bool?;
    IsAssertion = data == true;
    IsAssertionUndefined = !data.HasValue;
  }
}

public static class ModeInitializer
{
  public const string AssertionEnabledVariableName = "JET_MODE_ASSERT";

  /// <summary>
  /// Indirectly set the value of <see cref="Mode.IsAssertion"/> and evaluates the static constructor of <see cref="Mode"/> class.
  /// </summary>
  /// <returns>true if the assertion mode is properly defined and equal to the provided value<returns>
  public static bool Init(bool isAssertionEnabled)
  {
    if (AppDomain.CurrentDomain.GetData(AssertionEnabledVariableName) is not bool)
      AppDomain.CurrentDomain.SetData(AssertionEnabledVariableName, isAssertionEnabled);

    return !GetIsAssertionUndefined() && GetIsAssertion() == isAssertionEnabled;
  }

  /// <summary>
  /// Force touches <see cref="Mode"/> class and return the value of the current assertion enabled mode.
  /// </summary>
  /// <returns></returns>
  [MethodImpl(MethodImplOptions.NoInlining)]
  public static bool GetIsAssertion()
  {
    return Mode.IsAssertion;
  }

  /// <summary>
  /// Force touches <see cref="Mode"/> class and returns if the assertion mode was properly initialized from AppDomain
  /// config or default value was used instead
  /// </summary>
  /// <returns></returns>
  [MethodImpl(MethodImplOptions.NoInlining)]
  public static bool GetIsAssertionUndefined()
  {
    return Mode.IsAssertionUndefined;
  }
}