using System.Threading;
using JetBrains.Diagnostics;

namespace Test.Lifetimes
{
  public class Linearization
  {
    private readonly object myLock = new object();
    
    private int myNextId;

    //could be disabled for some reasons
    private bool myEnabled;

    public void Enable()
    {
      lock (myLock)
      {
        myEnabled = true;
        Monitor.PulseAll(myLock);
      }
    }

    public void Disable()
    {
      lock (myLock)
      {
        myEnabled = false;
        Monitor.PulseAll(myLock);
      }
    }

    public void Point(int id)
    {
      Assertion.Require(id >= 0, "{0} >= 0", id);

      lock (myLock)
      {
        while (myEnabled && id > myNextId)
          Monitor.Wait(myLock, 1000);

        //could break waiting
        if (!myEnabled) return;
        
        Assertion.Require(id <= myNextId, "Point {0} already set, nextId={1}", id, myNextId);
        myNextId++; 
        
        Monitor.PulseAll(myLock);
      }
    }

    public void Reset()
    {
      lock (myLock)
      {
        myNextId = 0;
        Monitor.PulseAll(myLock);
      }
    }
  }
}