using NUnit.Framework;

namespace Test.RdFramework.Reflection;

/// <summary>
/// Container class for test with different args counts
/// </summary>
[TestFixture]
public partial class ProxyGeneratorCalls
{
  /// <summary>
  /// If you've got compilation error here, ensure that source generators support is active.
  /// Test07 is a generated test from Test.RdFramework.Generator project.
  /// </summary>
  const string n = nameof(Test07);
}