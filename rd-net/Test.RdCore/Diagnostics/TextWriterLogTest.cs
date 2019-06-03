using System.IO;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.RdCore.Diagnostics
{
  public class TextWriterLogTest : RdCoreTestBase
  {
    [Test]
    public void TestHandlers()
    {
      var memoryStream = new MemoryStream();      
      var streamWriter = new StreamWriter(memoryStream);      
      
      var factory = new TextWriterLogFactory(streamWriter);

      var log1 = (factory.GetLog("category 1") as TextWriterLog).NotNull();
      var log2 = factory.GetLog("category 2");

      int extHandlersTriggered = 0;
      log1.Handlers += lmsg => { extHandlersTriggered++; };
      
      log1.Verbose("record 1 verbose");
      log1.Trace("record 1 trace");
      
      log2.Verbose("record 2 verbose");

      memoryStream.Seek(0, SeekOrigin.Begin);
      var streamReader = new StreamReader(memoryStream);
      var line1 = streamReader.ReadLine().NotNull();
      var line2 = streamReader.ReadLine().NotNull();
      
      Assert.True(line1.Contains("|V| category 1"));
      Assert.True(line1.Contains("| record 1"));
      
      Assert.True(line2.Contains("|V| category 2"));
      Assert.True(line2.Contains("| record 2"));
      
      
      Assert.AreEqual(1, extHandlersTriggered);
      Assert.Null(streamReader.ReadLine());
    }
    
    [Test]
    public void TestInFile()
    {
      var path = Path.GetTempFileName();
      TestLifetime.OnTermination(() => File.Delete(path));
      
      Lifetime.Using(lf =>
      {
        var factory = Log.CreateFileLogFactory(lf, path, append: false, enabledLevel: LoggingLevel.VERBOSE);
        var log1 = factory.GetLog("sample1");
        var log2 = factory.GetLog("sample2");

        log1.Info("info record");
        log2.Verbose("verbose record");
        log2.Trace("trace record"); //must be filtered
      });


      var lines = File.ReadAllLines(path);
      Assert.AreEqual(2, lines.Length);
      
      Assert.True(lines[0].Contains("|I| sample1"));
      Assert.True(lines[0].Contains("| info record"));
      
      Assert.True(lines[1].Contains("|V| sample2"));
      Assert.True(lines[1].Contains("| verbose record"));
    }
  }
}