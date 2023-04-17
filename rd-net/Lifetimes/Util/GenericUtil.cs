using System;

namespace JetBrains.Util;

public static class GenericUtil<T>
{
  public static readonly bool IsValueWriteAtomic = IsValueWriteAtomicCalculate();
  
  private static bool IsValueWriteAtomicCalculate()
  {
    var type = typeof (T);
    if (type.IsClass)
      return true;
    
    switch (Type.GetTypeCode(type))
    {
      case TypeCode.Boolean:
      case TypeCode.Char:
      case TypeCode.SByte:
      case TypeCode.Byte:
      case TypeCode.Int16:
      case TypeCode.UInt16:
      case TypeCode.Int32:
      case TypeCode.UInt32:
      case TypeCode.Single:
        return true;
      case TypeCode.Int64:
      case TypeCode.UInt64:
      case TypeCode.Double:
        return IntPtr.Size == 8;
      default:
        return false;
    }
  }
}