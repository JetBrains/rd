using System;
using System.ComponentModel;
using System.Diagnostics;
using JetBrains.Annotations;

namespace Test.RdCore
{
    public static class GeneralEx
    {
        [DebuggerStepThrough]
        [EditorBrowsable(EditorBrowsableState.Never)]
        public static T With<T>(this T control, [InstantHandle, NotNull] Action<T> action)
        {
            action(control);
            return control;
        }
    }
}