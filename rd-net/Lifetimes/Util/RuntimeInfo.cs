using System;
using System.Reflection;
using JetBrains.Annotations;

namespace JetBrains.Util
{
  public static class RuntimeInfo
  {
    [CanBeNull]
    public static Version CurrentMonoVersion;
    public static bool IsRunningOnMono; 
    public static readonly bool IsRunningUnderWindows;

    static RuntimeInfo()
    {
      IsRunningUnderWindows =
#if NETSTANDARD
      System.Runtime.InteropServices.RuntimeInformation.IsOSPlatform(System.Runtime.InteropServices.OSPlatform.Windows);
#else
        Environment.OSVersion.Platform == PlatformID.Win32NT ||
        Environment.OSVersion.Platform == PlatformID.Win32S ||
        Environment.OSVersion.Platform == PlatformID.Win32Windows ||
        Environment.OSVersion.Platform == PlatformID.WinCE;
#endif
      var monoRuntimeType = Type.GetType("Mono.Runtime");
      if (monoRuntimeType != null)
      {
        IsRunningOnMono = true;
#if !NETSTANDARD
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
#endif
      }
      else
      {
        IsRunningOnMono = false;
      }
    }
    
#if NET35
    private static bool TryParseVersion(string input, out Version version)
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