using System.Collections.Generic;
using JetBrains.Collections;
using NUnit.Framework;

namespace Test.Lifetimes.Collections
{
  [TestFixture]
  public class CompactListTest
  {
    [TestCase(new int[0], 1, false)]
    [TestCase(new[] { 1 }, 1, true)]
    [TestCase(new[] { 1 }, 2, false)]
    [TestCase(new[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, 5, true)]
    [TestCase(new[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, 0, false)]
    public void TestContains(int[] values, int value, bool expected)
    {
      var compactList = new CompactList<int>();
      for (int i = 0; i < values.Length; i++)
      {
        compactList.Add(values[i]);
      }

      Assert.AreEqual(compactList.Contains(value, EqualityComparer<int>.Default), expected);
    }

    [TestCase(new int[0], 1, -1)]
    [TestCase(new[] { 1 }, 1, 0)]
    [TestCase(new[] { 1 }, 2, -1)]
    [TestCase(new[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, 5, 4)]
    [TestCase(new[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, 0, -1)]
    public void TestIndexOf(int[] values, int value, int expectedIndex)
    {
      var compactList = new CompactList<int>();
      for (int i = 0; i < values.Length; i++)
      {
        compactList.Add(values[i]);
      }

      Assert.AreEqual(compactList.IndexOf(value, EqualityComparer<int>.Default), expectedIndex);
    }
  }
}