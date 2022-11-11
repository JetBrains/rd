using System;
using System.Diagnostics.CodeAnalysis;
using System.Reflection;
using JetBrains.Annotations;

#if !NET35
using System.Runtime.InteropServices;
#endif

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
#if !NET35
        if (Version.TryParse(input, out var version))
#else
        if (TryParseVersion(input, out var version))
#endif
        {
          CurrentMonoVersion = version;
        }

      }
      else
      {
        IsRunningOnMono = false;
      }

      IsUnalignedAccessAllowed =
#if NET35
        true
#else
        RuntimeInformation.ProcessArchitecture != Architecture.Arm
#endif
        ;
    }

#if NET35
    private static bool TryParseVersion(string input, [MaybeNullWhen(false)] out Version version)
    {
      if (string.IsNullOrEmpty(input))
      {
        version = null;
        return false;
      }
      
      try
      {
        version = new Version(input);
        return true;
      }
      catch
      {
        version = null;
        return false;
      }
    }
#endif
  }
}