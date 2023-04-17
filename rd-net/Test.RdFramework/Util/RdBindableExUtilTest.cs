using System.Collections.Generic;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using NUnit.Framework;

namespace Test.RdFramework.Util;

[TestFixture]
public class RdBindableExUtilTest
{
  [Test]
  public void IsBindableTest()
  {
    var instance = new RdBindableTestClass();
    Assert.IsTrue(instance.IsBindable());
    Assert.IsTrue(new[] { instance }.IsBindable());
    Assert.IsTrue(new List<RdBindableTestClass> { instance }.IsBindable());

    IRdBindable bindable = instance;
    Assert.IsTrue(bindable.IsBindable());
    Assert.IsTrue(new[] { bindable }.IsBindable());
    Assert.IsTrue(new List<IRdBindable> { bindable }.IsBindable());
    
    object obj = instance;
    Assert.IsTrue(obj.IsBindable());
    Assert.IsTrue(new[] { obj }.IsBindable());
    Assert.IsTrue(new List<object> { obj }.IsBindable());

    var i = 0;
    Assert.IsFalse(i.IsBindable());
    Assert.IsFalse(new[] { i }.IsBindable());
    Assert.IsFalse(new List<int> { i }.IsBindable());
    Assert.IsFalse(new List<object> { i }.IsBindable());
  } 
  
  [Test]
  public void IsBindableFastTest()
  {
    Assert.IsTrue(RdBindableEx.FastIsBindable<RdBindableTestClass>.Value);
    Assert.IsTrue(RdBindableEx.FastIsBindable<List<RdBindableTestClass>>.Value);
    Assert.IsTrue(RdBindableEx.FastIsBindable<RdBindableTestClass[]>.Value);
    
    Assert.IsTrue(RdBindableEx.FastIsBindable<IRdBindable>.Value);
    Assert.IsTrue(RdBindableEx.FastIsBindable<List<IRdBindable>>.Value);
    Assert.IsTrue(RdBindableEx.FastIsBindable<IRdBindable[]>.Value);
    
    Assert.IsFalse(RdBindableEx.FastIsBindable<object>.Value);
    Assert.IsFalse(RdBindableEx.FastIsBindable<List<object>>.Value);
    Assert.IsFalse(RdBindableEx.FastIsBindable<object[]>.Value);
    
    Assert.IsFalse(RdBindableEx.FastIsBindable<int>.Value);
    Assert.IsFalse(RdBindableEx.FastIsBindable<List<int>>.Value);
    Assert.IsFalse(RdBindableEx.FastIsBindable<int[]>.Value);
  } 

  
  private class RdBindableTestClass : IRdBindable
  {
    public RName Location { get; }
    public IProtocol TryGetProto() { throw new System.NotImplementedException(); }

    public bool TryGetSerializationContext(out SerializationCtx ctx) { throw new System.NotImplementedException(); }

    public void Print(PrettyPrinter printer) { throw new System.NotImplementedException(); }

    public RdId RdId { get; set; }
    public void PreBind(Lifetime lf, IRdDynamic parent, string name) { throw new System.NotImplementedException(); }

    public void Bind() { throw new System.NotImplementedException(); }

    public void Identify(IIdentities identities, RdId id) { throw new System.NotImplementedException(); }
  }
}