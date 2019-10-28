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
        c.Interface.Value = new ProjectFileDescriptor("ProjectItem");
        Assert.AreEqual(c.Interface.Value.GetType(), s.Interface.Value.GetType());
        Assert.AreEqual(c.Interface.Value.Name, s.Interface.Value.Name);
        Assert.AreNotSame(c.Interface.Value, s.Interface.Value);
      });
    }

    [Test]
    public void TestNestedNull()
    {
      WithExts<TestExt>((c, s) =>
      {
        c.Interface.Value = new ProjectFileDescriptor("ProjectItem");
        Assert.AreEqual(c.Interface.Value.GetType(), s.Interface.Value.GetType());
        Assert.AreEqual(c.Interface.Value.Name, s.Interface.Value.Name);
        Assert.AreNotSame(c.Interface.Value, s.Interface.Value);
      });
    }



    public override void SetUp()
    {
      base.SetUp();
      TestRdTypesCatalog.AddType(typeof(ProjectItemDescriptor));
      TestRdTypesCatalog.AddType(typeof(ProjectFileDescriptor));
      TestRdTypesCatalog.AddType(typeof(ProjectFolderDescriptor));
    }


    [RdExt]
    public class TestExt : RdExtReflectionBindableBase
    {
      public RdProperty<IProjectItemDescriptor> Interface { get; }
      internal RdProperty<ProjectItemDescriptor> Class { get; }
      public RdProperty<ProjectItemDescriptor[]> Array { get; }
    }



    [RdScalar] // not required
    public interface IProjectItemDescriptor
    {
      string Name { get; }
    }

    [RdScalar] // not required
    public abstract class ProjectItemDescriptor : IProjectItemDescriptor
    {
      protected ProjectItemDescriptor(string name)
      {
        Name = name;
      }

      public string Name { get; }
    }

    [RdScalar]
    public class ProjectInfo
    {
      public string Name;
      public string Path;
    }

    [RdScalar] // not required
    public class ProjectFileDescriptor : ProjectItemDescriptor
    {
      public ProjectInfo ProjectInfo;
      public ProjectFileDescriptor(string name) : base(name)
      {
      }
    }

    [RdScalar] // not required
    public class ProjectFolderDescriptor : ProjectItemDescriptor
    {
      public ProjectFolderDescriptor(string name) : base(name)
      {
      }
    }
  }
}