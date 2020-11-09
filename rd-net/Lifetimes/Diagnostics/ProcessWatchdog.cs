using System;
using System.ComponentModel;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Interop;
using JetBrains.Lifetimes;
using JetBrains.Util;

namespace JetBrains.Diagnostics
{
  /// <summary>
  /// Watchdog that automatically terminates current process if some other process exits.
  /// </summary>
  [PublicAPI] public static class ProcessWatchdog
  {
    private static readonly ILog ourLogger = Log.GetLog(nameof(ProcessWatchdog));
    private const int DELAY_BEFORE_RETRY = 1000;
    private const int ERROR_INVALID_PARAMETER = 87;

    public static void StartWatchdogForPidEnvironmentVariable(string envVarName)
    {
      StartWatchdogForPidEnvironmentVariable(envVarName, Lifetime.Eternal);
    }

    public static void StartWatchdogForPidEnvironmentVariable(string envVarName, Lifetime lifetime, TimeSpan? gracefulShutdownPeriod = null)
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
      StartWatchdogForPid(parentProcessPid, lifetime, gracefulShutdownPeriod);
    }

    public static void StartWatchdogForPid(int pid)
    {
      StartWatchdogForPid(pid, Lifetime.Eternal);
    }

    public static void StartWatchdogForPid(int pid, Lifetime lifetime, TimeSpan? gracefulShutdownPeriod = null)
    {
      var watchThread = new Thread(() =>
      {
        ourLogger.Info($"Monitoring parent process PID:{pid}");

        var useWinApi = true;

        while (true)
        {
          if (lifetime.IsNotAlive)
          {
            ourLogger.Info($"Monitoring of process {pid} stopped by lifetime termination");
            return;
          }
          if (!ProcessExists(pid, ref useWinApi))
          {
            var exitMsg = $"Parent process PID:{pid} has quit, killing ourselves via Process.Kill";
            try
            {
              LogLog.Error(exitMsg);
              ourLogger.Error(exitMsg);
              
              if (gracefulShutdownPeriod.HasValue)
              {
                var waitingMessage = $"Waiting for grace period: {gracefulShutdownPeriod.Value}";
                LogLog.Info(waitingMessage);
                ourLogger.Info(waitingMessage);
                
                Thread.Sleep(gracefulShutdownPeriod.Value);
              }
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
    private static extern int kill(int pid, int sig);

    public static bool ProcessExists(int pid, ref bool useWinApi)
    {
      try
      {
        if (!RuntimeInfo.IsRunningUnderWindows)
        {
          // Note: this implementation will return false in case of EPERM
          return kill(pid, 0) == 0;
        }

        if (!useWinApi)
          return ProcessExists_SystemDiagnostics(pid);
        
        try
        {
          return ProcessExists_Windows(pid);
        }
        catch (Win32Exception e)
        {
          // OpenProcess may fail with ERROR_ACCESS_DENIED and we don't know why
          // so fallback to slow implementation via System.Diagnostics.Process
          useWinApi = false;
          ourLogger.Warn(e);
          return ProcessExists_SystemDiagnostics(pid);
        }
      }
      catch (Exception e)
      {
        ourLogger.Error(e);
        return false;
      }
    }

    private static bool ProcessExists_Windows(int pid)
    {
      var handle = IntPtr.Zero;
      try
      {
        handle = Kernel32.OpenProcess(ProcessAccessRights.PROCESS_QUERY_LIMITED_INFORMATION, false, pid);
        if (handle == IntPtr.Zero)
        {
          var errorCode = Marshal.GetLastWin32Error();
          return errorCode == ERROR_INVALID_PARAMETER ? false : throw new Win32Exception(errorCode); // ERROR_INVALID_PARAMETER means that process doesn't exist
        }

        return Kernel32.GetExitCodeProcess(handle, out var exitCode)
          ? exitCode == ProcessExitCode.STILL_ALIVE
          : throw new Win32Exception();
      }
      finally
      {
        if (handle != IntPtr.Zero)
          Kernel32.CloseHandle(handle);
      }
    }

    private static bool ProcessExists_SystemDiagnostics(int pid)
    {
      return !Process.GetProcessById(pid).HasExited;
    }
  }
}