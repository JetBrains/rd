using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;

namespace Test.RdCross.Util
{
    public static class Ext
    {
        public static bool IsLocalChange<T>(this ISource<T> entity)
        {
            return ((RdReactiveBase) entity).IsLocalChange;
        }

        public static void PrintIfRemoteChange<T>([NotNull] this PrettyPrinter printer,
            [NotNull] ISource<T> entity,
            [NotNull] string entityName,
            [NotNull] params object[] values)
        {
            if (!entity.IsLocalChange())
            {
                printer.Println();
                "***".PrintEx(printer);
                printer.Println();
                (entityName + ":").PrintEx(printer);
                foreach (var value in values)
                {
                    value.PrintEx(printer);
                    printer.Println();
                }
            }
        }
    }
}