using System.Collections.Generic;
using JetBrains.Diagnostics;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class SerializersTest
  {
    [Test]
    public unsafe void TestReadRName()
    {
      var testNames = new List<RName> {
        RName.Empty,
        RName.Empty.Sub("", ""),
        RName.Empty.Sub("abc", ""),
        RName.Empty.Sub("some very long string with numbers 1234567890 and strange \u0d78\u0bf5 symbols", ""),
        RName.Empty.Sub("abc", "").Sub("asdf123", "::"),
        RName.Empty.Sub("arbitrary", "").Sub("separators with", " spaces and $&*@ symbols "),
        RName.Empty.Sub("a", "").Sub("b", ".").Sub("c", "::").Sub("d", "$").Sub("e", "_").Sub("$", ".").Sub("[]", "::"),
        RName.Empty.Sub("", "").Sub("", "").Sub("", "").Sub("", "").Sub("", "")
      };

      using var cookie = UnsafeWriter.NewThreadLocalWriter();
      var writer = cookie.Writer;
      foreach (var name in testNames)
      {
        var start = writer.Ptr;
        ExtCreatedUtils.WriteRName(writer, name);
        var reader = UnsafeReader.CreateReader(start, 1000);
        var value = ExtCreatedUtils.ReadRName(reader);

        Assert.True(RNameEquals(name, value), $"expected \"{name}\" but got \"{value}\"");
      }
    }
    
    private bool RNameEquals(RName a, RName b)
    {
      if (a == RName.Empty || b == RName.Empty)
        return a == b;
      if (a.LocalName.ToString() != b.LocalName.ToString() || a.Separator != b.Separator)
        return false;
      if (!(a.Parent is RName aParent) || !(b.Parent is RName bParent))
        return a == b;
      return RNameEquals(aParent, bParent);
    }
  }
}