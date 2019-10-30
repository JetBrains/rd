#if NET35
namespace System
{
  public struct ValueTuple<T1>
  {
    public T1 Item1;

    public ValueTuple(T1 item1)
    {
      Item1 = item1;
    }
  }

  public struct ValueTuple<T1, T2>
  {
    public T1 Item1;

    public T2 Item2;

    public ValueTuple(T1 item1, T2 item2)
    {
      Item1 = item1;
      Item2 = item2;
    }
  }

  public struct ValueTuple<T1, T2, T3>
  {
    public T1 Item1;
    public T2 Item2;
    public T3 Item3;

    public ValueTuple(T1 item1, T2 item2, T3 item3)
    {
      Item1 = item1;
      Item2 = item2;
      Item3 = item3;
    }
  }

  public struct ValueTuple<T1, T2, T3, T4>
  {
    public T1 Item1;
    public T2 Item2;
    public T3 Item3;
    public T4 Item4;

    public ValueTuple(T1 item1, T2 item2, T3 item3, T4 item4)
    {
      Item1 = item1;
      Item2 = item2;
      Item3 = item3;
      Item4 = item4;
    }
  }

  public struct ValueTuple<T1, T2, T3, T4, T5>
  {
    public T1 Item1;
    public T2 Item2;
    public T3 Item3;
    public T4 Item4;
    public T5 Item5;

    public ValueTuple(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5)
    {
      Item1 = item1;
      Item2 = item2;
      Item3 = item3;
      Item4 = item4;
      Item5 = item5;
    }
  }

  public struct ValueTuple<T1, T2, T3, T4, T5, T6>
  {
    public T1 Item1;
    public T2 Item2;
    public T3 Item3;
    public T4 Item4;
    public T5 Item5;
    public T6 Item6;

    public ValueTuple(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6)
    {
      Item1 = item1;
      Item2 = item2;
      Item3 = item3;
      Item4 = item4;
      Item5 = item5;
      Item6 = item6;
    }
  }

  public struct ValueTuple<T1, T2, T3, T4, T5, T6, T7>
  {
    public T1 Item1;
    public T2 Item2;
    public T3 Item3;
    public T4 Item4;
    public T5 Item5;
    public T6 Item6;
    public T7 Item7;

    public ValueTuple(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7)
    {
      Item1 = item1;
      Item2 = item2;
      Item3 = item3;
      Item4 = item4;
      Item5 = item5;
      Item6 = item6;
      Item7 = item7;
    }
  }

  public struct ValueTuple<T1, T2, T3, T4, T5, T6, T7, TRest>
    where TRest : struct
  {
    public T1 Item1;
    public T2 Item2;
    public T3 Item3;
    public T4 Item4;
    public T5 Item5;
    public T6 Item6;
    public T7 Item7;
    public TRest Rest;

    public ValueTuple(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, TRest rest)
    {
      Item1 = item1;
      Item2 = item2;
      Item3 = item3;
      Item4 = item4;
      Item5 = item5;
      Item6 = item6;
      Item7 = item7;
      Rest = rest;
    }
  }
}
#endif