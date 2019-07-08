using System.IO;

namespace Test.RdCross.Util
{
    public static class FileSystem
    {
        static string RdTmpDir => Path.Combine(Path.GetTempPath(), "rd");

        internal static string PortFile => Path.Combine(RdTmpDir, "port.txt");
    }
}