using System;
using System.IO;

namespace Test.RdCross.Util
{
    public static class FileSystem
    {
        public static bool IsFileReady(string filename)
        {
            try
            {
                using (FileStream inputStream = File.Open(filename, FileMode.Open, FileAccess.Read, FileShare.None))
                    return true;
            }
            catch (Exception)
            {
                return false;
            }
        }
        
        public static string RdTmpDir => Path.Combine(Path.GetTempPath(), "rd");

        internal static string PortFile => Path.Combine(RdTmpDir, "port.txt");
        internal static string PortFileClosed => Path.Combine(RdTmpDir, "port.txt.closed");
    }
}