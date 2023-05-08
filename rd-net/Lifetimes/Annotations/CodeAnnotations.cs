/* MIT License

Copyright (c) 2016 JetBrains http://www.jetbrains.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. */

using System;
// ReSharper disable All

#pragma warning disable 1591
// ReSharper disable UnusedMember.Global
// ReSharper disable MemberCanBePrivate.Global
// ReSharper disable UnusedAutoPropertyAccessor.Global
// ReSharper disable IntroduceOptionalParameters.Global
// ReSharper disable MemberCanBeProtected.Global
// ReSharper disable InconsistentNaming

namespace JetBrains.Annotations
{
    /// <summary>
    /// Indicates that the value of the marked element could be <c>null</c> sometimes,
    /// so the check for <c>null</c> is necessary before its usage.
    /// </summary>
    /// <example><code>
    /// [CanBeNull] object Test() => null;
    /// 
    /// void UseTest() {
    ///   var p = Test();
    ///   var s = p.ToString(); // Warning: Possible 'System.NullReferenceException'
    /// }
    /// </code></example>
    [AttributeUsage(
      AttributeTargets.Method | AttributeTargets.Parameter | AttributeTargets.Property |
      AttributeTargets.Delegate | AttributeTargets.Field | AttributeTargets.Event |
      AttributeTargets.Class | AttributeTargets.Interface | AttributeTargets.GenericParameter)]
    internal sealed class CanBeNullAttribute : Attribute { }

    /// <summary>
    /// Indicates that the value of the marked element could never be <c>null</c>.
    /// </summary>
    /// <example><code>
    /// [NotNull] object Foo() {
    ///   return null; // Warning: Possible 'null' assignment
    /// }
    /// </code></example>
    [AttributeUsage(
      AttributeTargets.Method | AttributeTargets.Parameter | AttributeTargets.Property |
      AttributeTargets.Delegate | AttributeTargets.Field | AttributeTargets.Event |
      AttributeTargets.Class | AttributeTargets.Interface | AttributeTargets.GenericParameter)]
    internal sealed class NotNullAttribute : Attribute { }

    /// <summary>
    /// Can be applied to symbols of types derived from IEnumerable as well as to symbols of Task
    /// and Lazy classes to indicate that the value of a collection item, of the Task.Result property
    /// or of the Lazy.Value property can never be null.
    /// </summary>
    [AttributeUsage(
      AttributeTargets.Method | AttributeTargets.Parameter | AttributeTargets.Property |
      AttributeTargets.Delegate | AttributeTargets.Field)]
    internal sealed class ItemNotNullAttribute : Attribute { }

    /// <summary>
    /// Can be applied to symbols of types derived from IEnumerable as well as to symbols of Task
    /// and Lazy classes to indicate that the value of a collection item, of the Task.Result property
    /// or of the Lazy.Value property can be null.
    /// </summary>
    [AttributeUsage(
      AttributeTargets.Method | AttributeTargets.Parameter | AttributeTargets.Property |
      AttributeTargets.Delegate | AttributeTargets.Field)]
    internal sealed class ItemCanBeNullAttribute : Attribute { }

    /// <summary>
    /// Implicitly apply [NotNull]/[ItemNotNull] annotation to all the of type members and parameters
    /// in particular scope where this annotation is used (type declaration or whole assembly).
    /// </summary>
    [AttributeUsage(
      AttributeTargets.Class | AttributeTargets.Struct | AttributeTargets.Interface | AttributeTargets.Assembly)]
    internal sealed class ImplicitNotNullAttribute : Attribute { }

    /// <summary>
    /// Indicates that the marked method builds string by format pattern and (optional) arguments.
    /// Parameter, which contains format string, should be given in constructor. The format string
    /// should be in <see cref="string.Format(IFormatProvider,string,object[])"/>-like form.
    /// </summary>
    /// <example><code>
    /// [StringFormatMethod("message")]
    /// void ShowError(string message, params object[] args) { /* do something */ }
    /// 
    /// void Foo() {
    ///   ShowError("Failed: {0}"); // Warning: Non-existing argument in format string
    /// }
    /// </code></example>
    [AttributeUsage(
      AttributeTargets.Constructor | AttributeTargets.Method |
      AttributeTargets.Property | AttributeTargets.Delegate)]
    internal sealed class StringFormatMethodAttribute : Attribute
    {
        /// <param name="formatParameterName">
        /// Specifies which parameter of an annotated method should be treated as format-string
        /// </param>
        public StringFormatMethodAttribute([NotNull] string formatParameterName)
        {
            FormatParameterName = formatParameterName;
        }

        [NotNull]
        public string FormatParameterName { get; private set; }
    }

    /// <summary>
    /// For a parameter that is expected to be one of the limited set of values.
    /// Specify fields of which type should be used as values for this parameter.
    /// </summary>
    [AttributeUsage(AttributeTargets.Parameter | AttributeTargets.Property | AttributeTargets.Field)]
    internal sealed class ValueProviderAttribute : Attribute
    {
        public ValueProviderAttribute([NotNull] string name)
        {
            Name = name;
        }

        [NotNull]
        public string Name { get; private set; }
    }

    /// <summary>
    /// Indicates that the function argument should be string literal and match one
    /// of the parameters of the caller function. For example, ReSharper annotates
    /// the parameter of <see cref="System.ArgumentNullException"/>.
    /// </summary>
    /// <example><code>
    /// void Foo(string param) {
    ///   if (param == null)
    ///     throw new ArgumentNullException("par"); // Warning: Cannot resolve symbol
    /// }
    /// </code></example>
    [AttributeUsage(AttributeTargets.Parameter)]
    internal sealed class InvokerParameterNameAttribute : Attribute { }

    /// <summary>
    /// Indicates that the method is contained in a type that implements
    /// <c>System.ComponentModel.INotifyPropertyChanged</c> interface and this method
    /// is used to notify that some property value changed.
    /// </summary>
    /// <remarks>
    /// The method should be non-static and conform to one of the supported signatures:
    /// <list>
    /// <item><c>NotifyChanged(string)</c></item>
    /// <item><c>NotifyChanged(params string[])</c></item>
    /// <item><c>NotifyChanged{T}(Expression{Func{T}})</c></item>
    /// <item><c>NotifyChanged{T,U}(Expression{Func{T,U}})</c></item>
    /// <item><c>SetProperty{T}(ref T, T, string)</c></item>
    /// </list>
    /// </remarks>
    /// <example><code>
    /// public class Foo : INotifyPropertyChanged {
    ///   public event PropertyChangedEventHandler PropertyChanged;
    /// 
    ///   [NotifyPropertyChangedInvocator]
    ///   protected virtual void NotifyChanged(string propertyName) { ... }
    ///
    ///   string _name;
    /// 
    ///   public string Name {
    ///     get { return _name; }
    ///     set { _name = value; NotifyChanged("LastName"); /* Warning */ }
    ///   }
    /// }
    /// </code>
    /// Examples of generated notifications:
    /// <list>
    /// <item><c>NotifyChanged("Property")</c></item>
    /// <item><c>NotifyChanged(() =&gt; Property)</c></item>
    /// <item><c>NotifyChanged((VM x) =&gt; x.Property)</c></item>
    /// <item><c>SetProperty(ref myField, value, "Property")</c></item>
    /// </list>
    /// </example>
    [AttributeUsage(AttributeTargets.Method)]
    internal sealed class NotifyPropertyChangedInvocatorAttribute : Attribute
    {
        public NotifyPropertyChangedInvocatorAttribute() { }
        public NotifyPropertyChangedInvocatorAttribute([NotNull] string parameterName)
        {
            ParameterName = parameterName;
        }

        public string? ParameterName { get; private set; }
    }

    /// <summary>
    /// Describes dependency between method input and output.
    /// </summary>
    /// <syntax>
    /// <p>Function Definition Table syntax:</p>
    /// <list>
    /// <item>FDT      ::= FDTRow [;FDTRow]*</item>
    /// <item>FDTRow   ::= Input =&gt; Output | Output &lt;= Input</item>
    /// <item>Input    ::= ParameterName: Value [, Input]*</item>
    /// <item>Output   ::= [ParameterName: Value]* {halt|stop|void|nothing|Value}</item>
    /// <item>Value    ::= true | false | null | notnull | canbenull</item>
    /// </list>
    /// If method has single input parameter, it's name could be omitted.<br/>
    /// Using <c>halt</c> (or <c>void</c>/<c>nothing</c>, which is the same)
    /// for method output means that the methods doesn't return normally.<br/>
    /// <c>canbenull</c> annotation is only applicable for output parameters.<br/>
    /// You can use multiple <c>[ContractAnnotation]</c> for each FDT row,
    /// or use single attribute with rows separated by semicolon.<br/>
    /// </syntax>
    /// <examples><list>
    /// <item><code>
    /// [ContractAnnotation("=> halt")]
    /// public void TerminationMethod()
    /// </code></item>
    /// <item><code>
    /// [ContractAnnotation("halt &lt;= condition: false")]
    /// public void Assert(bool condition, string text) // regular assertion method
    /// </code></item>
    /// <item><code>
    /// [ContractAnnotation("s:null => true")]
    /// public bool IsNullOrEmpty(string s) // string.IsNullOrEmpty()
    /// </code></item>
    /// <item><code>
    /// // A method that returns null if the parameter is null,
    /// // and not null if the parameter is not null
    /// [ContractAnnotation("null => null; notnull => notnull")]
    /// public object Transform(object data) 
    /// </code></item>
    /// <item><code>
    /// [ContractAnnotation("s:null=>false; =>true,result:notnull; =>false, result:null")]
    /// public bool TryParse(string s, out Person result)
    /// </code></item>
    /// </list></examples>
    [AttributeUsage(AttributeTargets.Method, AllowMultiple = true)]
    internal sealed class ContractAnnotationAttribute : Attribute
    {
        public ContractAnnotationAttribute([NotNull] string contract)
          : this(contract, false) { }

        public ContractAnnotationAttribute([NotNull] string contract, bool forceFullStates)
        {
            Contract = contract;
            ForceFullStates = forceFullStates;
        }

        [NotNull]
        public string Contract { get; private set; }
        public bool ForceFullStates { get; private set; }
    }

    /// <summary>
    /// Indicates that marked element should be localized or not.
    /// </summary>
    /// <example><code>
    /// [LocalizationRequiredAttribute(true)]
    /// class Foo {
    ///   string str = "my string"; // Warning: Localizable string
    /// }
    /// </code></example>
    [AttributeUsage(AttributeTargets.All)]
    internal sealed class LocalizationRequiredAttribute : Attribute
    {
        public LocalizationRequiredAttribute() : this(true) { }
        public LocalizationRequiredAttribute(bool required)
        {
            Required = required;
        }

        public bool Required { get; private set; }
    }

    /// <summary>
    /// Indicates that the value of the marked type (or its derivatives)
    /// cannot be compared using '==' or '!=' operators and <c>Equals()</c>
    /// should be used instead. However, using '==' or '!=' for comparison
    /// with <c>null</c> is always permitted.
    /// </summary>
    /// <example><code>
    /// [CannotApplyEqualityOperator]
    /// class NoEquality { }
    /// 
    /// class UsesNoEquality {
    ///   void Test() {
    ///     var ca1 = new NoEquality();
    ///     var ca2 = new NoEquality();
    ///     if (ca1 != null) { // OK
    ///       bool condition = ca1 == ca2; // Warning
    ///     }
    ///   }
    /// }
    /// </code></example>
    [AttributeUsage(AttributeTargets.Interface | AttributeTargets.Class | AttributeTargets.Struct)]
    internal sealed class CannotApplyEqualityOperatorAttribute : Attribute { }

    /// <summary>
    /// When applied to a target attribute, specifies a requirement for any type marked
    /// with the target attribute to implement or inherit specific type or types.
    /// </summary>
    /// <example><code>
    /// [BaseTypeRequired(typeof(IComponent)] // Specify requirement
    /// class ComponentAttribute : Attribute { }
    /// 
    /// [Component] // ComponentAttribute requires implementing IComponent interface
    /// class MyComponent : IComponent { }
    /// </code></example>
    [AttributeUsage(AttributeTargets.Class, AllowMultiple = true)]
    [BaseTypeRequired(typeof(Attribute))]
    internal sealed class BaseTypeRequiredAttribute : Attribute
    {
        public BaseTypeRequiredAttribute([NotNull] Type baseType)
        {
            BaseType = baseType;
        }

        [NotNull]
        public Type BaseType { get; private set; }
    }

    /// <summary>
    /// Indicates that the marked symbol is used implicitly (e.g. via reflection, in external library),
    /// so this symbol will not be marked as unused (as well as by other usage inspections).
    /// </summary>
    [AttributeUsage(AttributeTargets.All)]
    internal sealed class UsedImplicitlyAttribute : Attribute
    {
        public UsedImplicitlyAttribute()
          : this(ImplicitUseKindFlags.Default, ImplicitUseTargetFlags.Default) { }

        public UsedImplicitlyAttribute(ImplicitUseKindFlags useKindFlags)
          : this(useKindFlags, ImplicitUseTargetFlags.Default) { }

        public UsedImplicitlyAttribute(ImplicitUseTargetFlags targetFlags)
          : this(ImplicitUseKindFlags.Default, targetFlags) { }

        public UsedImplicitlyAttribute(ImplicitUseKindFlags useKindFlags, ImplicitUseTargetFlags targetFlags)
        {
            UseKindFlags = useKindFlags;
            TargetFlags = targetFlags;
        }

        public ImplicitUseKindFlags UseKindFlags { get; private set; }
        public ImplicitUseTargetFlags TargetFlags { get; private set; }
    }

    /// <summary>
    /// Should be used on attributes and causes ReSharper to not mark symbols marked with such attributes
    /// as unused (as well as by other usage inspections)
    /// </summary>
    [AttributeUsage(AttributeTargets.Class | AttributeTargets.GenericParameter)]
    internal sealed class MeansImplicitUseAttribute : Attribute
    {
        public MeansImplicitUseAttribute()
          : this(ImplicitUseKindFlags.Default, ImplicitUseTargetFlags.Default) { }

        public MeansImplicitUseAttribute(ImplicitUseKindFlags useKindFlags)
          : this(useKindFlags, ImplicitUseTargetFlags.Default) { }

        public MeansImplicitUseAttribute(ImplicitUseTargetFlags targetFlags)
          : this(ImplicitUseKindFlags.Default, targetFlags) { }

        public MeansImplicitUseAttribute(ImplicitUseKindFlags useKindFlags, ImplicitUseTargetFlags targetFlags)
        {
            UseKindFlags = useKindFlags;
            TargetFlags = targetFlags;
        }

        [UsedImplicitly]
        public ImplicitUseKindFlags UseKindFlags { get; private set; }
        [UsedImplicitly]
        public ImplicitUseTargetFlags TargetFlags { get; private set; }
    }

    [Flags]
    internal enum ImplicitUseKindFlags
    {
        Default = Access | Assign | InstantiatedWithFixedConstructorSignature,
        /// <summary>Only entity marked with attribute considered used.</summary>
        Access = 1,
        /// <summary>Indicates implicit assignment to a member.</summary>
        Assign = 2,
        /// <summary>
        /// Indicates implicit instantiation of a type with fixed constructor signature.
        /// That means any unused constructor parameters won't be reported as such.
        /// </summary>
        InstantiatedWithFixedConstructorSignature = 4,
        /// <summary>Indicates implicit instantiation of a type.</summary>
        InstantiatedNoFixedConstructorSignature = 8
    }

    /// <summary>
    /// Specify what is considered used implicitly when marked
    /// with <see cref="MeansImplicitUseAttribute"/> or <see cref="UsedImplicitlyAttribute"/>.
    /// </summary>
    [Flags]
    internal enum ImplicitUseTargetFlags
    {
        Default = Itself,
        Itself = 1,
        /// <summary>Members of entity marked with attribute are considered used.</summary>
        Members = 2,
        /// <summary>Entity marked with attribute and all its members considered used.</summary>
        WithMembers = Itself | Members
    }

    /// <summary>
    /// This attribute is intended to mark publicly available API
    /// which should not be removed and so is treated as used.
    /// </summary>
    [MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
    internal sealed class PublicAPIAttribute : Attribute
    {
        public PublicAPIAttribute() { }
        public PublicAPIAttribute([NotNull] string comment)
        {
            Comment = comment;
        }

        public string? Comment { get; private set; }
    }

    /// <summary>
    /// Tells code analysis engine if the parameter is completely handled when the invoked method is on stack.
    /// If the parameter is a delegate, indicates that delegate is executed while the method is executed.
    /// If the parameter is an enumerable, indicates that it is enumerated while the method is executed.
    /// </summary>
    [AttributeUsage(AttributeTargets.Parameter)]
    internal sealed class InstantHandleAttribute : Attribute { }

    /// <summary>
    /// Indicates that a method does not make any observable state changes.
    /// The same as <c>System.Diagnostics.Contracts.PureAttribute</c>.
    /// </summary>
    /// <example><code>
    /// [Pure] int Multiply(int x, int y) => x * y;
    /// 
    /// void M() {
    ///   Multiply(123, 42); // Waring: Return value of pure method is not used
    /// }
    /// </code></example>
    [AttributeUsage(AttributeTargets.Method)]
    internal sealed class PureAttribute : Attribute { }

    /// <summary>
    /// Indicates that the return value of method invocation must be used.
    /// </summary>
    [AttributeUsage(AttributeTargets.Method)]
    internal sealed class MustUseReturnValueAttribute : Attribute
    {
        public MustUseReturnValueAttribute() { }
        public MustUseReturnValueAttribute([NotNull] string justification)
        {
            Justification = justification;
        }

        public string? Justification { get; private set; }
    }

    /// <summary>
    /// Indicates the type member or parameter of some type, that should be used instead of all other ways
    /// to get the value that type. This annotation is useful when you have some "context" value evaluated
    /// and stored somewhere, meaning that all other ways to get this value must be consolidated with existing one.
    /// </summary>
    /// <example><code>
    /// class Foo {
    ///   [ProvidesContext] IBarService _barService = ...;
    /// 
    ///   void ProcessNode(INode node) {
    ///     DoSomething(node, node.GetGlobalServices().Bar);
    ///     //              ^ Warning: use value of '_barService' field
    ///   }
    /// }
    /// </code></example>
    [AttributeUsage(
      AttributeTargets.Field | AttributeTargets.Property | AttributeTargets.Parameter | AttributeTargets.Method |
      AttributeTargets.Class | AttributeTargets.Interface | AttributeTargets.Struct | AttributeTargets.GenericParameter)]
    internal sealed class ProvidesContextAttribute : Attribute { }

    /// <summary>
    /// Indicates that a parameter is a path to a file or a folder within a web project.
    /// Path can be relative or absolute, starting from web root (~).
    /// </summary>
    [AttributeUsage(AttributeTargets.Parameter)]
    internal sealed class PathReferenceAttribute : Attribute
    {
        public PathReferenceAttribute() { }
        public PathReferenceAttribute([NotNull, PathReference] string basePath)
        {
            BasePath = basePath;
        }

        public string? BasePath { get; private set; }
    }
    

    /// <summary>
    /// Indicates how method, constructor invocation or property access
    /// over collection type affects content of the collection.
    /// </summary>
    [AttributeUsage(AttributeTargets.Method | AttributeTargets.Constructor | AttributeTargets.Property)]
    internal sealed class CollectionAccessAttribute : Attribute
    {
        public CollectionAccessAttribute(CollectionAccessType collectionAccessType)
        {
            CollectionAccessType = collectionAccessType;
        }

        public CollectionAccessType CollectionAccessType { get; private set; }
    }

    [Flags]
    internal enum CollectionAccessType
    {
        /// <summary>Method does not use or modify content of the collection.</summary>
        None = 0,
        /// <summary>Method only reads content of the collection but does not modify it.</summary>
        Read = 1,
        /// <summary>Method can change content of the collection but does not add new elements.</summary>
        ModifyExistingContent = 2,
        /// <summary>Method can add new elements to the collection.</summary>
        UpdatedContent = ModifyExistingContent | 4
    }

    /// <summary>
    /// Indicates that the marked method is assertion method, i.e. it halts control flow if
    /// one of the conditions is satisfied. To set the condition, mark one of the parameters with 
    /// <see cref="AssertionConditionAttribute"/> attribute.
    /// </summary>
    [AttributeUsage(AttributeTargets.Method)]
    internal sealed class AssertionMethodAttribute : Attribute { }
    
    

    /// <summary>
    /// Indicates that method is pure LINQ method, with postponed enumeration (like Enumerable.Select,
    /// .Where). This annotation allows inference of [InstantHandle] annotation for parameters
    /// of delegate type by analyzing LINQ method chains.
    /// </summary>
    [AttributeUsage(AttributeTargets.Method)]
    internal sealed class LinqTunnelAttribute : Attribute { }

    /// <summary>
    /// Indicates that IEnumerable, passed as parameter, is not enumerated.
    /// </summary>
    [AttributeUsage(AttributeTargets.Parameter)]
    internal sealed class NoEnumerationAttribute : Attribute { }

    /// <summary>
    /// Indicates that parameter is regular expression pattern.
    /// </summary>
    [AttributeUsage(AttributeTargets.Parameter)]
    internal sealed class RegexPatternAttribute : Attribute { }

    /// <summary>
    /// <para>
    /// Defines the code search template using the Structural Search and Replace syntax.
    /// It allows you to find and, if necessary, replace blocks of code that match a specific pattern.
    /// Search and replace patterns consist of a textual part and placeholders.
    /// Textural part must contain only identifiers allowed in the target language and will be matched exactly (white spaces, tabulation characters, and line breaks are ignored).
    /// Placeholders allow matching variable parts of the target code blocks.
    /// A placeholder has the following format: $placeholder_name$- where placeholder_name is an arbitrary identifier.
    /// </para>
    /// <para>
    /// Available placeholders:
    /// <list type="bullet">
    /// <item>$this$ - expression of containing type</item>
    /// <item>$thisType$ - containing type</item>
    /// <item>$member$ - current member placeholder</item>
    /// <item>$qualifier$ - this placeholder is available in the replace pattern and can be used to insert a qualifier expression matched by the $member$ placeholder.
    /// (Note that if $qualifier$ placeholder is used, then $member$ placeholder will match only qualified references)</item>
    /// <item>$expression$ - expression of any type</item>
    /// <item>$identifier$ - identifier placeholder</item>
    /// <item>$args$ - any number of arguments</item>
    /// <item>$arg$ - single argument</item>
    /// <item>$arg1$ ... $arg10$ - single argument</item>
    /// <item>$stmts$ - any number of statements</item>
    /// <item>$stmt$ - single statement</item>
    /// <item>$stmt1$ ... $stmt10$ - single statement</item>
    /// <item>$name{Expression, 'Namespace.FooType'}$ - expression with 'Namespace.FooType' type</item>
    /// <item>$expression{'Namespace.FooType'}$ - expression with 'Namespace.FooType' type</item>
    /// <item>$name{Type, 'Namespace.FooType'}$ - 'Namespace.FooType' type</item>
    /// <item>$type{'Namespace.FooType'}$ - 'Namespace.FooType' type</item>
    /// <item>$statement{1,2}$ - 1 or 2 statements</item>
    /// </list>
    /// </para>
    /// <para>
    /// Note that you can also define your own placeholders of the supported types and specify arguments for each placeholder type.
    /// This can be done using the following format: $name{type, arguments}$. Where 'name' - is the name of your placeholder,
    /// 'type' - is the type of your placeholder (one of the following: Expression, Type, Identifier, Statement, Argument, Member),
    /// 'arguments' - arguments list for your placeholder. Each placeholder type supports its own arguments, check examples below for more details.
    /// The placeholder type may be omitted and determined from the placeholder name, if the name has one of the following prefixes:
    /// <list type="bullet">
    /// <item>expr, expression - expression placeholder, e.g. $exprPlaceholder{}$, $expressionFoo{}$</item>
    /// <item>arg, argument - argument placeholder, e.g. $argPlaceholder{}$, $argumentFoo{}$</item>
    /// <item>ident, identifier - identifier placeholder, e.g. $identPlaceholder{}$, $identifierFoo{}$</item>
    /// <item>stmt, statement - statement placeholder, e.g. $stmtPlaceholder{}$, $statementFoo{}$</item>
    /// <item>type - type placeholder, e.g. $typePlaceholder{}$, $typeFoo{}$</item>
    /// <item>member - member placeholder, e.g. $memberPlaceholder{}$, $memberFoo{}$</item>
    /// </list>
    /// </para>
    /// <para>
    /// Expression placeholder arguments:
    /// <list type="bullet">
    /// <item>expressionType - string value in single quotes, specifies full type name to match (empty string by default)</item>
    /// <item>exactType - boolean value, specifies if expression should have exact type match (false by default)</item>
    /// </list>
    /// Examples:
    /// <list type="bullet">
    /// <item>$myExpr{Expression, 'Namespace.FooType', true}$ - defines expression placeholder, matching expressions of the 'Namespace.FooType' type with exact matching.</item>
    /// <item>$myExpr{Expression, 'Namespace.FooType'}$ - defines expression placeholder, matching expressions of the 'Namespace.FooType' type or expressions which can be implicitly converted to 'Namespace.FooType'.</item>
    /// <item>$myExpr{Expression}$ - defines expression placeholder, matching expressions of any type.</item>
    /// <item>$exprFoo{'Namespace.FooType', true}$ - defines expression placeholder, matching expressions of the 'Namespace.FooType' type with exact matching.</item>
    /// </list>
    /// </para>
    /// <para>
    /// Type placeholder arguments:
    /// <list type="bullet">
    /// <item>type - string value in single quotes, specifies full type name to match (empty string by default)</item>
    /// <item>exactType - boolean value, specifies if expression should have exact type match (false by default)</item>
    /// </list>
    /// Examples:
    /// <list type="bullet">
    /// <item>$myType{Type, 'Namespace.FooType', true}$ - defines type placeholder, matching 'Namespace.FooType' types with exact matching.</item>
    /// <item>$myType{Type, 'Namespace.FooType'}$ - defines type placeholder, matching 'Namespace.FooType' types or types, which can be implicitly converted to 'Namespace.FooType'.</item>
    /// <item>$myType{Type}$ - defines type placeholder, matching any type.</item>
    /// <item>$typeFoo{'Namespace.FooType', true}$ - defines types placeholder, matching 'Namespace.FooType' types with exact matching.</item>
    /// </list>
    /// </para>
    /// <para>
    /// Identifier placeholder arguments:
    /// <list type="bullet">
    /// <item>nameRegex - string value in single quotes, specifies regex to use for matching (empty string by default)</item>
    /// <item>nameRegexCaseSensitive - boolean value, specifies if name regex is case sensitive (true by default)</item>
    /// <item>type - string value in single quotes, specifies full type name to match (empty string by default)</item>
    /// <item>exactType - boolean value, specifies if expression should have exact type match (false by default)</item>
    /// </list>
    /// Examples:
    /// <list type="bullet">
    /// <item>$myIdentifier{Identifier, 'my.*', false, 'Namespace.FooType', true}$ - defines identifier placeholder, matching identifiers (ignoring case) starting with 'my' prefix with 'Namespace.FooType' type.</item>
    /// <item>$myIdentifier{Identifier, 'my.*', true, 'Namespace.FooType', true}$ - defines identifier placeholder, matching identifiers (case sensitively) starting with 'my' prefix with 'Namespace.FooType' type.</item>
    /// <item>$identFoo{'my.*'}$ - defines identifier placeholder, matching identifiers (case sensitively) starting with 'my' prefix.</item>
    /// </list>
    /// </para>
    /// <para>
    /// Statement placeholder arguments:
    /// <list type="bullet">
    /// <item>minimalOccurrences - minimal number of statements to match (-1 by default)</item>
    /// <item>maximalOccurrences - maximal number of statements to match (-1 by default)</item>
    /// </list>
    /// Examples:
    /// <list type="bullet">
    /// <item>$myStmt{Statement, 1, 2}$ - defines statement placeholder, matching 1 or 2 statements.</item>
    /// <item>$myStmt{Statement}$ - defines statement placeholder, matching any number of statements.</item>
    /// <item>$stmtFoo{1, 2}$ - defines statement placeholder, matching 1 or 2 statements.</item>
    /// </list>
    /// </para>
    /// <para>
    /// Argument placeholder arguments:
    /// <list type="bullet">
    /// <item>minimalOccurrences - minimal number of arguments to match (-1 by default)</item>
    /// <item>maximalOccurrences - maximal number of arguments to match (-1 by default)</item>
    /// </list>
    /// Examples:
    /// <list type="bullet">
    /// <item>$myArg{Argument, 1, 2}$ - defines argument placeholder, matching 1 or 2 arguments.</item>
    /// <item>$myArg{Argument}$ - defines argument placeholder, matching any number of arguments.</item>
    /// <item>$argFoo{1, 2}$ - defines argument placeholder, matching 1 or 2 arguments.</item>
    /// </list>
    /// </para>
    /// <para>
    /// Member placeholder arguments:
    /// <list type="bullet">
    /// <item>docId - string value in single quotes, specifies XML documentation id of the member to match (empty by default)</item>
    /// </list>
    /// Examples:
    /// <list type="bullet">
    /// <item>$myMember{Member, 'M:System.String.IsNullOrEmpty(System.String)'}$ - defines member placeholder, matching 'IsNullOrEmpty' member of the 'System.String' type.</item>
    /// <item>$memberFoo{'M:System.String.IsNullOrEmpty(System.String)'}$ - defines member placeholder, matching 'IsNullOrEmpty' member of the 'System.String' type.</item>
    /// </list>
    /// </para>
    /// <para>
    /// For more information please refer to the <a href="https://www.jetbrains.com/help/resharper/Navigation_and_Search__Structural_Search_and_Replace.html">Structural Search and Replace</a> article.
    /// </para>
    /// </summary>
    [AttributeUsage(
        AttributeTargets.Method
        | AttributeTargets.Constructor
        | AttributeTargets.Property
        | AttributeTargets.Field
        | AttributeTargets.Event
        | AttributeTargets.Interface
        | AttributeTargets.Class
        | AttributeTargets.Struct
        | AttributeTargets.Enum,
        AllowMultiple = true,
        Inherited = false)]
    public sealed class CodeTemplateAttribute : Attribute
    {
        public CodeTemplateAttribute(string searchTemplate)
        {
            SearchTemplate = searchTemplate;
        }

        /// <summary>
        /// Structural search pattern to use in the code template.
        /// The pattern includes a textual part, which must contain only identifiers allowed in the target language,
        /// and placeholders, which allow matching variable parts of the target code blocks.
        /// </summary>
        public string SearchTemplate { get; }

        /// <summary>
        /// Message to show when the search pattern was found.
        /// You can also prepend the message text with "Error:", "Warning:", "Suggestion:" or "Hint:" prefix to specify the pattern severity.
        /// Code patterns with replace templates produce suggestions by default.
        /// However, if a replace template is not provided, then warning severity will be used.
        /// </summary>
        public string? Message { get; set; }

        /// <summary>
        /// Structural search replace pattern to use in code template replacement.
        /// </summary>
        public string? ReplaceTemplate { get; set; }

        /// <summary>
        /// The replace message to show in the light bulb.
        /// </summary>
        public string? ReplaceMessage { get; set; }

        /// <summary>
        /// Apply code formatting after code replacement.
        /// </summary>
        public bool FormatAfterReplace { get; set; } = true;

        /// <summary>
        /// Whether similar code blocks should be matched.
        /// </summary>
        public bool MatchSimilarConstructs { get; set; }

        /// <summary>
        /// Automatically insert namespace import directives or remove qualifiers that become redundant after the template is applied.
        /// </summary>
        public bool ShortenReferences { get; set; }

        /// <summary>
        /// The string to use as a suppression key.
        /// By default the following suppression key is used 'CodeTemplate_SomeType_SomeMember',
        /// where 'SomeType' and 'SomeMember' are names of the associated containing type and member to which this attribute is applied.
        /// </summary>
        public string? SuppressionKey { get; set; }
    }
}