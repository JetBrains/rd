namespace JetBrains.Util
{
  public static class NullableEx
  {
    public static T? ToNullable<T> (this T t) where T : struct
    {
      return t;
    }
  }
}