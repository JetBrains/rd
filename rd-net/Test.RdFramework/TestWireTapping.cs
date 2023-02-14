using System;
using System.IO;
using JetBrains.Lifetimes;
using Test.RdFramework.Components;

namespace Test.RdFramework;

public class TestWireTapping : IDisposable
{
  private readonly StreamWriter myWriter;
  private LifetimeDefinition myLifetimeDefinition;

  public TestWireTapping(string filename, TestWire clientWire, TestWire serverWire)
  {
    myWriter = new StreamWriter(filename);
    myLifetimeDefinition = new LifetimeDefinition();
    myLifetimeDefinition.Lifetime.OnTermination(myWriter);
    clientWire.OnTransmit.Advise(myLifetimeDefinition.Lifetime, bytes => myWriter.WriteLine("Client: " + BitConverter.ToString(bytes)));
    serverWire.OnTransmit.Advise(myLifetimeDefinition.Lifetime, bytes => myWriter.WriteLine("Server: " + BitConverter.ToString(bytes)));
  }

  public void Dispose()
  {
    myLifetimeDefinition.Terminate();
  }
}