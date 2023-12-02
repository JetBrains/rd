#if !NET35
using System;
using System.Diagnostics;
using System.Globalization;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Util;
using NUnit.Framework;

namespace Test.Lifetimes.Diagnostics;

public class ProcessWatchdogTest : LifetimesTestBase
{
  [Test]
  public Task TestWithSleepingProcess() => DoTest(StartSleepingProcess, true);

  [Test]
  public Task TestWithProcessReturning259() => DoTest(() => GetTerminatedProcess(259), false);

  private static Task DoTest(Func<Process> processCreator, bool assertAlive) => Lifetime.UsingAsync(async lt =>
  {
    var process = lt.Bracket(
      processCreator,
      p =>
      {
        if (!p.HasExited) p.Kill();
        p.Dispose();
      });

    var tcs = new TaskCompletionSource<Unit>();
    var options = new ProcessWatchdog.Options(process.Id, lt)
    {
      BeforeProcessKill = () => tcs.SetResult(Unit.Instance),
      KillCurrentProcess = () => { }
    };

    ProcessWatchdog.StartWatchdogForPid(options);

    var timeForReliableDetection = ProcessWatchdog.DELAY_BEFORE_RETRY * 2;
    var task = tcs.Task;
    if (assertAlive)
    {
      await Task.Delay(timeForReliableDetection, lt);
      Assert.IsFalse(process.HasExited, "Process should not be exited.");
      Assert.IsFalse(task.IsCompleted, "Watchdog should not be triggered.");
    }

    if (!process.HasExited) process.Kill();

    if (await Task.WhenAny(task, Task.Delay(timeForReliableDetection, lt)) != task)
    {
      Assert.Fail($"Termination of process {process.Id} wasn't detected during the timeout.");
    }

    var exs = Assert.Throws<AggregateException>(TestLogger.ExceptionLogger.ThrowLoggedExceptions).InnerExceptions;
    Assert.IsTrue(
      exs.All(e => e.Message.Contains($"Parent process PID:{process.Id} has quit, killing ourselves via Process.Kill")),
      $"No expected data in some of the exceptions: {string.Join("\n", exs.Select(e => e.Message))}");
  });

  private Process StartSleepingProcess()
  {
    var startInfo = RuntimeInfo.IsRunningUnderWindows
      ? new ProcessStartInfo("cmd.exe", "/c ping 127.0.0.1 -n 30")
      : new ProcessStartInfo("sleep", "30");
    startInfo.UseShellExecute = false;
    startInfo.CreateNoWindow = true;
    startInfo.RedirectStandardOutput = true;
    startInfo.RedirectStandardError = true;

    var logger = Log.GetLog<ProcessWatchdogTest>();
    var process = Process.Start(startInfo)!;
    process.ErrorDataReceived += (_, args) => logger.Warn($"[{process.Id}] {args.Data}");
    process.OutputDataReceived += (_, args) => logger.Info($"[{process.Id}] {args.Data}");
    process.Exited += (_, _) => logger.Info($"[{process.Id}] Exited with code: {process.ExitCode}");

    return process;
  }

  private Process GetTerminatedProcess(int exitCode)
  {
    var process = RuntimeInfo.IsRunningUnderWindows
      ? Process.Start(new ProcessStartInfo("cmd.exe", $"/c exit {exitCode.ToString(CultureInfo.InvariantCulture)}")
      {
        WindowStyle = ProcessWindowStyle.Hidden
      })
      : Process.Start("/usr/bin/sh", $"-c \"exit {exitCode.ToString(CultureInfo.InvariantCulture)}\"");
    process!.WaitForExit();
    Assert.AreEqual(exitCode, process.ExitCode);
    return process;
  }
}
#endif