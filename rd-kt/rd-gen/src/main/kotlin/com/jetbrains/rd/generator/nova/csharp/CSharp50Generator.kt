package com.jetbrains.rd.generator.nova.csharp

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.Enum
import com.jetbrains.rd.generator.nova.FlowKind.*
import com.jetbrains.rd.generator.nova.csharp.CSharpSanitizer.sanitize
import com.jetbrains.rd.generator.nova.util.capitalizeInvariant
import com.jetbrains.rd.generator.nova.util.decapitalizeInvariant
import com.jetbrains.rd.generator.nova.util.joinToOptString
import com.jetbrains.rd.util.hash.IncrementalHash64
import com.jetbrains.rd.util.string.Eol
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.string.printer
import java.io.File

open class CSharp50Generator(
    flowTransform: FlowTransform = FlowTransform.AsIs,
    val defaultNamespace: String,
    override val folder: File,
    generatedFileSuffix: String = ".Generated"
) : GeneratorBase(flowTransform, generatedFileSuffix) {

    object Inherits : ISetting<String, Declaration>
    object InheritsAutomation : ISetting<Boolean, Declaration>

    object ClassAttributes : ISetting<Array<String>, Declaration>

    //language specific properties
    object Namespace : ISetting<String, IDeclaration>

    val IDeclaration.namespace: String get() = getSetting(Namespace) ?: defaultNamespace

    object FsPath : ISetting<(CSharp50Generator) -> File, Toplevel>

    val Toplevel.fsPath: File
        get() = getSetting(FsPath)?.invoke(this@CSharp50Generator) ?: File(folder, "${this.name}$generatedFileSuffix.cs")

    object FlowTransformProperty : ISetting<FlowTransform, Declaration>

    val Member.Reactive.memberFlowTransform: FlowTransform
        get() = owner.getSetting(FlowTransformProperty) ?: flowTransform

    object AdditionalUsings : ISetting<(CSharp50Generator) -> List<String>, Toplevel>

    val Toplevel.additionalUsings: List<String>
        get() = getSetting(AdditionalUsings)?.invoke(this@CSharp50Generator) ?: emptyList()

    object Intrinsic : SettingWithDefault<CSharpIntrinsicMarshaller, Declaration>(CSharpIntrinsicMarshaller.default)
    object PublicCtors : ISetting<Unit, Declaration>
    object Partial : ISetting<Unit, Declaration>
    object EmitStruct : ISetting<Unit, Struct.Concrete>
    object DontRegisterAllSerializers : ISetting<Unit, Toplevel>


    protected val IType.isPrimitivesArray: Boolean
        get() =
            this is IArray && listOf(
                    PredefinedType.byte,
                    PredefinedType.short,
                    PredefinedType.int,
                    PredefinedType.long,
                    PredefinedType.float,
                    PredefinedType.double,
                    PredefinedType.char,
                    PredefinedType.bool
            ).contains(itemType)


    protected fun IDeclaration.sanitizedName(scope: IDeclaration): String {
        val needQualification =
                namespace != scope.namespace
                        || scope.allMembers.map { it.publicName }.contains(name)
        return needQualification.condstr { "$namespace." } + name
    }


    protected val IType.isValueType: Boolean
        get() =
            this is Enum
                    ||
                    this is PredefinedType.NativeIntegral
                    ||
                    this is PredefinedType.UnsignedIntegral
                    ||
                    this is ValueClass
                    ||
                    this is Struct.Concrete && this.hasSetting(EmitStruct)
                    ||
                    listOf(
                            PredefinedType.float,
                            PredefinedType.double,
                            PredefinedType.char,
                            PredefinedType.bool,

                            PredefinedType.guid,
                            PredefinedType.dateTime,
                            PredefinedType.rdId,
                            PredefinedType.secureString
                            //"string" and "uri" are reference types

                    ).contains(this)


    protected fun Declaration.allTypesForDelegation(): Iterable<IType> {
        fun needDelegate(type: IType, memberIsReactive: Boolean) : Boolean =
                type is IArray && !(type.isPrimitivesArray)
                        || type is IImmutableList
                        || type is INullable
                        || type is InternedScalar
                        || type is Enum && memberIsReactive
                        || type is Declaration && type.isOpen
                        || type is IAttributedType && needDelegate(type.itemType, memberIsReactive)


        return allMembers.flatMap {
            when (it) {
                is Member.Field -> listOf(it.type).filter { needDelegate(it, false) }
                is Member.Reactive -> it.genericParams.filter { needDelegate(it, true) }
                else -> emptyList()
            }
        }.distinct()
    }


    ///types
    protected open fun IType.substitutedName(scope: IDeclaration): String {
        return when (this) {
            is Declaration -> sanitizedName(scope)
            is InternedScalar -> itemType.substitutedName(scope)
            is INullable -> itemType.substitutedName(scope) + itemType.isValueType.condstr { "?" }
            is IAttributedType -> itemType.substitutedName(scope)
            is IArray -> itemType.substitutedName(scope) + "[]"
            is IImmutableList -> "List<${itemType.substitutedName(scope)}>"
            is PredefinedType -> {
                when {
                    listOf(
                            PredefinedType.bool,
                            PredefinedType.byte,
                            PredefinedType.short,
                            PredefinedType.int,
                            PredefinedType.long,
                            PredefinedType.float,
                            PredefinedType.double,
                            PredefinedType.char,
                            PredefinedType.string
                    ).contains(this) -> name.decapitalizeInvariant()
                    this is PredefinedType.UnsignedIntegral -> {
                        if (itemType is PredefinedType.byte) {
                            "byte"
                        } else {
                            "u${itemType.substitutedName(scope)}"
                        }
                    }
                    this == PredefinedType.void -> "Unit"
                    this == PredefinedType.secureString -> "RdSecureString"
                    else -> name
                }
            }

            else -> fail("Unsupported type ${javaClass.simpleName}")
        }
    }

    //declarations
    protected val Declaration.hasSecondaryCtor: Boolean get() = (this.isConcrete || this.isOpen || this is Toplevel) && this.allMembers.any { it.hasEmptyConstructor }

    //members
    val Member.Reactive.actualFlow: FlowKind get() = memberFlowTransform.transform(flow)


    fun Member.needNullCheck(): Boolean {
        fun IType.needNullCheck() : Boolean = when {
            this is IAttributedType -> itemType.needNullCheck()
            (this !is INullable && !this.isValueType) -> true

            else -> false
        }
        return (this !is Member.Field) || this.type.needNullCheck()
    }

    val notnull = "[NotNull]"
    fun Member.nullAttr(isCtorParam: Boolean = false): String {

        fun IType.attr(member: Member) : String = when(this) {
            is INullable -> when(member) {
                is Member.Field -> if (isCtorParam && member.isOptional) "[CanBeNull] " else "[CanBeNull] "
                else -> if (isCtorParam) "[CanBeNull] " else "[CanBeNull] "
            }
            is IAttributedType -> itemType.attr(member)
            else -> if (this.isValueType) "" else "$notnull "
        }

        return when (this) {
            is Member.Reactive.Stateful.Extension -> {
                (this.findDelegate()?.delegateType as? Member.DelegateType.Delegated)?.type?.attr(this)
                    ?: this.delegatedBy.attr(this)
            }
            !is Member.Field -> "$notnull "
            else -> this.type.attr(this)
        }
    }


    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open fun Member.Reactive.intfSimpleName(scope: IDeclaration): String = when (this) {
        is Member.Reactive.Task -> when (actualFlow) {
            Source -> "IRdCall"
            Sink -> "IRdEndpoint"
            Both -> "RdCall"
        }
        is Member.Reactive.Signal -> when (actualFlow) {
            Sink -> if (freeThreaded) "ISignal" else "ISource"
            Source, Both -> "ISignal"
        }
        is Member.Reactive.Stateful.Property -> when (actualFlow) {
            Sink -> "IReadonlyProperty"
            Source, Both -> "IViewableProperty"
        }

        is Member.Reactive.Stateful.AsyncProperty -> when (actualFlow) {
            Sink -> "IReadonlyAsyncProperty"
            Source, Both -> "IAsyncProperty"
        }

        is Member.Reactive.Stateful.List -> when (actualFlow) {
            Sink -> "IViewableList"
            Source, Both -> "IViewableList"
        }
        is Member.Reactive.Stateful.Set -> when (actualFlow) {
            Sink -> "IViewableSet"
            Source, Both -> "IViewableSet"
        }
        is Member.Reactive.Stateful.AsyncSet -> "AsyncRdSet"

        is Member.Reactive.Stateful.Map -> when (actualFlow) {
            Sink -> "IViewableMap"
            Source, Both -> "IViewableMap"
        }

        is Member.Reactive.Stateful.AsyncMap -> "AsyncRdMap"

        is Member.Reactive.Stateful.Extension -> implSimpleName(scope)

        else -> fail("Unsupported member: $this")
    }

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open fun Member.Reactive.implSimpleName(scope: IDeclaration): String = when (this) {
            is Member.Reactive.Task -> "RdCall"
            is Member.Reactive.Signal -> "RdSignal"
            is Member.Reactive.Stateful.Property -> "RdProperty"
            is Member.Reactive.Stateful.AsyncProperty -> "AsyncRdProperty"
            is Member.Reactive.Stateful.List -> "RdList"
            is Member.Reactive.Stateful.Set -> "RdSet"
            is Member.Reactive.Stateful.AsyncSet -> "AsyncRdSet"
            is Member.Reactive.Stateful.Map -> "RdMap"
            is Member.Reactive.Stateful.AsyncMap -> "AsyncRdMap"
            is Member.Reactive.Stateful.Extension -> delegateFqnSubstitutedName(scope)

            else -> fail("Unsupported member: $this")
        }


    protected open fun Member.intfSubstitutedName(scope: Declaration): String = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
        is Member.Reactive -> intfSimpleName(scope) + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) }
        is Member.Const -> type.substitutedName(scope)
        is Member.Method -> publicName
    }

    protected open fun Member.Reactive.intfSubstitutedMapName(scope: Declaration): String =
        "IPerContextMap<${context!!.type.substitutedName(scope)}, ${implSubstitutedName(scope, true)}>"


    protected open fun Member.implSubstitutedName(scope: Declaration, perClientIdRawName: Boolean = false): String = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
        is Member.Const -> type.substitutedName(scope)
        is Member.Reactive -> (implSimpleName(scope) + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) }).let {
            if(context != null && !perClientIdRawName) "RdPerContextMap<${context!!.type.substitutedName(scope)}, $it>" else it
        }
        is Member.Method -> publicName
    }

    protected open fun Member.creationExpressionSubstituted(scope: Declaration) = when (this) {
        is Member.Reactive.Stateful.Extension -> simpleCreationExpression(scope) + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) }
        else -> "new " + implSubstitutedName(scope)
    }

    protected open fun Member.Reactive.simpleCreationExpression(scope: IDeclaration) : String = when (this) {
        is Member.Reactive.Stateful.Extension -> {
            val delegate = findDelegate() ?: fail("Could not find delegate: $this")
            delegate.factoryFqn ?: ("new " + delegate.fqnSubstitutedName(scope))
        }
        else -> implSimpleName(scope)
    }

    protected open val Member.hasEmptyConstructor: Boolean
        get() = when (this) {
            is Member.Field -> type.hasEmptyConstructor && !emptyCtorSuppressed
            is Member.Reactive -> hasEmptyConstructor

            else -> fail("Unsupported member: $this")
        }

    protected open val Member.Reactive.hasEmptyConstructor : Boolean get() = when (this) {
        is Member.Reactive.Stateful.Extension -> delegatedBy.hasEmptyConstructor
        else -> true
    }

    protected open val Member.isBindable: Boolean
        get() = when (this) {
            is Member.Field -> type is IBindable
            is Member.Reactive -> isBindable

            else -> false
        }

    protected open val Member.Reactive.isBindable : Boolean get() = when (this) {
        is Member.Reactive.Stateful.Extension -> when {
            this.delegatedBy !is BindableDeclaration -> false
            (this.findDelegate()?.delegateType as? Member.DelegateType.Delegated)?.type is IBindable -> true
            this.findDelegate()?.delegateType is Member.DelegateType.Custom -> true
            else -> false
        }
        else -> true
    }


    protected open val Member.publicName: String get() = name.capitalizeInvariant()
    protected open val Member.encapsulatedName: String get() = isEncapsulated.condstr { "_" } + publicName
    protected open val Member.isEncapsulated: Boolean get() = when (this) {
        is Member.Reactive.Stateful.Extension -> when {
            isSimplyDelegated(this@CSharp50Generator, memberFlowTransform) -> false
            else -> true
        }
        is Member.Reactive -> true
        else -> false
    }

    protected fun Member.Reactive.customSerializers(containing: Declaration, leadingComma: Boolean, ignorePerClientId: Boolean = false): String {
        if(context != null && !ignorePerClientId)
            return leadingComma.condstr { ", " } + perClientIdMapValueFactory(containing)
        val res = genericParams.joinToString {
            val allowSpecificOpenTypeReference = false
            it.readerDelegateRef(containing, allowSpecificOpenTypeReference) + ", " + it.writerDelegateRef(containing, allowSpecificOpenTypeReference)
        }
        return (genericParams.isNotEmpty() && leadingComma).condstr { ", " } + res
    }

    protected fun Member.Reactive.perClientIdMapValueFactory(containing: Declaration) : String {
        require(this.context != null)
        return "${context!!.longRef(containing)}, isMaster => { var value = new ${this.implSubstitutedName(containing, true)}(${customSerializers(containing, false, true)}${defaultValueAsString(true)}); ${(this is Member.Reactive.Stateful.Map).condstr { "value.IsMaster = isMaster;" }} return value; }"
    }

    protected fun Context.longRef(scope: Declaration): String {
        return when(this) {
            is Context.External -> fqnFor(this@CSharp50Generator)
            is Context.Generated -> pointcut!!.sanitizedName(scope) + "." + sanitizedName(scope) + ".Instance"
        }
    }


    //generation

    override fun realGenerate(toplevels: List<Toplevel>, collector: MarshallersCollector) {
        toplevels.forEach { tl ->
            tl.fsPath.bufferedWriter().use { writer ->
                PrettyPrinter().apply {
                    eolKind = Eol.linux
                    step = 2

                    //actual generation
                    file(tl)

                    writer.write(toString())
                }
            }
        }
    }


    protected open fun PrettyPrinter.file(tl: Toplevel) {
        autogenerated()
        usings(tl)

        println()

        namespace(tl)

        val allTypesWithUnknowns = tl.declaredTypes + unknowns(tl.declaredTypes)

        +"{"
        indent {
            if (tl.isLibrary)
                libdef(tl, allTypesWithUnknowns)
            else
                typedef(tl)

            allTypesWithUnknowns.sortedBy { it.name }.forEach { type ->
                typedef(type)
            }
        }
        +"}"
    }

    protected open fun PrettyPrinter.namespace(decl: Declaration) {
        +"namespace ${decl.namespace}"
    }

    protected open fun PrettyPrinter.autogenerated() {
            +"//------------------------------------------------------------------------------"
            +"// <auto-generated>"
            +"//     This code was generated by a ${RdGen::class.simpleName} v${RdGen.version}."
            +"//"
            +"//     Changes to this file may cause incorrect behavior and will be lost if"
            +"//     the code is regenerated."
            +"// </auto-generated>"
            +"//------------------------------------------------------------------------------"
    }
    protected open fun PrettyPrinter.usings(tl: Toplevel) {
        +"using System;"
        +"using System.Linq;"
        +"using System.Collections.Generic;"
        +"using System.Runtime.InteropServices;"
        +"using JetBrains.Annotations;"
        println()

        +"using JetBrains.Core;"
        +"using JetBrains.Diagnostics;"
        +"using JetBrains.Collections;"
        +"using JetBrains.Collections.Viewable;"
        +"using JetBrains.Lifetimes;"
        +"using JetBrains.Serialization;"
        +"using JetBrains.Rd;"
        +"using JetBrains.Rd.Base;"
        +"using JetBrains.Rd.Impl;"
        +"using JetBrains.Rd.Tasks;"
        +"using JetBrains.Rd.Util;"
        +"using JetBrains.Rd.Text;"
        println()

        println()

        tl.additionalUsings.printlnWithBlankLine {
            "using $it;"
        }
//        tl.referencedTypes.plus(tl.declaredTypes.flatMap { it.referencedTypes })
//            .filterIsInstance(Declaration::class.java)
//            .map {
//                it.namespace
//            }
//            .filterNot { it == tl.namespace }
//            .distinct()
//            .printlnWithBlankLine { "using $it;" }

        +"// ReSharper disable RedundantEmptyObjectCreationArgumentList"
        +"// ReSharper disable InconsistentNaming"
        +"// ReSharper disable RedundantOverflowCheckingContext"
        println()
    }


    protected open fun PrettyPrinter.libdef(decl: Toplevel, types: List<Declaration>) {
        if (decl.getSetting(Intrinsic) != null) return
        +"public static class ${decl.name} {"
        indent {
            registerSerializersTrait(decl, types)
        }
        +"}"
    }




    protected open fun PrettyPrinter.typedef(decl: Declaration) {
        if (decl.getSetting(Intrinsic) != null || decl is Context) return

        println()
        println()

        doc(decl)

        if (decl is Enum) {
            enum(decl)
            return
        }

        if(decl is Interface){
            interfaceDef(decl)
            return
        }

        if (decl is Toplevel && !decl.isExtension) {
            +(decl.getSetting(ClassAttributes)?.joinToOptString(prefix = "[", postfix = "]") ?: "")
        }

        p("public ")

        if(decl !is Toplevel) {
            if (decl.isAbstract) p("abstract ")
            if (decl.isSealed && !decl.hasSetting(EmitStruct)) p("sealed ")
        }
        if (decl.getSetting(Partial) != null) p("partial ")

        if (decl.isValue || decl.hasSetting(EmitStruct)) {
            p("struct ${decl.name}")
        } else {
            p("class ${decl.name}")
        }

        baseClassTrait(decl)

        +"{"
        indent {
            +"//fields"
            fieldsTrait(decl)
            +"//primary constructor"
            primaryConstructor(decl)
            +"//secondary constructor"
            secondaryConstructorTrait(decl)

            +"//deconstruct trait"
            deconstructTrait(decl)

            +"//statics"
            staticsTrait(decl)

            +"//custom body"
            customBodyTrait(decl)
            + "//methods"
            methodsTrait(decl)
            +"//equals trait"
            equalsTrait(decl)
            +"//hash code trait"
            hashCodeTrait(decl)
            +"//pretty print"
            prettyPrintTrait(decl)
            +"//toString"
            toStringTrait(decl)
        }
        +"}"

        if (decl.isExtension) {
            extensionTrait(decl as Ext)
        }
    }

    protected open fun PrettyPrinter.doc(decl: Declaration) {
        if (decl.documentation != null || decl.sourceFileAndLine != null) {
            + "/// <summary>"
            decl.documentation?.lines()?.forEach {
                + "/// $it"
            }
            decl.sourceFileAndLine?.let {
                + "/// <p>Generated from: $it</p>"
            }
            + "/// </summary>"
        }
    }

    private fun docComment(doc: String?) = (doc != null).condstr {
        "\n" +
                "/// <summary>\n" +
                "/// $doc\n" +
                "/// </summary>\n"
    }

    protected fun PrettyPrinter.staticsTrait(decl: Declaration) {

        println()
        readerAndDelegatesTrait(decl)

        println()
        writerAndDelegatesTrait(decl)


        if (decl is Toplevel) {
            println()
            +"protected override long SerializationHash => ${decl.serializationHash(IncrementalHash64()).result}L;"
            println()
            registerSerializersTrait(decl, decl.declaredTypes + unknowns(decl.declaredTypes))
            println()
            createMethodTrait(decl)
        }

        println()
        +"//constants"
        constantTrait(decl)
        println()
    }

    private fun getDefaultValue(containing: Declaration, member: Member, ignorePerClientId: Boolean = false): String? =
        if (!ignorePerClientId && member is Member.Reactive && member.context != null)
            null
        else

            when (member) {
                is Member.Reactive.Stateful.PropertyBase -> when {
                    member.defaultValue is String -> "\"" + member.defaultValue + "\""
                    member.defaultValue is Member.Const -> member.defaultValue.name
                    member.defaultValue != null -> member.defaultValue.toString()
                    member.isNullable -> "null"
                    else -> null
                }
                is Member.Const -> {
                    val value = member.value
                    when (member.type) {
                        is PredefinedType.string -> "\"$value\""
                        is PredefinedType.char -> "'$value'"
                        is PredefinedType.long -> "${value}L"
                        is PredefinedType.uint -> "${value}U"
                        is PredefinedType.ulong -> "${value}UL"
                        is PredefinedType.float -> "${value}F"
                        is Enum -> "${member.type.substitutedName(containing)}.${sanitize(value)}"
                        else -> value
                    }
                }
                is Member.Reactive.Stateful.Extension -> when (member.delegatedBy) {
                    is ITypeDeclaration -> "new " + member.delegatedBy.sanitizedName(containing) + "()"
                    else -> null
                }
                else -> null
            }

    protected fun PrettyPrinter.constantTrait(decl: Declaration) {
        decl.constantMembers.forEach {
            val value = getDefaultValue(decl, it)
            +"public const ${it.type.substitutedName(decl)} ${it.name} = $value;"
        }
        if(decl is Toplevel) {
            decl.declaredTypes.forEach {
                if(it is Context.Generated) {
                    val keyTypeName = "${it.contextImplementationFqn}<${it.type.substitutedName(decl)}>"
                    +"public class ${it.keyName} : $keyTypeName {"
                    indent {
                        +"private ${it.keyName}() : base(\"${it.keyName}\", ${it.isHeavyKey}, ${it.type.readerDelegateRef(decl, false)}, ${it.type.writerDelegateRef(decl,false)}) {}"
                        +"public static readonly ${it.keyName} Instance = new ${it.keyName}();"
                        +"protected override void RegisterOn(ISerializers serializers)"
                        +"{"
                        +"serializers.Register((_, __) => Instance, (_, __, ___) => { });"
                        +"}"
                    }
                    +"}"
                }
            }
        }
    }

    protected fun PrettyPrinter.registerSerializersTrait(decl: Toplevel, declaredAndUnknownTypes: List<Declaration>) {
        if (!decl.isLibrary)
            +"protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;"

        +"public static void RegisterDeclaredTypesSerializers(ISerializers serializers)"
        +"{"
        indent {
            val internedTypes = declaredAndUnknownTypes.flatMap { it.referencedTypes }.filterIsInstance<InternedScalar>().map { it.itemType }
            val typesUnderPerClientIdMembers = declaredAndUnknownTypes.flatMap { it.ownMembers }.filterIsInstance<Member.Reactive>().filter { it.context != null }.flatMap { it.referencedTypes }

            val allTypesForRegistration = declaredAndUnknownTypes.filter { it.base != null || it.isOpen } +
                    internedTypes.filterIsInstance<Declaration>() + typesUnderPerClientIdMembers.filterIsInstance<Declaration>()

            allTypesForRegistration.filter { !it.isAbstract }.distinct().println {
                if (it is IType)
                    "serializers.Register(${it.readerDelegateRef(decl, true)}, ${it.writerDelegateRef(decl, true)});"
                else
                    fail("Unsupported declaration in register: $it")
            }
            println()

            if (decl.getSetting(DontRegisterAllSerializers) == null) {
                val invocationPattern = { typeName: String -> "serializers.RegisterToplevelOnce(typeof($typeName), $typeName.RegisterDeclaredTypesSerializers);" }

                if (decl is Root) {
                    decl.toplevels.println { invocationPattern(it.sanitizedName(decl)) }
                } else {
                    +invocationPattern(decl.root.sanitizedName(decl))
                }
            }
        }
        +"}"

    }

    //only for non-extensions
    protected fun PrettyPrinter.createMethodTrait(decl: Toplevel) {
        if (decl.isExtension) return

        +"public ${decl.name}(Lifetime lifetime, IProtocol protocol) : this()"
        +"{"

        indent {
            +"Identify(protocol.Identities, protocol.Identities.Mix(RdId.Root, \"${decl.name}\"));"
            +"this.BindTopLevel(lifetime, protocol, \"${decl.name}\");" //better than nameof(${decl.name}) because one could rename generated class and it'll still able to connect to Kt
        }
        +"}"
    }


    fun IType.readerDeclaredElsewhereDelegateRef(containing: Declaration, allowSpecificOpenTypeReference: Boolean): String? = when (this) {
        is Enum -> null //to overwrite Declaration
        is PredefinedType -> "JetBrains.Rd.Impl.Serializers.Read$name"
        is Declaration -> if(this.isOpen && !allowSpecificOpenTypeReference) null else (this.getSetting(Intrinsic)?.readDelegateFqn ?: "${sanitizedName(containing)}.Read")
        is IArray -> if (this.isPrimitivesArray) "JetBrains.Rd.Impl.Serializers.Read$name" else null
        is IAttributedType -> itemType.readerDeclaredElsewhereDelegateRef(containing, allowSpecificOpenTypeReference)
        else -> null
    }

    fun IType.readerDelegateRef(containing: Declaration, allowSpecificOpenTypeReference: Boolean) = readerDeclaredElsewhereDelegateRef(containing, allowSpecificOpenTypeReference)
            ?: when (this) {
                is InternedScalar -> "Read${name}At${internKey.keyName}"
                else -> "Read$name" //must be constructed here
            }

    protected fun PrettyPrinter.readerAndDelegatesTrait(decl: Declaration) {

        fun IType.complexDelegateBuilder(): String = readerDeclaredElsewhereDelegateRef(decl, false) ?: when (this) {
            is Enum -> "new CtxReadDelegate<${sanitizedName(decl)}>(JetBrains.Rd.Impl.Serializers.ReadEnum<${sanitizedName(decl)}>)"
            is IArray -> itemType.complexDelegateBuilder() + ".Array()"
            is InternedScalar -> itemType.complexDelegateBuilder() + ".Interned(\"${internKey.keyName}\")"
            is IImmutableList -> itemType.complexDelegateBuilder() + ".List()"
            is INullable -> itemType.complexDelegateBuilder() +
                    ".Nullable" + (if (itemType.isValueType) "Struct" else "Class") + "()"
            is IAttributedType -> itemType.complexDelegateBuilder()
            is Declaration -> {
                assert(isOpen)
                "Polymorphic<${sanitizedName(decl)}>.ReadAbstract(${sanitizedName(decl)}_Unknown.Read)"
            }
            else -> fail("Unknown type: $this")
        }


        fun IType.reader(): String = when (this) {
            is Enum -> "(${sanitizedName(decl)})reader.ReadInt()"
            is PredefinedType -> "reader.Read$name()"
            is InternedScalar -> "ctx.ReadInterned(reader, \"${internKey.keyName}\", ${itemType.complexDelegateBuilder()})"
            is IAttributedType -> itemType.reader()
            else -> readerDelegateRef(decl, false) + "(ctx, reader)"
        }

        fun Member.Reactive.Stateful.Extension.extReader(): String = when {
            isSimplyDelegated(this@CSharp50Generator, memberFlowTransform) -> delegatedBy.reader()
            else -> "${creationExpressionSubstituted(decl)}(${delegatedBy.reader()})"
        }

        fun Member.reader(): String = when (this) {
            is Member.Field -> type.reader()
            is Member.Reactive.Stateful.Extension -> extReader()
            is Member.Reactive -> "${implSubstitutedName(decl)}.Read(ctx, reader${customSerializers(decl, leadingComma = true)})"

            else -> fail("Unknown member: $this")
        }


        val modifiers = "public static" + (decl.base?.let { " new" } ?: "")

        if (decl.isAbstract) {
            +"$modifiers CtxReadDelegate<${decl.name}> Read = Polymorphic<${decl.name}>.ReadAbstract(${decl.name}_Unknown.Read);"
            return
        }


        if (decl is Struct || decl is ValueClass || decl is Class || decl is Aggregate) {
            +"$modifiers CtxReadDelegate<${decl.name}> Read = (ctx, reader) => "
            +"{"
            indent {
                if (decl is Class || decl is Aggregate) {
                    +"var _id = RdId.Read(reader);"
                }
                (decl.membersOfBaseClasses + decl.ownMembers).println { "var ${sanitize(it.name, "ctx", "reader")} = ${it.reader()};" }
                p("var _result = new ${decl.name}(${decl.allMembers.joinToString(", ") { sanitize(it.name, "ctx", "reader") }})")
                if (decl is Class || decl is Aggregate) {
                    p(".WithId(_id)")
                }
                +(";")
                if (decl is Class && decl.internRootForScopes.isNotEmpty()) {
                    +"_result.mySerializationContext = ctx.WithInternRootsHere(_result, ${decl.internRootForScopes.joinToString { "\"$it\"" }});"
                }
                +"return _result;"
            }
            +"};"
        }

        decl.allTypesForDelegation().map {
            "public static${it.hideOverloadAttribute(decl)} CtxReadDelegate<${it.substitutedName(decl)}> ${it.readerDelegateRef(decl, false)} = ${it.complexDelegateBuilder()};"
        }.distinct().forEach { + it}
    }


    fun IType.writerDeclaredElsewhereDelegateRef(containing: Declaration, allowSpecificOpenTypeReference: Boolean): String? = when (this) {
        is Enum -> null //to overwrite Declaration
        is PredefinedType -> "JetBrains.Rd.Impl.Serializers.Write$name"
        is Declaration -> if(isOpen && !allowSpecificOpenTypeReference) null else (this.getSetting(Intrinsic)?.writeDelegateFqn ?: "${sanitizedName(containing)}.Write")
        is IArray -> if (this.isPrimitivesArray) "JetBrains.Rd.Impl.Serializers.Write$name" else null
        is IAttributedType -> itemType.writerDeclaredElsewhereDelegateRef(containing, allowSpecificOpenTypeReference)
        else -> null
    }

    fun IType.writerDelegateRef(containing: Declaration, allowSpecificOpenTypeReference: Boolean) = writerDeclaredElsewhereDelegateRef(containing, allowSpecificOpenTypeReference)
            ?: when (this) {
                is InternedScalar -> "Write${name}At${internKey.keyName}"
                else -> "Write$name" //must be constructed here
            }

    protected fun PrettyPrinter.writerAndDelegatesTrait(decl: Declaration) {

        fun IType.complexDelegateBuilder(): String = writerDeclaredElsewhereDelegateRef(decl, false) ?: when (this) {
            is Enum -> "new CtxWriteDelegate<${sanitizedName(decl)}>(JetBrains.Rd.Impl.Serializers.WriteEnum<${sanitizedName(decl)}>)"
            is IArray -> itemType.complexDelegateBuilder() + ".Array()"
            is IImmutableList -> itemType.complexDelegateBuilder() + ".List()"
            is InternedScalar -> itemType.complexDelegateBuilder() + ".Interned(\"${internKey.keyName}\")"
            is INullable -> itemType.complexDelegateBuilder() +
                    ".Nullable" + (if (itemType.isValueType) "Struct" else "Class") + "()"
            is IAttributedType -> itemType.complexDelegateBuilder()
            is Declaration -> {
                assert(isOpen)
                "Polymorphic<${sanitizedName(decl)}>.Write"
            }
            else -> fail("Unknown type: $this")
        }


        fun IType.writer(field: String): String = when (this) {
            is Enum -> "writer.Write((int)$field)"
            is PredefinedType -> "writer.Write($field)"
            is InternedScalar -> "ctx.WriteInterned(writer, $field, \"${internKey.keyName}\", ${itemType.complexDelegateBuilder()})"
            is IAttributedType -> itemType.writer(field)
            else -> writerDelegateRef(decl, false) + "(ctx, writer, $field)"
        }

        fun Member.writer(): String = when (this) {
            is Member.Field -> type.writer("value.$encapsulatedName")
            is Member.Reactive.Stateful.Extension -> when (findDelegate()?.delegateType) {
                is Member.DelegateType.Delegated -> delegatedBy.writer(("value.$encapsulatedName"))
                else -> delegatedBy.writer(("value.$encapsulatedName.Delegate"))
            }
            is Member.Reactive -> "${implSubstitutedName(decl)}.Write(ctx, writer, value.$encapsulatedName)"

            else -> fail("Unknown member: $this")
        }


        val modifiers = "public static" + (decl.base?.let { " new" } ?: "")
        if (decl.isAbstract) {
            +"$modifiers CtxWriteDelegate<${decl.name}> Write = Polymorphic<${decl.name}>.Write;"
            return
        }


        if (decl is Struct || decl is ValueClass || decl is Class || decl is Aggregate) {
            +"$modifiers CtxWriteDelegate<${decl.name}> Write = (ctx, writer, value) => "
            +"{"
            indent {
                if (decl is Class || decl is Aggregate) {
                    +"value.RdId.Write(writer);"
                }
                (decl.membersOfBaseClasses + decl.ownMembers).println { it. writer() + ";" }
                if (decl is Class && decl.internRootForScopes.isNotEmpty()) {
                    +"value.mySerializationContext = ctx.WithInternRootsHere(value, ${decl.internRootForScopes.joinToString { "\"$it\"" }});"
                }
            }
            +"};"
        }

        decl.allTypesForDelegation().map {
            "public static ${it.hideOverloadAttribute(decl)} CtxWriteDelegate<${it.substitutedName(decl)}> ${it.writerDelegateRef(decl, false)} = ${it.complexDelegateBuilder()};"
        }.distinct().forEach { + it }
    }

    private fun IType.hideOverloadAttribute(decl : Declaration): String{
        var currentDecl = decl
        while (currentDecl.base != null){
            currentDecl = currentDecl.base!!
            if(currentDecl.isOpen && currentDecl.allTypesForDelegation().contains(this)){
                return " new"
            }

        }

        return ""
    }


    protected fun PrettyPrinter.fieldsTrait(decl: Declaration) {

        fun IType.attrsStr() = (this as? IAttributedType)?.getAttrsStr()?.let { "$it " } ?: ""
        fun Member.getSetTrait(prefix: String) = "$prefix ${this.intfSubstitutedName(decl)} ${this.publicName} {get; private set;}"

        +"//public fields"
        for (member in decl.ownMembers) {
            p(docComment(member.documentation))
            val prefix = member.nullAttr() + "public"
            when (member) {
                is Member.Reactive -> when {
                    member is Member.Reactive.Stateful.Extension && !member.isEncapsulated -> {
                        val attrStr = (member.findDelegate()?.delegateType as? Member.DelegateType.Delegated)
                            ?.type?.attrsStr() ?: ""
                        +"${attrStr}${member.getSetTrait(prefix)}"
                    }
                    member is Member.Reactive.Signal && member.actualFlow == Source -> {
                        val type = member.referencedTypes[0]
                        val isNotVoid = type != PredefinedType.void
                        +"$prefix void ${member.publicName}(${isNotVoid.condstr { type.substitutedName(decl) + " value" }}) => ${member.encapsulatedName}.Fire(${isNotVoid.condstr { "value" }});"
                    }
                    else -> {
                        if (member.context != null) {
                            + "$prefix ${member.intfSubstitutedName(decl)} ${member.publicName} => ${member.encapsulatedName}.GetForCurrentContext();"
                            + "$prefix ${member.intfSubstitutedMapName(decl)} ${member.publicName}PerContextMap => ${member.encapsulatedName};"
                        } else
                            +"$prefix ${member.intfSubstitutedName(decl)} ${member.publicName} => ${member.encapsulatedName};"
                    }
                }
                is Member.Field -> {
                    +"${member.type.attrsStr()}${member.getSetTrait(prefix)}"
                }
                else -> fail("Unsupported member: $member")
            }
        }
        println()

        +"//private fields"
        decl.ownMembers
            .filterIsInstance<Member.Reactive>()
            .filter { it.isEncapsulated }
            .printlnWithBlankLine {
            it.nullAttr() + (if (decl.isSealed) "private" else "protected") + " readonly ${it.implSubstitutedName(decl)} ${it.encapsulatedName};"
        }

        if (decl is Class && decl.internRootForScopes.isNotEmpty()) {
            +"private SerializationCtx mySerializationContext;"
            +"public override bool TryGetSerializationContext(out SerializationCtx ctx) { ctx = mySerializationContext; return true; }"
        }
    }


    protected fun PrettyPrinter.customBodyTrait(decl: Declaration) {
        if (decl.getSetting(InheritsAutomation) == true) {
            +"public override event System.ComponentModel.PropertyChangedEventHandler PropertyChanged;"
        }
    }

    private fun PrettyPrinter.printConstructorParameterList(decl: Declaration, members: List<Member>) {
        fun getDefaultValue(member: Member, typeName: String): String? {
            // Returns "null" for defaultValue = null (i.e. for pure `optional` fields). Returns actual `null` if there
            // is no default value.
            if (member is Member.Field && isOptional(member)) {
                return member.defaultValue.let { defaultValue ->
                    when (defaultValue) {
                        is String ->
                            if (member.type is Enum) {
                                if (member.type.flags && defaultValue.isEmpty())
                                    "($typeName)0"
                                else
                                    "$typeName.$defaultValue"
                            } else {
                                "\"$defaultValue\""
                            }
                        else -> defaultValue.toString()
                    }
                }
            }

            return null
        }

        fun PrettyPrinter.printDefaultValueWithAttributes(member: Member, typeName: String) {
            getDefaultValue(member, typeName)?.let { defaultValue ->
                if (defaultValue != "null") {
                    p("[Optional] [DefaultParameterValue($defaultValue)] ")
                } else {
                    p("[Optional] ")
                }
            }
        }

        fun PrettyPrinter.printDefaultValueAssignment(member: Member, typeName: String) {
            getDefaultValue(member, typeName)?.let { defaultValue ->
                p(" = ")
                p(defaultValue)
            }
        }

        val indexAfterWhichAllHaveDefaultValues = members.withIndex()
            .reversed()
            .dropWhile {
                val member = it.value
                member is Member.Field && isOptional(member)
            }.firstOrNull()?.index ?: -1
        +members.withIndex()
            .joinToString(",\r\n") {
                val member = it.value
                val typeName = member.implSubstitutedName(decl)
                val attributes = member.getIncludedTypeAttributes()?:""

                printer {
                    p(attributes)
                    p(member.nullAttr(true)) // [Null], [NotNull], [CanBeNull]
                    if (it.index < indexAfterWhichAllHaveDefaultValues)
                        printDefaultValueWithAttributes(member, typeName)
                    p(typeName)
                    p(" ")
                    p(sanitize(member.name))
                    if (it.index > indexAfterWhichAllHaveDefaultValues)
                        printDefaultValueAssignment(member, typeName)
                }.toString()
            }
    }

    private fun isOptional(member: Member.Field) =
        (member.isOptional || member.defaultValue != null)

    protected fun PrettyPrinter.secondaryConstructorTrait(decl: Declaration) {
        if (!decl.hasSecondaryCtor) return

        val accessModifier = when {
            decl.hasSetting(PublicCtors) -> "public"
            decl.isExtension -> "internal"
            decl is Toplevel -> "private"
            else -> "public"
        }


        +"$accessModifier ${decl.name} ("
        indent {
            printConstructorParameterList(
                decl,
                decl.allMembers.filter { !it.hasEmptyConstructor }
            )
        }
        +") : this ("
        indent {
            +decl.allMembers
                    .joinToString(",\n") {
                        if (!it.hasEmptyConstructor) {
                            sanitize(it.name)
                        } else {
                            val arguments = (it as? Member.Reactive)?.customSerializers(decl, leadingComma = false) ?: ""
                            val defValue = when (val rawDefaultValue = getDefaultValue(decl, it)) {
                                "null" -> ""
                                null -> ""
                                else -> (if (arguments.isEmpty()) "" else ", ") + rawDefaultValue
                            }
                            "${it.creationExpressionSubstituted(decl)}($arguments$defValue)"
                        }
                    }
        }
        +") {}"
    }

    private fun PrettyPrinter.deconstructTrait(decl: Declaration) {
        if (decl.isDataClass || decl.isValue || (decl.isConcrete && decl.base == null && decl.hasSetting(AllowDeconstruct))) {
            val params = decl.ownMembers.joinToString {
                "${it.nullAttr(false)}out ${it.implSubstitutedName(decl)} ${sanitize(it.name)}"
            }
            +"public void Deconstruct($params)"
            +"{"
            indent {
                decl.ownMembers.println { "${sanitize(it.name)} = ${it.encapsulatedName};" }
            }
            +"}"
        }
    }

    private fun Member.defaultValueAsString(ignorePerClientId: Boolean = false): String {
        return if (this is Member.Reactive.Stateful.PropertyBase && defaultValue != null && (context == null || ignorePerClientId)) {
            when (defaultValue) {
                is String -> ", \"$defaultValue\""
                is Member.Const -> ", ${defaultValue.name}"
                else -> ", $defaultValue"
            }
        } else
            ""
    }


    private fun PrettyPrinter.equalsTrait(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

        fun IType.eq(fieldName: String, member: Member): String = when (this) {
            !is IScalar -> fail("Field $decl.`$member` must have scalar type but was $this")
            is IArray, is IImmutableList -> "$fieldName.SequenceEqual(other.$fieldName)"
            is Enum, is PredefinedType -> "$fieldName == other.$fieldName"
            is IAttributedType -> itemType.eq(fieldName, member)
            else -> "Equals($fieldName, other.$fieldName)"
        }


        +"public override bool Equals(object obj)"
        +"{"
        indent {
            +"if (ReferenceEquals(null, obj)) return false;"
            +"if (ReferenceEquals(this, obj)) return true;"
            +"if (obj.GetType() != GetType()) return false;"
            +"return Equals((${decl.name}) obj);"
        }
        +"}"


        +"public bool Equals(${decl.name} other)"
        +"{"
        indent {
            if (!(decl.isValue || decl.hasSetting(EmitStruct))) {
                +"if (ReferenceEquals(null, other)) return false;"
                +"if (ReferenceEquals(this, other)) return true;"
            }
            val res = decl.allMembers.mapNotNull { m ->
                when (m) {
                    is Member.Field -> {
                        if (m.usedInEquals) Triple(m.encapsulatedName, m.type, m)
                        else null
                    }

                    is Member.Reactive.Stateful.Extension -> Triple(m.encapsulatedName, m.delegatedBy, m)
                    else -> fail("Must be field but was `$m`")
                }
            }.joinToString(" && ") { (name, type, member) -> type.eq(name, member) }
                .takeIf { it.isNotBlank() } ?: "true"

            +"return $res;"
        }
        +"}"

        if (decl.isValue) {
            +"public static bool operator ==(${decl.name} left, ${decl.name} right)"
            +"{"
            indent {
                +"return left.Equals(right);"
            }
            +"}"

            +"public static bool operator !=(${decl.name} left, ${decl.name} right)"
            +"{"
            indent {
                +"return !left.Equals(right);"
            }
            +"}"
        }
    }


    private fun PrettyPrinter.hashCodeTrait(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

        fun IScalar.hc(v: String, m: Member): String = when (this) {
            is Enum -> "(int) $v"
            is IArray, is IImmutableList -> "$v.ContentHashCode()"
            is INullable -> "($v != null ? " + (itemType as IScalar).hc(v, m) + " : 0)"
            is ScalarAttributedType<IScalar> -> (itemType as? IScalar)?.hc(v, m) ?: fail("Field $decl.`$m` must have scalar type but was $itemType")
            else -> "$v.GetHashCode()"
        }


        +"public override int GetHashCode()"
        +"{"
        indent {
            +"unchecked {"
            indent {
                +"var hash = 0;"

                decl.allMembers.println { m ->
                    when (m) {
                        is Member.Field -> {
                            val t = m.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${m.type}")
                            if (!m.usedInEquals) null
                            else t
                        }
                        is Member.Reactive.Stateful.Extension -> {
                            m.delegatedBy.let { delegatedBy ->
                                delegatedBy as? IScalar ?: fail("Extension $decl.`$m` must have scalar type but was $delegatedBy")
                            }
                        }
                        else -> fail("Must be field but was `$m`")
                    }?.let { t -> "hash = hash * 31 + ${t.hc(m.encapsulatedName, m)};" } ?: ""
                }

                +"return hash;"
            }
            +"}"
        }
        +"}"
    }


    private fun PrettyPrinter.prettyPrintTrait(decl: Declaration) {
        if (!(decl is Toplevel || decl.isConcrete || decl.isOpen || decl.isValue)) return

        fun Declaration.attributes() : String{
            if(decl !is Struct && decl !is ValueClass) return "override "

            var currentDecl = this
            while (currentDecl.base != null){
                currentDecl = currentDecl.base!!
                if(currentDecl.isOpen){
                    return "override "
                }
            }

            return if(decl.isOpen) "virtual " else ""
        }

        +"public ${decl.attributes()}void Print(PrettyPrinter printer)"
        +"{"
        indent {
            +"printer.Println(\"${decl.name} (\");"
            decl.allMembers.printlnWithPrefixSuffixAndIndent("using (printer.IndentCookie()) {", "}") { "printer.Print(\"${it.name} = \"); ${it.encapsulatedName}.PrintEx(printer); printer.Println();" }
            +"printer.Print(\")\");"
        }
        +"}"
    }

    private fun PrettyPrinter.toStringTrait(decl: Declaration) {
        if (!(decl is Toplevel || decl.isConcrete || decl.isOpen || decl.isValue)) return

        +"public override string ToString()"
        +"{"
        indent {
            +"var printer = new SingleLinePrettyPrinter();"
            +"Print(printer);"
            +"return printer.ToString();"
        }
        +"}"
    }

    private fun IAttributedType.getAttrsStr(): String? =
        this.attributes.getOrDefault(Lang.CSharp, null)?.toTypedArray()
            ?.joinToOptString(", ", "[", "]") { it }

    private fun Member.getIncludedTypeAttributes(): String? = when(this) {
        is Member.Field -> (type as? IAttributedType)?.getAttrsStr()
        is Member.Const -> (type as? IAttributedType)?.getAttrsStr()

        else -> null
    }


    private fun PrettyPrinter.primaryConstructor(decl: Declaration) {
        if (decl !is Toplevel && decl.allMembers.isEmpty()) return //no constructors

        fun IType.isNullable() : Boolean = when(this) {
            is INullable -> true
            is IAttributedType -> itemType.isNullable()
            else -> false
        }

        val accessModifier = when {
            decl.hasSetting(PublicCtors) -> "public"
            decl.isAbstract -> "protected"
            decl.isOpen && decl.hasSecondaryCtor -> "protected"
            decl.hasSecondaryCtor -> "private"
            decl.isExtension -> "internal"
            decl is Toplevel -> "private"
            else -> "public"
        }

        +"$accessModifier ${decl.name}("
        indent {
            printConstructorParameterList(decl, decl.allMembers)
        }
        p(")")
        val base = decl.base
        if (base != null && !base.allMembers.isEmpty()) {
            +" : base ("
            indent {
                +base.allMembers.joinToString(",\r\n") { sanitize(it.name) }
            }
            p(" ) ")
        }
        println()

        +"{"
        indent {
            decl.ownMembers.filter { it.needNullCheck() }.printlnWithBlankLine { "if (${sanitize(it.name)} == null) throw new ArgumentNullException(\"${it.name}\");" }

            decl.ownMembers.println { "${it.encapsulatedName} = ${sanitize(it.name)};" }

            decl.ownMembers
                    .filterIsInstance<Member.Reactive.Stateful>()
                    .filter { it !is Member.Reactive.Stateful.Extension && it.genericParams.none { it is IBindable } && it.context == null}
                    .println { "${it.encapsulatedName}.OptimizeNested = true;" }

//            decl.ownMembers
//                    .filterIsInstance<Member.Reactive.Stateful.Property>()
//                    .filter { it.perContextKey == null }
//                    .println { "${it.encapsulatedName}.IsMaster = ${it.master};" }
//
//            decl.ownMembers
//                    .filterIsInstance<Member.Reactive.Stateful.Map>()
//                    .filter { it.perContextKey == null }
//                    .println { "${it.encapsulatedName}.IsMaster = ${it.master};" }

            decl.ownMembers
                    .filterIsInstance<Member.Reactive>()
                    .filter { it.freeThreaded }
                    .println { "${it.encapsulatedName}.Async = true;" }

            decl.ownMembers
                    .filterIsInstance<Member.Reactive>()
                    .filter { it.genericParams.any { it.isNullable() } }.println { "${it.encapsulatedName}.ValueCanBeNull = true;" }

            decl.ownMembers
                    .filter { it.isBindable }
                    .println { """BindableChildren.Add(new KeyValuePair<string, object>("${it.name}", ${it.encapsulatedName}));""" }
        }
        +"}"
    }


    protected open fun PrettyPrinter.baseClassTrait(decl: Declaration) {

        val base = decl.base
        val baseClassesStr =
                if (base == null) {
                    when (decl) {
                        is Toplevel -> "RdExtBase"
                        is BindableDeclaration -> "RdBindableBase"
                        is Struct.Concrete, is ValueClass -> "IPrintable, IEquatable<${decl.name}>"
                        else -> "" //abstract struct doesn't implement these methods, enum must not reach this place
                    }
                } else {
                    base.sanitizedName(decl)
                }

        var res = baseClassesStr +
                (decl.getSetting(Inherits)?.let { ", $it" } ?: "") +
                (if (decl.getSetting(InheritsAutomation) == true) ", JetBrains.Application.UI.UIAutomation.IAutomation" else "") //todo remove

        if (res.startsWith(','))
            res = res.substring(1)

        val prefix = if(res.isBlank()) "" else ", "
        res += if (decl.implements.isNotEmpty()) decl.implements.joinToString(prefix = prefix) { it.sanitizedName(decl) } else ""

        if (!res.isBlank()) {
            +" : $res"
        }
    }


    protected open fun PrettyPrinter.enum(decl: Enum) {
        if (decl.flags)
            +"[Flags]"
        +"public enum ${decl.name} {"
        indent {
            +decl.constants.withIndex().joinToString(separator = ",\r\n") { (idx, enumConst) ->
                docComment(enumConst.documentation) + sanitize(enumConst.name) + decl.flags.condstr { " = 1 << $idx" }
            }
        }
        +"}"
    }

    protected open fun PrettyPrinter.interfaceDef(decl: Interface) {
        +"public interface ${decl.name.capitalizeInvariant()}"
        indent {baseInterfacesTrait(decl) }
        p("{")
        println()
        indent { methodsTrait(decl) }
        p("}")
    }

    protected open fun PrettyPrinter.baseInterfacesTrait(decl: Interface) {
        if (decl.baseInterfaces.isNotEmpty()) {
            +" : ${decl.baseInterfaces.joinToString { it.name }}"
        }
    }

    protected open fun PrettyPrinter.methodsTrait(decl: Interface) {
        decl.ownMembers.filterIsInstance<Member.Method>().forEach { method ->
            +"${if(method.resultType == PredefinedType.void) "void" else method.resultType.substitutedName(decl)} ${method.name.capitalizeInvariant()}(${method.args.joinToString { t -> "${t.second.substitutedName(decl)} ${t.first}"}});"
        }
    }

    protected open fun PrettyPrinter.methodsTrait(decl: Declaration){
        decl.implements.forEach { inter->
            inter.allMembers.filterIsInstance<Member.Method>().forEach { method->
                methodTrait(method, decl, isAbstract = decl.isAbstract)
            }
        }

        if (decl.isSealed) {
            var currentDecl = decl.base
            while (currentDecl != null && currentDecl.isAbstract) {
                currentDecl.implements.forEach { inter ->
                    inter.allMembers.filterIsInstance<Member.Method>().forEach {
                        methodTrait(it, decl, isAbstract = false)
                    }
                }
                currentDecl = currentDecl.base
            }
        }
    }

    private fun Member.Reactive.Stateful.Extension.findDelegate() = findDelegate(this@CSharp50Generator, memberFlowTransform)

    protected open fun Member.ExtensionDelegate.fqnSubstitutedName(scope: IDeclaration): String = this.delegateType.let { delegateType ->
        when (delegateType) {
            is Member.DelegateType.Custom -> delegateType.fqn
            is Member.DelegateType.Delegated -> delegateType.type.substitutedName(scope)
        }
    }

    protected open fun Member.Reactive.Stateful.Extension.delegateFqnSubstitutedName(scope: IDeclaration): String = findDelegate()?.fqnSubstitutedName(scope)
        ?: this.javaClass.simpleName

    protected open fun PrettyPrinter.methodTrait(method: Member.Method, decl: Declaration ,isAbstract: Boolean) {
        println("public ${isAbstract.condstr { "abstract " }}${isUnknown(decl).condstr { "override " }}${if (method.resultType == PredefinedType.void) "void" else method.resultType.substitutedName(decl)} ${method.name.capitalizeInvariant()}(${method.args.joinToString { t -> "${t.second.substitutedName(decl)} ${t.first}" }})${isAbstract.condstr { ";" }}")

        if (isAbstract) return
        println("{")
        indent {
            println("throw new NotImplementedException(\"You should implement method in derived class\");")
        }
        println("}")
        println()
    }

    private fun isUnknown(decl: Declaration) = decl is Class.Concrete && decl.isUnknown || decl is Struct.Concrete && decl.isUnknown

    private fun PrettyPrinter.extensionTrait(decl: Ext) {
        val pointcut = decl.pointcut ?: return
        val ownerLowerName = pointcut.name.decapitalizeInvariant()

        +"public static class ${pointcut.name}${decl.name}Ex"
        +" {"
        indent {
            val lowerName = decl.name.decapitalizeInvariant()
            val extName = decl.extName?.capitalizeInvariant() ?: decl.name
            +"public static ${decl.name} Get$extName(this ${pointcut.sanitizedName(decl)} $ownerLowerName)"
            +"{"
            indent {
                +"""return $ownerLowerName.GetOrCreateExtension("$lowerName", () => new ${decl.name}());"""
            }
            +"}"
        }
        +"}"
    }

    override fun toString(): String {
        return "CSharp50($flowTransform, \"$defaultNamespace\", '${folder.canonicalPath}')"
    }


}
