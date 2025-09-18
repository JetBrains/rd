using System;
using System.Diagnostics.CodeAnalysis;
using System.Reflection;
using JetBrains.Annotations;

using System.Runtime.InteropServices;

namespace JetBrains.Util
{
  public static class RuntimeInfo
  {
    public static Version? CurrentMonoVersion;
    public static readonly bool IsRunningOnMono; 
    public static readonly bool IsRunningUnderWindows;
    public static readonly bool IsRunningOnCore;
    public static readonly bool IsUnalignedAccessAllowed;

    static RuntimeInfo()
    {
      IsRunningUnderWindows =
        Environment.OSVersion.Platform == PlatformID.Win32NT ||
        Environment.OSVersion.Platform == PlatformID.Win32S ||
        Environment.OSVersion.Platform == PlatformID.Win32Windows ||
        Environment.OSVersion.Platform == PlatformID.WinCE;
      
      IsRunningOnCore = typeof(string).Assembly.FullName.StartsWith(
        "System.Private.CoreLib",
        StringComparison.Ordinal);

      var monoRuntimeType = Type.GetType("Mono.Runtime");
      if (monoRuntimeType != null)
      {
        IsRunningOnMono = true;

        var displayName = monoRuntimeType.GetMethod("GetDisplayName", BindingFlags.NonPublic | BindingFlags.Static);
        if (displayName == null) return;
        var versionString = displayName.Invoke(null, null).ToString();
        var input = versionString.Split(' ')[0];
        if (Version.TryParse(input, out var version))
        {
          CurrentMonoVersion = version;
        }

      }
      else
      {
        IsRunningOnMono = false;
      }

      IsUnalignedAccessAllowed =
        RuntimeInformation.ProcessArchitecture != Architecture.Arm
        ;
    }

  }
}