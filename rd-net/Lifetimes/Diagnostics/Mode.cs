using System;

namespace JetBrains.Diagnostics;

public static class Mode
{
  public static readonly bool Assertion = AppDomain.CurrentDomain.GetData("JET_MODE_ASSERT") as bool? == true;
}