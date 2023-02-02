using System;
using JetBrains.Diagnostics;

namespace JetBrains.Rd.Impl
{
  public struct BufferWindow
  {
    public delegate int Receiver(byte[] data, int offset, int size);
    
    public readonly byte[] Data;
    public int Lo;
    public int Hi; //exclusive

    public int Available => Hi - Lo;

    public void Clear()
    {
      Lo = Hi = 0;
    }
    
    public BufferWindow(int length) : this()
    {
      Lo = 0;
      Hi = 0;
      Data = new byte[length];
    }

    public void MoveTo(byte[] dst, int offset, int size)
    {
      Array.Copy(Data, Lo, dst, offset, size);
      Lo += size;
    }
    
    public bool Read(ref BufferWindow helper, Receiver receiver) => Read(ref helper, receiver, Data.Length - Hi);
    public bool Read(ref BufferWindow helper, Receiver receiver, int size)
    {
      var hi = Hi + size;
      if (Mode.IsAssertion) Assertion.Assert(hi <= Data.Length, "hi <= Data.Length");
      
      while (Hi < hi)
      {
        if (Mode.IsAssertion) Assertion.Assert(helper.Hi >= helper.Lo, "helper.Hi >= helper.Lo");

        if (helper.Available > 0)
        {
          var copylen = Math.Min(hi - Hi, helper.Available);

          helper.MoveTo(Data, Hi, copylen);
          Hi += copylen;
        }
        else
        {
          if (helper.Hi == helper.Data.Length)
          {
            helper.Hi = helper.Lo = 0;              
          }
          var receiverAvailable =  receiver(helper.Data, helper.Hi, helper.Data.Length - helper.Hi);
          if (receiverAvailable == 0)
            return false;
          
          helper.Hi += receiverAvailable;
        }
      }
      if (Mode.IsAssertion) Assertion.Assert(Hi == hi, "lo == hi");
      return true;
    }
  }
}