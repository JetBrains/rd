using System;
using System.IO;

namespace JetBrains.Rd.Scripts
{
    public static class LineEndingUtil
    {
        public static string Detect(string path)
        {
            using (var fileStream = File.OpenRead(path))
            {
                char prevChar = '\0';
                for (int i = 0; i < 4000; i++)
                {
                    int b;
                    if ((b = fileStream.ReadByte()) == -1)
                        break;

                    char curChar = (char)b;
                    if (curChar == '\n' )
                    {
                        if (prevChar == '\r')
                            return "\r\n";
                        return  "\n";
                    }
                    prevChar = curChar;
                }
            }

            return Environment.NewLine;
        }
    }
}
