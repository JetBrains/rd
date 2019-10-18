using System;
using System.Diagnostics;

namespace SampleGame.Common
{
  internal static class Util
  {
    public static bool Fork(string[] args)
    {
      if (args.Length > 0)
        return false;

      var dotnet = Process.GetCurrentProcess().MainModule.FileName;
      var startInfo = new ProcessStartInfo(dotnet, string.Join(" ", Environment.GetCommandLineArgs()) + " FORK")
      {
        UseShellExecute = true
      };
      Process.Start(startInfo);

      return true;
    }
  }
}