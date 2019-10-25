using demo;
using JetBrains.Rd.Tasks;

namespace Test.RdCross
{
    // ReSharper disable once UnusedMember.Global
    internal class CrossTestCsClientRdCall : CrossTestCsClientBase
    {
        protected override void Start(string[] args)
        {
            Queue(() =>
            {
                var demoModel = new DemoModel(ModelLifetime, Protocol);
                
                demoModel.Call.Set((lifetime, c) => RdTask<string>.Successful(c.ToString()));

                demoModel.Callback.Start("Csharp");
            });
        }
    }
}