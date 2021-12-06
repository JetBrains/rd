// Licensed to the .NET Foundation under one or more agreements.
// The .NET Foundation licenses this file to you under the MIT license.

// This was copied from https://github.com/dotnet/runtime/blob/f54ab52d24ee524a246e463d754e526832850d4a/src/libraries/System.Private.CoreLib/src/System/Runtime/CompilerServices/DefaultInterpolatedStringHandler.cs.
// and updated to test JetDefaultInterpolatedStringHandler and fix compilation

#if !NET35
using System.Globalization;
using System.Text;
using JetBrains.Diagnostics.StringInterpolation;
using NUnit.Framework;

namespace System.Runtime.CompilerServices.Tests
{
    public class JetDefaultInterpolatedStringHandlerTests
    {
        [Test]
        [TestCase(0, 0)]
        [TestCase(1, 1)]
        [TestCase(42, 84)]
        [TestCase(-1, 0)]
        [TestCase(-1, -1)]
        [TestCase(-16, 1)]
        public void LengthAndHoleArguments_Valid(int literalLength, int formattedCount)
        {
            new JetDefaultInterpolatedStringHandler(literalLength, formattedCount);

            Span<char> scratch1 = stackalloc char[1];
            foreach (IFormatProvider provider in new IFormatProvider[] { null, new ConcatFormatter(), CultureInfo.InvariantCulture, CultureInfo.CurrentCulture, new CultureInfo("en-US"), new CultureInfo("fr-FR") })
            {
                new JetDefaultInterpolatedStringHandler(literalLength, formattedCount, provider);

                new JetDefaultInterpolatedStringHandler(literalLength, formattedCount, provider, default);
                new JetDefaultInterpolatedStringHandler(literalLength, formattedCount, provider, scratch1);
                new JetDefaultInterpolatedStringHandler(literalLength, formattedCount, provider, Array.Empty<char>());
                new JetDefaultInterpolatedStringHandler(literalLength, formattedCount, provider, new char[256]);
            }
        }

        [Test]
        public void ToString_DoesntClear()
        {
            JetDefaultInterpolatedStringHandler handler = new JetDefaultInterpolatedStringHandler(0, 0);
            handler.AppendLiteral("hi");
            for (int i = 0; i < 3; i++)
            {
                Assert.AreEqual("hi", handler.ToString());
            }
            Assert.AreEqual("hi", handler.ToStringAndClear());
        }

        [Test]
        public void ToStringAndClear_Clears()
        {
            JetDefaultInterpolatedStringHandler handler = new JetDefaultInterpolatedStringHandler(0, 0);
            handler.AppendLiteral("hi");
            Assert.AreEqual("hi", handler.ToStringAndClear());
            Assert.AreEqual(string.Empty, handler.ToStringAndClear());
        }

        [Test]
        public void AppendLiteral()
        {
            var expected = new StringBuilder();
            JetDefaultInterpolatedStringHandler actual = new JetDefaultInterpolatedStringHandler(0, 0);

            foreach (string s in new[] { "", "a", "bc", "def", "this is a long string", "!" })
            {
                expected.Append(s);
                actual.AppendLiteral(s);
            }

            Assert.AreEqual(expected.ToString(), actual.ToStringAndClear());
        }

        [Test]
        public void AppendFormatted_ReadOnlySpanChar()
        {
            var expected = new StringBuilder();
            JetDefaultInterpolatedStringHandler actual = new JetDefaultInterpolatedStringHandler(0, 0);

            foreach (string s in new[] { "", "a", "bc", "def", "this is a longer string", "!" })
            {
                // span
                expected.Append(s);
                actual.AppendFormatted(s.AsSpan());

                // span, format
                expected.AppendFormat("{0:X2}", s);
                actual.AppendFormatted(s.AsSpan(), format: "X2");

                foreach (int alignment in new[] { 0, 3, -3 })
                {
                    // span, alignment
                    expected.AppendFormat("{0," + alignment.ToString(CultureInfo.InvariantCulture) + "}", s);
                    actual.AppendFormatted(s.AsSpan(), alignment);

                    // span, alignment, format
                    expected.AppendFormat("{0," + alignment.ToString(CultureInfo.InvariantCulture) + ":X2}", s);
                    actual.AppendFormatted(s.AsSpan(), alignment, "X2");
                }
            }

            Assert.AreEqual(expected.ToString(), actual.ToStringAndClear());
        }

        [Test]
        public void AppendFormatted_String()
        {
            var expected = new StringBuilder();
            JetDefaultInterpolatedStringHandler actual = new JetDefaultInterpolatedStringHandler(0, 0);

            foreach (string s in new[] { null, "", "a", "bc", "def", "this is a longer string", "!" })
            {
                // string
                expected.AppendFormat("{0}", s);
                actual.AppendFormatted(s);

                // string, format
                expected.AppendFormat("{0:X2}", s);
                actual.AppendFormatted(s, "X2");

                foreach (int alignment in new[] { 0, 3, -3 })
                {
                    // string, alignment
                    expected.AppendFormat("{0," + alignment.ToString(CultureInfo.InvariantCulture) + "}", s);
                    actual.AppendFormatted(s, alignment);

                    // string, alignment, format
                    expected.AppendFormat("{0," + alignment.ToString(CultureInfo.InvariantCulture) + ":X2}", s);
                    actual.AppendFormatted(s, alignment, "X2");
                }
            }

            Assert.AreEqual(expected.ToString(), actual.ToStringAndClear());
        }

        [Test]
        public void AppendFormatted_String_ICustomFormatter()
        {
            var provider = new ConcatFormatter();

            var expected = new StringBuilder();
            JetDefaultInterpolatedStringHandler actual = new JetDefaultInterpolatedStringHandler(0, 0, provider);

            foreach (string s in new[] { null, "", "a" })
            {
                // string
                expected.AppendFormat(provider, "{0}", s);
                actual.AppendFormatted(s);

                // string, format
                expected.AppendFormat(provider, "{0:X2}", s);
                actual.AppendFormatted(s, "X2");

                // string, alignment
                expected.AppendFormat(provider, "{0,3}", s);
                actual.AppendFormatted(s, 3);

                // string, alignment, format
                expected.AppendFormat(provider, "{0,-3:X2}", s);
                actual.AppendFormatted(s, -3, "X2");
            }

            Assert.AreEqual(expected.ToString(), actual.ToStringAndClear());
        }

        [Test]
        public void AppendFormatted_ReferenceTypes()
        {
            var expected = new StringBuilder();
            JetDefaultInterpolatedStringHandler actual = new JetDefaultInterpolatedStringHandler(0, 0);

            foreach (string rawInput in new[] { null, "", "a", "bc", "def", "this is a longer string", "!" })
            {
                foreach (object o in new object[]
                {
                    rawInput, // raw string directly; ToString will return itself
                    new StringWrapper(rawInput), // wrapper object that returns string from ToString
                    new FormattableStringWrapper(rawInput), // IFormattable wrapper around string
                    // new SpanFormattableStringWrapper(rawInput) // ISpanFormattable wrapper around string
                })
                {
                    // object
                    expected.AppendFormat("{0}", o);
                    actual.AppendFormatted(o);
                    if (o is IHasToStringState tss1)
                    {
                        Assert.True(string.IsNullOrEmpty(tss1.ToStringState.LastFormat));
                        AssertModeMatchesType(tss1);
                    }

                    // object, format
                    expected.AppendFormat("{0:X2}", o);
                    actual.AppendFormatted(o,  "X2");
                    if (o is IHasToStringState tss2)
                    {
                        Assert.AreEqual("X2", tss2.ToStringState.LastFormat);
                        AssertModeMatchesType(tss2);
                    }

                    foreach (int alignment in new[] { 0, 3, -3 })
                    {
                        // object, alignment
                        expected.AppendFormat("{0," + alignment.ToString(CultureInfo.InvariantCulture) + "}", o);
                        actual.AppendFormatted(o, alignment);
                        if (o is IHasToStringState tss3)
                        {
                            Assert.True(string.IsNullOrEmpty(tss3.ToStringState.LastFormat));
                            AssertModeMatchesType(tss3);
                        }

                        // object, alignment, format
                        expected.AppendFormat("{0," + alignment.ToString(CultureInfo.InvariantCulture) + ":X2}", o);
                        actual.AppendFormatted(o, alignment, "X2");
                        if (o is IHasToStringState tss4)
                        {
                            Assert.AreEqual("X2", tss4.ToStringState.LastFormat);
                            AssertModeMatchesType(tss4);
                        }
                    }
                }
            }

            Assert.AreEqual(expected.ToString(), actual.ToStringAndClear());
        }

        [Test]
        [TestCase(false)]
        [TestCase(true)]
        public void AppendFormatted_ReferenceTypes_CreateProviderFlowed(bool useScratch)
        {
            var provider = new CultureInfo("en-US");
            JetDefaultInterpolatedStringHandler handler = useScratch ?
                new JetDefaultInterpolatedStringHandler(1, 2, provider, stackalloc char[16]) :
                new JetDefaultInterpolatedStringHandler(1, 2, provider);

            foreach (IHasToStringState tss in new IHasToStringState[] { new FormattableStringWrapper("hello"), /*new SpanFormattableStringWrapper("hello")*/ })
            {
                handler.AppendFormatted(tss);
                Assert.AreSame(provider, tss.ToStringState.LastProvider);

                handler.AppendFormatted(tss, 1);
                Assert.AreSame(provider, tss.ToStringState.LastProvider);

                handler.AppendFormatted(tss, "X2");
                Assert.AreSame(provider, tss.ToStringState.LastProvider);

                handler.AppendFormatted(tss, 1, "X2");
                Assert.AreSame(provider, tss.ToStringState.LastProvider);
            }
        }

        [Test]
        public void AppendFormatted_ReferenceTypes_ICustomFormatter()
        {
            var provider = new ConcatFormatter();

            var expected = new StringBuilder();
            JetDefaultInterpolatedStringHandler actual = new JetDefaultInterpolatedStringHandler(0, 0, provider);

            foreach (string s in new[] { null, "", "a" })
            {
                foreach (IHasToStringState tss in new IHasToStringState[] { new FormattableStringWrapper(s), /*new SpanFormattableStringWrapper(s)*/ })
                {
                    void AssertTss(IHasToStringState tss, string format)
                    {
                        Assert.AreEqual(format, tss.ToStringState.LastFormat);
                        Assert.AreSame(provider, tss.ToStringState.LastProvider);
                        Assert.AreEqual(ToStringMode.ICustomFormatterFormat, tss.ToStringState.ToStringMode);
                    }

                    // object
                    expected.AppendFormat(provider, "{0}", tss);
                    actual.AppendFormatted(tss);
                    AssertTss(tss, null);

                    // object, format
                    expected.AppendFormat(provider, "{0:X2}", tss);
                    actual.AppendFormatted(tss, "X2");
                    AssertTss(tss, "X2");

                    // object, alignment
                    expected.AppendFormat(provider, "{0,3}", tss);
                    actual.AppendFormatted(tss, 3);
                    AssertTss(tss, null);

                    // object, alignment, format
                    expected.AppendFormat(provider, "{0,-3:X2}", tss);
                    actual.AppendFormatted(tss, -3, "X2");
                    AssertTss(tss, "X2");
                }
            }

            Assert.AreEqual(expected.ToString(), actual.ToStringAndClear());
        }

        [Test]
        public void AppendFormatted_ValueTypes()
        {
            void Test<T>(T t)
            {
                var expected = new StringBuilder();
                JetDefaultInterpolatedStringHandler actual = new JetDefaultInterpolatedStringHandler(0, 0);

                // struct
                expected.AppendFormat("{0}", t);
                actual.AppendFormatted(t);
                Assert.True(string.IsNullOrEmpty(((IHasToStringState)t).ToStringState.LastFormat));
                AssertModeMatchesType(((IHasToStringState)t));

                // struct, format
                expected.AppendFormat("{0:X2}", t);
                actual.AppendFormatted(t, "X2");
                Assert.AreEqual("X2", ((IHasToStringState)t).ToStringState.LastFormat);
                AssertModeMatchesType(((IHasToStringState)t));

                foreach (int alignment in new[] { 0, 3, -3 })
                {
                    // struct, alignment
                    expected.AppendFormat("{0," + alignment.ToString(CultureInfo.InvariantCulture) + "}", t);
                    actual.AppendFormatted(t, alignment);
                    Assert.True(string.IsNullOrEmpty(((IHasToStringState)t).ToStringState.LastFormat));
                    AssertModeMatchesType(((IHasToStringState)t));

                    // struct, alignment, format
                    expected.AppendFormat("{0," + alignment.ToString(CultureInfo.InvariantCulture) + ":X2}", t);
                    actual.AppendFormatted(t, alignment, "X2");
                    Assert.AreEqual("X2", ((IHasToStringState)t).ToStringState.LastFormat);
                    AssertModeMatchesType(((IHasToStringState)t));
                }

                Assert.AreEqual(expected.ToString(), actual.ToStringAndClear());
            }

            Test(new FormattableInt32Wrapper(42));
            // Test(new SpanFormattableInt32Wrapper(84));
            Test((FormattableInt32Wrapper?)new FormattableInt32Wrapper(42));
            // Test((SpanFormattableInt32Wrapper?)new SpanFormattableInt32Wrapper(84));
        }

        [Test]
        [TestCase(false)]
        [TestCase(true)]
        public void AppendFormatted_ValueTypes_CreateProviderFlowed(bool useScratch)
        {
            void Test<T>(T t)
            {
                var provider = new CultureInfo("en-US");
                JetDefaultInterpolatedStringHandler handler = useScratch ?
                    new JetDefaultInterpolatedStringHandler(1, 2, provider, stackalloc char[16]) :
                    new JetDefaultInterpolatedStringHandler(1, 2, provider);

                handler.AppendFormatted(t);
                Assert.AreSame(provider, ((IHasToStringState)t).ToStringState.LastProvider);

                handler.AppendFormatted(t, 1);
                Assert.AreSame(provider, ((IHasToStringState)t).ToStringState.LastProvider);

                handler.AppendFormatted(t, "X2");
                Assert.AreSame(provider, ((IHasToStringState)t).ToStringState.LastProvider);

                handler.AppendFormatted(t, 1, "X2");
                Assert.AreSame(provider, ((IHasToStringState)t).ToStringState.LastProvider);
            }

            Test(new FormattableInt32Wrapper(42));
            // Test(new SpanFormattableInt32Wrapper(84));
            Test((FormattableInt32Wrapper?)new FormattableInt32Wrapper(42));
            // Test((SpanFormattableInt32Wrapper?)new SpanFormattableInt32Wrapper(84));
        }

        [Test]
        public void AppendFormatted_ValueTypes_ICustomFormatter()
        {
            var provider = new ConcatFormatter();

            void Test<T>(T t)
            {
                void AssertTss(T tss, string format)
                {
                    Assert.AreEqual(format, ((IHasToStringState)tss).ToStringState.LastFormat);
                    Assert.AreSame(provider, ((IHasToStringState)tss).ToStringState.LastProvider);
                    Assert.AreEqual(ToStringMode.ICustomFormatterFormat, ((IHasToStringState)tss).ToStringState.ToStringMode);
                }

                var expected = new StringBuilder();
                JetDefaultInterpolatedStringHandler actual = new JetDefaultInterpolatedStringHandler(0, 0, provider);

                // struct
                expected.AppendFormat(provider, "{0}", t);
                actual.AppendFormatted(t);
                AssertTss(t, null);

                // struct, format
                expected.AppendFormat(provider, "{0:X2}", t);
                actual.AppendFormatted(t, "X2");
                AssertTss(t, "X2");

                // struct, alignment
                expected.AppendFormat(provider, "{0,3}", t);
                actual.AppendFormatted(t, 3);
                AssertTss(t, null);

                // struct, alignment, format
                expected.AppendFormat(provider, "{0,-3:X2}", t);
                actual.AppendFormatted(t, -3, "X2");
                AssertTss(t, "X2");

                Assert.AreEqual(expected.ToString(), actual.ToStringAndClear());
            }

            Test(new FormattableInt32Wrapper(42));
            // Test(new SpanFormattableInt32Wrapper(84));
            Test((FormattableInt32Wrapper?)new FormattableInt32Wrapper(42));
            // Test((SpanFormattableInt32Wrapper?)new SpanFormattableInt32Wrapper(84));
        }

        [Test]
        [TestCase(false)]
        [TestCase(true)]
        public void Grow_Large(bool useScratch)
        {
            var expected = new StringBuilder();
            JetDefaultInterpolatedStringHandler handler = useScratch ?
                new JetDefaultInterpolatedStringHandler(3, 1000, null, stackalloc char[16]) :
                new JetDefaultInterpolatedStringHandler(3, 1000);

            for (int i = 0; i < 1000; i++)
            {
                handler.AppendFormatted(i);
                expected.Append(i);

                handler.AppendFormatted(i, 3);
                expected.AppendFormat("{0,3}", i);
            }

            Assert.AreEqual(expected.ToString(), handler.ToStringAndClear());
        }

        private static void AssertModeMatchesType<T>(T tss) where T : IHasToStringState
        {
            ToStringMode expected =
                // tss is ISpanFormattable ? ToStringMode.ISpanFormattableTryFormat :
                tss is IFormattable ? ToStringMode.IFormattableToString :
                ToStringMode.ObjectToString;
            Assert.AreEqual(expected, tss.ToStringState.ToStringMode);
        }

        // private sealed class SpanFormattableStringWrapper : ISpanFormattable, IHasToStringState
        // {
        //     private readonly string _value;
        //     public ToStringState ToStringState { get; } = new ToStringState();
        //
        //     public SpanFormattableStringWrapper(string value) => _value = value;
        //
        //     public bool TryFormat(Span<char> destination, out int charsWritten, ReadOnlySpan<char> format, IFormatProvider provider)
        //     {
        //         ToStringState.LastFormat = format.ToString();
        //         ToStringState.LastProvider = provider;
        //         ToStringState.ToStringMode = ToStringMode.ISpanFormattableTryFormat;
        //
        //         if (_value is null)
        //         {
        //             charsWritten = 0;
        //             return true;
        //         }
        //
        //         if (_value.Length > destination.Length)
        //         {
        //             charsWritten = 0;
        //             return false;
        //         }
        //
        //         charsWritten = _value.Length;
        //         _value.AsSpan().CopyTo(destination);
        //         return true;
        //     }
        //
        //     public string ToString(string format, IFormatProvider formatProvider)
        //     {
        //         ToStringState.LastFormat = format;
        //         ToStringState.LastProvider = formatProvider;
        //         ToStringState.ToStringMode = ToStringMode.IFormattableToString;
        //         return _value;
        //     }
        //
        //     public override string ToString()
        //     {
        //         ToStringState.LastFormat = null;
        //         ToStringState.LastProvider = null;
        //         ToStringState.ToStringMode = ToStringMode.ObjectToString;
        //         return _value;
        //     }
        // }

        // private struct SpanFormattableInt32Wrapper : ISpanFormattable, IHasToStringState
        // {
        //     private readonly int _value;
        //     public ToStringState ToStringState { get; }
        //
        //     public SpanFormattableInt32Wrapper(int value)
        //     {
        //         ToStringState = new ToStringState();
        //         _value = value;
        //     }
        //
        //     public bool TryFormat(Span<char> destination, out int charsWritten, ReadOnlySpan<char> format, IFormatProvider provider)
        //     {
        //         ToStringState.LastFormat = format.ToString();
        //         ToStringState.LastProvider = provider;
        //         ToStringState.ToStringMode = ToStringMode.ISpanFormattableTryFormat;
        //
        //         return _value.TryFormat(destination, out charsWritten, format, provider);
        //     }
        //
        //     public string ToString(string format, IFormatProvider formatProvider)
        //     {
        //         ToStringState.LastFormat = format;
        //         ToStringState.LastProvider = formatProvider;
        //         ToStringState.ToStringMode = ToStringMode.IFormattableToString;
        //         return _value.ToString(format, formatProvider);
        //     }
        //
        //     public override string ToString()
        //     {
        //         ToStringState.LastFormat = null;
        //         ToStringState.LastProvider = null;
        //         ToStringState.ToStringMode = ToStringMode.ObjectToString;
        //         return _value.ToString();
        //     }
        // }

        private sealed class FormattableStringWrapper : IFormattable, IHasToStringState
        {
            private readonly string _value;
            public ToStringState ToStringState { get; } = new ToStringState();

            public FormattableStringWrapper(string s) => _value = s;

            public string ToString(string format, IFormatProvider formatProvider)
            {
                ToStringState.LastFormat = format;
                ToStringState.LastProvider = formatProvider;
                ToStringState.ToStringMode = ToStringMode.IFormattableToString;
                return _value;
            }

            public override string ToString()
            {
                ToStringState.LastFormat = null;
                ToStringState.LastProvider = null;
                ToStringState.ToStringMode = ToStringMode.ObjectToString;
                return _value;
            }
        }

        private struct FormattableInt32Wrapper : IFormattable, IHasToStringState
        {
            private readonly int _value;
            public ToStringState ToStringState { get; }

            public FormattableInt32Wrapper(int i)
            {
                ToStringState = new ToStringState();
                _value = i;
            }

            public string ToString(string format, IFormatProvider formatProvider)
            {
                ToStringState.LastFormat = format;
                ToStringState.LastProvider = formatProvider;
                ToStringState.ToStringMode = ToStringMode.IFormattableToString;
                return _value.ToString(format, formatProvider);
            }

            public override string ToString()
            {
                ToStringState.LastFormat = null;
                ToStringState.LastProvider = null;
                ToStringState.ToStringMode = ToStringMode.ObjectToString;
                return _value.ToString();
            }
        }

        private sealed class ToStringState
        {
            public string LastFormat { get; set; }
            public IFormatProvider LastProvider { get; set; }
            public ToStringMode ToStringMode { get; set; }
        }

        private interface IHasToStringState
        {
            ToStringState ToStringState { get; }
        }

        private enum ToStringMode
        {
            ObjectToString,
            IFormattableToString,
            ISpanFormattableTryFormat,
            ICustomFormatterFormat,
        }

        private sealed class StringWrapper
        {
            private readonly string _value;

            public StringWrapper(string s) => _value = s;

            public override string ToString() => _value;
        }

        private sealed class ConcatFormatter : IFormatProvider, ICustomFormatter
        {
            public object GetFormat(Type formatType) => formatType == typeof(ICustomFormatter) ? this : null;

            public string Format(string format, object arg, IFormatProvider formatProvider)
            {
                string s = format + " " + arg + formatProvider;

                if (arg is IHasToStringState tss)
                {
                    // Set after using arg.ToString() in concat above
                    tss.ToStringState.LastFormat = format;
                    tss.ToStringState.LastProvider = formatProvider;
                    tss.ToStringState.ToStringMode = ToStringMode.ICustomFormatterFormat;
                }

                return s;
            }
        }
    }
}
#endif