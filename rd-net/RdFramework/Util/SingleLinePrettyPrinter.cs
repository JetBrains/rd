using System;

namespace JetBrains.Rd.Util
{
  public class SingleLinePrettyPrinter : PrettyPrinter
  {
    public SingleLinePrettyPrinter()
      : base(1000)
    {
      
    }
    public override string ToString()
    {
      var baseRes = base.ToString();
      return baseRes.Replace(Environment.NewLine, "");
    }
  }
}