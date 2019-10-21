using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class PolymorphicScalarTest : RdReflectionTestBase
  {
    [Test]
    public void TestClass()
    {
      WithExts<TestExt>((c, s) =>
      {
        c.Class.Value = new ProjectFolderDescriptor("ProjectFolder");
        Assert.AreEqual(c.Class.Value.GetType(), s.Class.Value.GetType());
        Assert.AreEqual(c.Class.Value.Name, s.Class.Value.Name);
        Assert.AreNotSame(c.Class.Value, s.Class.Value);
      });
    }

    [Test]
    public void TestArrays()
    {
      WithExts<TestExt>((c, s) =>
      {
        c.Array.Value = new ProjectItemDescriptor[]{ new ProjectFolderDescriptor("ProjectFolder"), new ProjectFileDescriptor("ProjectFile") };
        Assert.AreEqual(c.Array.Value[0].GetType(), s.Array.Value[0].GetType());
        Assert.AreEqual(c.Array.Value[0].Name, s.Array.Value[0].Name);
        Assert.AreEqual(c.Array.Value[1].Name, s.Array.Value[1].Name);
        Assert.AreNotSame(c.Array.Value, s.Array.Value);
      });
    }

    [Test]
    public void TestInterface()
    {
      WithExts<TestExt>((c, s) =>
      {
        c.Interface.Value = new ProjectItemDescriptor("ProjectItem");
        Assert.AreEqual(c.Interface.Value.GetType(), s.Interface.Value.GetType());
        Assert.AreEqual(c.Interface.Value.Name, s.Interface.Value.Name);
        Assert.AreNotSame(c.Interface.Value, s.Interface.Value);
      });
    }

    public override void SetUp()
    {
      base.SetUp();
      TestRdTypesCatalog.Register<ProjectItemDescriptor>();
      TestRdTypesCatalog.Register<ProjectFileDescriptor>();
      TestRdTypesCatalog.Register<ProjectFolderDescriptor>();
    }


    [RdExt]
    public class TestExt : RdExtReflectionBindableBase
    {
      public RdProperty<IProjectItemDescriptor> Interface { get; }
      public RdProperty<ProjectItemDescriptor> Class { get; }
    }

    [RdScalar] // not required
    public interface IProjectItemDescriptor
    {
      string Name { get; }
    }

    [RdScalar] // not required
    public class ProjectItemDescriptor : IProjectItemDescriptor
    {
      public ProjectItemDescriptor(string name)
      {
        Name = name;
      }

      public string Name { get; }
    }

    [RdScalar] // not required
    public class ProjectFileDescriptor : ProjectItemDescriptor
    {
      public ProjectFileDescriptor() : base(null)
      {
      }

      public ProjectFileDescriptor(string name) : base(name)
      {
      }
    }

    [RdScalar] // not required
    public class ProjectFolderDescriptor : ProjectItemDescriptor
    {
      public ProjectFolderDescriptor() : base(null)
      {
      }

      public ProjectFolderDescriptor(string name) : base(name)
      {
      }
    }
  }
}