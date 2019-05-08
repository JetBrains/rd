using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Threading;
using JetBrains.Util;

namespace JetBrains.Diagnostics
{
  public static class ProcessWatchdog
  {
    private static readonly ILog ourLogger = Log.GetLog(nameof(ProcessWatchdog));
    private const int DELAY_BEFORE_RETRY = 1000;

    public static void StartWatchdogForPidEnvironmentVariable(string envVarName)
    {
      var parentProcessPidString = Environment.GetEnvironmentVariable(envVarName);
      if (parentProcessPidString == null)
      {
        ourLogger.Error($"Environment variable '{envVarName}' is not defined => do not watch parent process to die");
        return;
      }
      if (!int.TryParse(parentProcessPidString, out var parentProcessPid))
      {
        ourLogger.Error($"Unable to parse int from environment variable '{envVarName}' => do not watch parent process to die");
        return;
      }
      StartWatchdogForPid(parentProcessPid);
    }

    public static void StartWatchdogForPid(int pid)
    {
      var watchThread = new Thread(() =>
      {
        ourLogger.Info($"Monitoring parent process PID:{pid}");

        while (true)
        {
          if (!ProcessExists(pid))
          {
            var exitMsg = $"Parent process PID:{pid} has quit, killing ourselves via Process.Kill";
            try
            {
              LogLog.Error(exitMsg);
              ourLogger.Error(exitMsg);
            }
            catch
            {
              // ignored
            }

            Process.GetCurrentProcess().Kill();
            return;
          }

          Thread.Sleep(DELAY_BEFORE_RETRY);
        }
      }) {IsBackground = true, Name = "WatchParentPid:" + pid};

      watchThread.Start();
    }

    [DllImport("libc", SetLastError = true)]
    public static extern int kill(int pid, int sig);

    private static bool ProcessExists(int pid)
    {
      try
      {
        if (!RuntimeInfo.IsRunningUnderWindows)
        {
          // Note: this implementation will return false in case of EPERM
          return kill(pid, 0) == 0;
        }

        var process = Process.GetProcessById(pid);
        return !process.HasExited;
      }
      catch
      {
        return false;
      }
    }
  }
}