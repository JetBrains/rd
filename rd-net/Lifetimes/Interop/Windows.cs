using System;
using System.Runtime.InteropServices;

namespace JetBrains.Interop
{
    internal static class Kernel32
    {
        private const string DllName = "kernel32.dll";
        
        [DllImport(DllName, SetLastError = true)]
        public static extern IntPtr OpenProcess(
            [In] ProcessAccessRights dwDesiredAccess,
            [In] bool bInheritHandle,
            [In] int dwProcessId);
    
        [DllImport(DllName, SetLastError = true)]
        public static extern bool GetExitCodeProcess(
            [In] IntPtr hProcess,
            [Out] out ProcessExitCode lpExitCode
        );
        
        [DllImport(DllName, SetLastError = true)]
        public static extern bool CloseHandle(
            [In] IntPtr handle
        );
    }
    
    [Flags]
    internal enum ProcessAccessRights
    {
        DELETE = 0x10000,
        READ_CONTROL = 0x20000,
        SYNCHRONIZE = 0x100000,
        WRITE_DAC = 0x40000,
        WRITE_OWNER = 0x80000,
        PROCESS_CREATE_PROCESS = 0x80,
        PROCESS_CREATE_THREAD = 0x2,
        PROCESS_DUP_HANDLE = 0x40,
        PROCESS_QUERY_INFORMATION = 0x400,
        PROCESS_QUERY_LIMITED_INFORMATION = 0x1000,
        PROCESS_SET_INFORMATION = 0x200,
        PROCESS_SET_QUOTA = 0x100,
        PROCESS_SUSPEND_RESUME = 0x800,
        PROCESS_TERMINATE = 0x1,
        PROCESS_VM_OPERATION = 0x8,
        PROCESS_VM_READ = 0x10,
        PROCESS_VM_WRITE = 0x20,
    }

    internal enum ProcessExitCode
    {
        STILL_ALIVE = 259
    }
}