#if !NET35
using System;
using System.Diagnostics;
using System.Globalization;
using System.Threading.Tasks;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Util;
using NUnit.Framework;

namespace Test.Lifetimes.Diagnostics;

public class ProcessWatchdogTest
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
      Assert.IsFalse(task.IsCompleted);
    }

    if (!process.HasExited) process.Kill();

    if (await Task.WhenAny(task, Task.Delay(timeForReliableDetection, lt)) != task)
    {
      Assert.Fail($"Termination of process {process.Id} wasn't detected during the timeout.");
    }
  });

  private Process StartSleepingProcess()
  {
    if (RuntimeInfo.IsRunningUnderWindows)
    {
      return Process.Start(new ProcessStartInfo("cmd.exe", "/c timeout 30")
      {
        WindowStyle = ProcessWindowStyle.Hidden
      });
    }

    return Process.Start("sleep", "30");
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