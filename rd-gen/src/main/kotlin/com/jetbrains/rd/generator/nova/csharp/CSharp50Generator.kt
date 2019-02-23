package com.jetbrains.rd.generator.nova.csharp

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.Enum
import com.jetbrains.rd.generator.nova.FlowKind.*
import com.jetbrains.rd.generator.nova.util.joinToOptString
import com.jetbrains.rd.util.hash.IncrementalHash64
import com.jetbrains.rd.util.string.Eol
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.string.printer
import java.io.File

open class CSharp50Generator(val defaultFlowTransform: FlowTransform, val defaultNamespace: String, override val folder : File, val fileName: (Toplevel) -> String = { tl -> tl.name}) : GeneratorBase() {


    object Inherits : ISetting<String, Declaration>
    object InheritsAutomation : ISetting<Boolean, Declaration>

    object ClassAttributes : ISetting<Array<String>, Declaration>

    //language specific properties
    object Namespace : ISetting<String, Declaration>
    val Declaration.namespace: String get() = getSetting(Namespace) ?: defaultNamespace

    object FsPath : ISetting<(CSharp50Generator) -> File, Toplevel>
    val Toplevel.fsPath: File get() = getSetting(FsPath)?.invoke(this@CSharp50Generator) ?: File(folder, "${fileName(this)}.Generated.cs")

    object FlowTransformProperty : ISetting<FlowTransform, Declaration>
    val Member.Reactive.flowTransform: FlowTransform get() = owner.getSetting(FlowTransformProperty) ?: defaultFlowTransform

    object AdditionalUsings : ISetting<(CSharp50Generator) -> List<String>, Toplevel>
    val Toplevel.additionalUsings: List<String> get() = getSetting(CSharp50Generator.AdditionalUsings)?.invoke(this@CSharp50Generator) ?: emptyList()

    object Intrinsic : SettingWithDefault<CSharpIntrinsicMarshaller, Declaration>(CSharpIntrinsicMarshaller.default)
    object PublicCtors: ISetting<Unit, Declaration>
    object Partial : ISetting<Unit, Declaration>
    object DontRegisterAllSerializers: ISetting<Unit, Toplevel>


    protected val IType.isPrimitivesArray : Boolean get() =
    this is IArray && listOf (
        PredefinedType.byte,
        PredefinedType.short,
        PredefinedType.int,
        PredefinedType.long,
        PredefinedType.float,
        PredefinedType.double,
        PredefinedType.char,
        PredefinedType.bool
    ).contains(itemType)


    protected fun Declaration.sanitizedName(scope: Declaration) : String {
        val needQualification =
            namespace != scope.namespace
            || scope.allMembers.map { it.publicName }.contains(name)
        return needQualification.condstr { namespace + "." } + name
    }


    val keywords = arrayOf("abstract", "as",  "base",	"bool",	"break",
    "byte", "case",	"catch",	"char",	"checked",
    "class", "const",	"continue",	"decimal",	"default",
    "delegate", "do",	"double",	"else",	"enum",
    "event",	"explicit",	"extern",	"false",	"finally",
    "fixed",	"float",	"for",	"foreach",
    "goto",	"if",	"implicit",	"in",	"int",
    "interface",	"internal",	"is",	"lock",	"long",
    "namespace",	"new",	"null",	"object",	"operator",
    "out",	"override",	"params",	"private",	"protected",
    "public",	"readonly",	"ref",	"return",	"sbyte",
    "sealed",	"short",	"sizeof",	"stackalloc",
    "static",	"string",	"struct",	"switch",	"this",
    "throw",	"true",	"try",	"typeof",	"uint",
    "ulong",	"unchecked",	"unsafe",	"ushort",	"using",
    "var",	"virtual",	"void",	"volatile",	"while")


    private fun sanitize(name: String, vararg contextVariables: String) : String = keywords.contains(name).condstr { "@" } + contextVariables.contains(name).condstr { "_" } + name

    protected val IType.isValueType : Boolean get() =
        this is Enum
        ||
        listOf (
            PredefinedType.byte,
            PredefinedType.short,
            PredefinedType.int,
            PredefinedType.long,
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


    protected fun Declaration.allTypesForDelegation() : Iterable<IType> {
        fun needDelegate(type : IType, memberIsReactive: Boolean)  =
            type is IArray && !(type.isPrimitivesArray)
        ||  type is IImmutableList
        ||  type is INullable
        ||  type is InternedScalar
        ||  type is Enum && memberIsReactive


        return allMembers.flatMap { when (it) {
            is Member.Field -> listOf(it.type).filter { needDelegate(it, false) }
            is Member.Reactive -> it.genericParams.filter { needDelegate(it, true) }
            else -> emptyList()
        }}.distinct()
    }


    ///types
    protected open fun IType.substitutedName(scope: Declaration) : String {
        return when (this) {
          is Declaration -> sanitizedName(scope)
          is InternedScalar -> itemType.substitutedName(scope)
          is INullable -> itemType.substitutedName(scope) + itemType.isValueType.condstr { "?" }
          is IArray -> itemType.substitutedName(scope) + "[]"
          is IImmutableList -> "List<${itemType.substitutedName(scope)}>"
          is PredefinedType -> {
              if (listOf(
                  PredefinedType.byte,
                  PredefinedType.short,
                  PredefinedType.int,
                  PredefinedType.long,
                  PredefinedType.float,
                  PredefinedType.double,
                  PredefinedType.char,
                  PredefinedType.bool,
                  PredefinedType.string
              ).contains(this)) name.decapitalize()
              else if (this == PredefinedType.void) "Unit"
              else if (this == PredefinedType.secureString) "RdSecureString"
              else name
          }

          else -> fail("Unsupported type ${javaClass.simpleName}")
        }
    }

    //declarations
    protected val Declaration.hasSecondaryCtor : Boolean get () = (this.isConcrete || this is Toplevel) && this.allMembers.any { it.hasEmptyConstructor }

    //members
    val Member.Reactive.actualFlow : FlowKind get() = flowTransform.transform(flow)


    fun Member.needNullCheck() = (this !is Member.Field) || (this.type !is INullable && !this.type.isValueType)

    val notnull = "[NotNull]"
    fun Member.nullAttr(isCtorParam: Boolean = false) =
        if (this !is Member.Field) "$notnull "
        else if (this.type is INullable)
            if (isCtorParam && isOptional) "[Optional] "
            else "[CanBeNull] "
        else if (this.type.isValueType) ""
        else "$notnull "



    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open val Member.Reactive.intfSimpleName : String get () {
        return when (this) {
            is Member.Reactive.Task -> when (actualFlow) {
                Source -> "IRdCall"
                Sink -> "RdEndpoint"
                Both -> "IRdRpc" //todo
            }
            is Member.Reactive.Signal -> when (actualFlow) {
                Sink -> if (freeThreaded) "ISignal" else "ISource"
                Source, Both -> "ISignal"
            }
            is Member.Reactive.Stateful.Property -> when (actualFlow) {
                Sink -> "IReadonlyProperty"
                Source, Both -> "IViewableProperty"
            }
            is Member.Reactive.Stateful.List -> when (actualFlow) {
                Sink -> "IViewableList"
                Source, Both -> "IViewableList"
            }
            is Member.Reactive.Stateful.Set -> when (actualFlow) {
                Sink -> "IViewableSet"
                Source, Both -> "IViewableSet"
            }
            is Member.Reactive.Stateful.Map -> when (actualFlow) {
                Sink -> "IViewableMap"
                Source, Both -> "IViewableMap"
            }

            is Member.Reactive.Stateful.Extension -> implSimpleName

            else -> fail("Unsupported member: $this")
        }
    }

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open val Member.Reactive.implSimpleName : String get () = when (this) {
        is Member.Reactive.Task -> when (actualFlow) {
            Sink -> "RdEndpoint"
            Source -> "RdCall"
            Both -> "RdCall" //todo
        }
        is Member.Reactive.Signal -> "RdSignal"
        is Member.Reactive.Stateful.Property -> "RdProperty"
        is Member.Reactive.Stateful.List -> "RdList"
        is Member.Reactive.Stateful.Set -> "RdSet"
        is Member.Reactive.Stateful.Map -> "RdMap"
        is Member.Reactive.Stateful.Extension -> fqn(this@CSharp50Generator, flowTransform)

        else -> fail ("Unsupported member: $this")
    }


    protected open fun Member.intfSubstitutedName(scope: Declaration): String = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
        is Member.Reactive -> intfSimpleName + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) }
    }

    protected open fun Member.implSubstitutedName(scope: Declaration): String = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
        is Member.Reactive -> implSimpleName + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) }
    }


    protected open val Member.hasEmptyConstructor : Boolean get() = when (this) {
        is Member.Field -> type.hasEmptyConstructor && !emptyCtorSuppressed
        is Member.Reactive -> true

        else -> fail ("Unsupported member: $this")
    }


    protected open val Member.isBindable : Boolean get() = when (this) {
        is Member.Field -> type is IBindable
        is Member.Reactive -> true

        else -> false
    }


    protected open val Member.publicName : String get() = name.capitalize()
    protected open val Member.encapsulatedName : String get() = isEncapsulated.condstr { "_" } + publicName
    protected open val Member.isEncapsulated : Boolean get() = this is Member.Reactive

    protected fun Member.Reactive.customSerializers(containing: Declaration, leadingComma: Boolean) : String {
        val res =  genericParams.joinToString { it.readerDelegateRef(containing) + ", " + it.writerDelegateRef(containing) }
        return (genericParams.isNotEmpty() && leadingComma).condstr { ", " } + res
    }








    //generation

    override fun generate(root: Root, clearFolderIfExists: Boolean, toplevels: List<Toplevel>) {
        prepareGenerationFolder(folder, clearFolderIfExists)

        toplevels.sortedBy { it.name }.forEach { tl ->
            tl.fsPath.bufferedWriter().use { writer ->
                PrettyPrinter().apply {
                    eolKind = Eol.osSpecified
                    step = 2

                    //actual generation
                    file(tl)

                    writer.write(toString())
                }
            }
        }
    }





    protected open fun PrettyPrinter.file(tl : Toplevel) {
        usings(tl)

        println()

        namespace(tl)

        val allTypesWithUnknowns = tl.declaredTypes + unknowns(tl.declaredTypes)

        + "{"
        indent {
            if (tl.isLibrary)
                libdef(tl, allTypesWithUnknowns)
            else
                typedef(tl)

            allTypesWithUnknowns.sortedBy { it.name }.forEach { type ->
                typedef(type)
            }
        }
        + "}"
    }

    protected open fun PrettyPrinter.namespace(decl: Declaration) {
        + "namespace ${decl.namespace}"
    }

    protected open fun PrettyPrinter.usings(tl: Toplevel) {
        + "using System;"
        + "using System.Linq;"
        + "using System.Collections.Generic;"
        + "using System.Runtime.InteropServices;"
        + "using JetBrains.Annotations;"
        println()

        + "using JetBrains.Core;"
        + "using JetBrains.Diagnostics;"
        + "using JetBrains.Collections;"
        + "using JetBrains.Collections.Viewable;"
        + "using JetBrains.Lifetimes;"
        + "using JetBrains.Serialization;"
        + "using JetBrains.Platform.RdFramework;"
        + "using JetBrains.Platform.RdFramework.Base;"
        + "using JetBrains.Platform.RdFramework.Impl;"
        + "using JetBrains.Platform.RdFramework.Tasks;"
        + "using JetBrains.Platform.RdFramework.Util;"
        + "using JetBrains.Platform.RdFramework.Text;"
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

        + "// ReSharper disable RedundantEmptyObjectCreationArgumentList"
        + "// ReSharper disable InconsistentNaming"
        + "// ReSharper disable RedundantOverflowCheckingContext"
        println()
    }


    protected open fun PrettyPrinter.libdef(decl: Toplevel, types: List<Declaration>) {
        if (decl.getSetting(CSharp50Generator.Intrinsic) != null) return
        + "public static class ${decl.name} {"
        indent {
            registerSerializersTrait(decl, types)
        }
        + "}"
    }

    protected open fun PrettyPrinter.typedef(decl: Declaration) {
        if (decl.getSetting(Intrinsic) != null) return

        println()
        println()

        docComment(decl.documentation)

        if (decl is Enum) {
            enum(decl)
            return
        }

        if (decl is Toplevel && !decl.isExtension)
        {
            + (decl.getSetting(ClassAttributes)?.joinToOptString(prefix = "[", postfix = "]") ?: "")
        }

        p("public ")

        if (decl.isAbstract) p("abstract ")
        if (decl.getSetting(Partial) != null) p("partial ")

        p ("class ${decl.name}")

        baseClassTrait(decl)

        + "{"
        indent {
            + "//fields"
            fieldsTrait(decl)
            + "//primary constructor"
            primaryConstructor(decl)
            + "//secondary constructor"
            secondaryConstructorTrait(decl)

            + "//statics"
            staticsTrait(decl)

            +"//custom body"
            customBodyTrait(decl)

            + "//equals trait"
            equalsTrait(decl)
            + "//hash code trait"
            hashCodeTrait(decl)
            + "//pretty print"
            prettyPrintTrait(decl)
            + "//toString"
            toStringTrait(decl)
        }
        + "}"

        if (decl.isExtension) {
            extensionTrait(decl as Ext)
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
            + "protected override long SerializationHash => ${decl.serializationHash(IncrementalHash64()).result}L;"
            println()
            registerSerializersTrait(decl, decl.declaredTypes + unknowns(decl.declaredTypes))
            println()
            createMethodTrait(decl)
        }
    }

    protected fun PrettyPrinter.registerSerializersTrait(decl: Toplevel, declaredAndUnknownTypes: List<Declaration>) {
        if (!decl.isLibrary)
            + "protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;"

        + "public static void RegisterDeclaredTypesSerializers(ISerializers serializers)"
        + "{"
        indent {
            val internedTypes = declaredAndUnknownTypes.flatMap { it.referencedTypes }.filterIsInstance<InternedScalar>().map { it.itemType }


            val allTypesForRegistration = declaredAndUnknownTypes.filter{ it.base != null} +
                    internedTypes.filterIsInstance<Declaration>()

            allTypesForRegistration.filter{!it.isAbstract }.distinct().println {
                if (it is IType)
                    "serializers.Register(${it.readerDelegateRef(decl)}, ${it.writerDelegateRef(decl)});"
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
        + "}"

    }

    //only for non-extensions
    protected fun PrettyPrinter.createMethodTrait(decl: Toplevel) {
        if (decl.isExtension) return

        + "public ${decl.name}(Lifetime lifetime, IProtocol protocol) : this()"
        + "{"


        val protocol = decl.namespace.contains(".Protocol").condstr { "JetBrains.Platform.RdFramework.Impl." } + "Protocol"

        indent {
            + "Identify(protocol.Identities, RdId.Root.Mix(GetType().Name));"
            + "Bind(lifetime, protocol, GetType().Name);"

            + "if ($protocol.InitializationLogger.IsTraceEnabled())"
            indent {
                + "$protocol.InitializationLogger.Trace (\"CREATED toplevel object {0}\", this.PrintToString());"
            }
        }
        + "}"

    }






    fun IType.readerDeclaredElsewhereDelegateRef(containing: Declaration) = when (this) {
        is Enum -> null //to overwrite Declaration
        is PredefinedType -> "JetBrains.Platform.RdFramework.Impl.Serializers.Read$name"
        is Declaration -> this.getSetting(Intrinsic)?.readDelegateFqn ?: "${sanitizedName(containing)}.Read"
        is IArray -> if (this.isPrimitivesArray) "JetBrains.Platform.RdFramework.Impl.Serializers.Read$name" else null
        else -> null
    }                                                           

    fun IType.readerDelegateRef(containing: Declaration) = readerDeclaredElsewhereDelegateRef(containing) ?: when(this) {
        is InternedScalar -> "Read${name}At${internKey.keyName}"
        else -> "Read$name" //must be constructed here
    }
    
    protected fun PrettyPrinter.readerAndDelegatesTrait(decl: Declaration) {

        fun IType.complexDelegateBuilder() : String = readerDeclaredElsewhereDelegateRef(decl) ?: when (this) {
            is Enum -> "new CtxReadDelegate<${sanitizedName(decl)}>(JetBrains.Platform.RdFramework.Impl.Serializers.ReadEnum<${sanitizedName(decl)}>)"
            is IArray -> itemType.complexDelegateBuilder()+".Array()"
            is InternedScalar -> itemType.complexDelegateBuilder()+".Interned(\"${internKey.keyName}\")"
            is IImmutableList -> itemType.complexDelegateBuilder()+".List()"
            is INullable -> itemType.complexDelegateBuilder() +
                ".Nullable" + (if (itemType.isValueType) "Struct" else "Class") + "()"
            else -> fail("Unknown type: $this")
        }


        fun IType.reader() : String  = when (this) {
            is Enum -> "(${sanitizedName(decl)})reader.ReadInt()"
            is PredefinedType -> "reader.Read$name()"
            is InternedScalar -> "ctx.ReadInterned(reader, \"${internKey.keyName}\", ${itemType.complexDelegateBuilder()})"
            else ->  readerDelegateRef(decl) +"(ctx, reader)"
        }

        fun Member.reader() : String  = when (this) {
            is Member.Field -> type.reader()
            is Member.Reactive.Stateful.Extension -> "new ${implSubstitutedName(decl)}(${delegatedBy.reader()})"
            is Member.Reactive -> "${implSubstitutedName(decl)}.Read(ctx, reader${customSerializers(decl, leadingComma = true)})"

            else -> fail("Unknown member: $this")
        }



        val modifiers = "public static" + (decl.base?.let {" new"}?:"")

        if (decl.isAbstract) {
            + "$modifiers CtxReadDelegate<${decl.name}> Read = Polymorphic<${decl.name}>.ReadAbstract(${decl.name}_Unknown.Read);"
            return
        }


        if (decl is Struct || decl is Class || decl is Aggregate) {
            +"$modifiers CtxReadDelegate<${decl.name}> Read = (ctx, reader) => "
            +"{"
            indent {
                if (decl is Class || decl is Aggregate) {
                    + "var _id = RdId.Read(reader);"
                }
                (decl.membersOfBaseClasses + decl.ownMembers).println { "var ${sanitize(it.name, "ctx", "reader")} = ${it.reader()};" }
                p("var _result = new ${decl.name}(${decl.allMembers.joinToString(", ") { sanitize(it.name, "ctx", "reader") }})")
                if (decl is Class || decl is Aggregate) {
                    p(".WithId(_id)")
                }
                +(";")
                if(decl is Class && decl.internRootForScopes.isNotEmpty()) {
                    +"_result.mySerializationContext = ctx.WithInternRootsHere(_result, ${decl.internRootForScopes.joinToString { "\"$it\"" }});"
                }
                +"return _result;"
            }
            +"};"
        }

        decl.allTypesForDelegation().forEach {
            + "public static CtxReadDelegate<${it.substitutedName(decl)}> ${it.readerDelegateRef(decl)} = ${it.complexDelegateBuilder()};"
        }
    }



    fun IType.writerDeclaredElsewhereDelegateRef(containing: Declaration) = when (this) {
        is Enum -> null //to overwrite Declaration
        is PredefinedType -> "JetBrains.Platform.RdFramework.Impl.Serializers.Write$name"
        is Declaration -> this.getSetting(Intrinsic)?.writeDelegateFqn ?: "${sanitizedName(containing)}.Write"
        is IArray -> if (this.isPrimitivesArray) "JetBrains.Platform.RdFramework.Impl.Serializers.Write$name" else null
        else -> null
    }

    fun IType.writerDelegateRef(containing: Declaration) = writerDeclaredElsewhereDelegateRef(containing) ?: when(this) {
        is InternedScalar -> "Write${name}At${internKey.keyName}"
        else -> "Write$name" //must be constructed here
    }

    protected fun PrettyPrinter.writerAndDelegatesTrait(decl: Declaration) {

        fun IType.complexDelegateBuilder() : String = writerDeclaredElsewhereDelegateRef(decl) ?: when (this) {
            is Enum -> "new CtxWriteDelegate<${sanitizedName(decl)}>(JetBrains.Platform.RdFramework.Impl.Serializers.WriteEnum<${sanitizedName(decl)}>)"
            is IArray -> itemType.complexDelegateBuilder()+".Array()"
            is IImmutableList -> itemType.complexDelegateBuilder()+".List()"
            is InternedScalar -> itemType.complexDelegateBuilder()+".Interned(\"${internKey.keyName}\")"
            is INullable -> itemType.complexDelegateBuilder() +
                ".Nullable" + (if (itemType.isValueType) "Struct" else "Class") + "()"
            else -> fail("Unknown type: $this")
        }


        fun IType.writer(field: String) : String  = when (this) {
            is Enum -> "writer.Write((int)$field)"
            is PredefinedType -> "writer.Write($field)"
            is InternedScalar -> "ctx.WriteInterned(writer, $field, \"${internKey.keyName}\", ${itemType.complexDelegateBuilder()})"
            else ->  writerDelegateRef(decl) +"(ctx, writer, $field)"
        }

        fun Member.writer() : String = when (this) {
            is Member.Field -> type.writer("value.$encapsulatedName")
            is Member.Reactive.Stateful.Extension -> delegatedBy.writer(("value.$encapsulatedName.Delegate"))
            is Member.Reactive -> "${implSubstitutedName(decl)}.Write(ctx, writer, value.$encapsulatedName)"

            else -> fail("Unknown member: $this")
        }


        val modifiers = "public static" + (decl.base?.let {" new"}?:"")
        if (decl.isAbstract) {
            + "$modifiers CtxWriteDelegate<${decl.name}> Write = Polymorphic<${decl.name}>.Write;"
            return
        }


        if (decl is Struct || decl is Class || decl is Aggregate) {
            +"$modifiers CtxWriteDelegate<${decl.name}> Write = (ctx, writer, value) => "
            +"{"
            indent {
                if (decl is Class || decl is Aggregate) {
                    + "value.RdId.Write(writer);"
                }
                (decl.membersOfBaseClasses + decl.ownMembers).println { it.writer() + ";" }
                if(decl is Class && decl.internRootForScopes.isNotEmpty()) {
                    + "value.mySerializationContext = ctx.WithInternRootsHere(value, ${decl.internRootForScopes.joinToString { "\"$it\"" }});"
                }
            }
            +"};"
        }

        decl.allTypesForDelegation().forEach {
            + "public static CtxWriteDelegate<${it.substitutedName(decl)}> ${it.writerDelegateRef(decl)} = ${it.complexDelegateBuilder()};"
        }
    }



    protected fun PrettyPrinter.fieldsTrait(decl: Declaration) {

        + "//public fields"
        for (member in decl.ownMembers) {
            p(docComment(member.documentation))
            val prefix = member.nullAttr() + "public"
            when (member) {
                is Member.Reactive ->
                    if (member is Member.Reactive.Signal && member.actualFlow == Source) {
                        val type = member.referencedTypes[0]
                        val isNotVoid = type != PredefinedType.void
                        +"$prefix void ${member.publicName}(${isNotVoid.condstr { type.substitutedName(decl) + " value" }}) => ${member.encapsulatedName}.Fire(${isNotVoid.condstr { "value" }});"
                    }
                    else
                        + "$prefix ${member.intfSubstitutedName(decl)} ${member.publicName} => ${member.encapsulatedName};"
                is Member.Field ->
                    + "$prefix ${member.intfSubstitutedName(decl)} ${member.publicName} {get; private set;}"
                else -> fail("Unsupported member: $member")
            }
        }
        println()

        + "//private fields"
        decl.ownMembers.filterIsInstance<Member.Reactive>().printlnWithBlankLine {
            it.nullAttr() + (if (decl.isAbstract) "protected" else "private") + " readonly ${it.implSubstitutedName(decl)} ${it.encapsulatedName};"
        }

        if (decl is Class && decl.internRootForScopes.isNotEmpty()) {
            + "private SerializationCtx mySerializationContext;"
            + "public override SerializationCtx SerializationContext { get { return mySerializationContext; } }"
        }
    }



    protected fun PrettyPrinter.customBodyTrait(decl: Declaration) {
        if(decl.getSetting(InheritsAutomation) ?: false) {
            +"public event System.ComponentModel.PropertyChangedEventHandler PropertyChanged;"
        }
    }



    protected fun PrettyPrinter.secondaryConstructorTrait(decl: Declaration) {
        if (!decl.hasSecondaryCtor) return


        fun PrettyPrinter.defaultValue(member: Member, typeName: String) {
            if (member is Member.Field)
                member.defaultValue?.let { defaultValue ->
                    p(" = ")
                    when (defaultValue) {
                        is String -> p (
                                if (member.type is Enum) {
                                    if (member.type.flags && defaultValue.isEmpty())
                                        "($typeName)0"
                                    else "$typeName.$defaultValue"
                                }
                                else
                                    "\"$defaultValue\""
                        )
                        else -> p(defaultValue.toString())
                    }
                }
        }

        val accessModifier = when {
            decl.hasSetting(PublicCtors) -> "public"
            decl.isExtension -> "internal"
            decl is Toplevel -> "private"
            else -> "public"
        }


        + "$accessModifier ${decl.name} ("
        indent {
            + decl.allMembers
                .filter { !it.hasEmptyConstructor }
                .joinToString(",\n") {
                    val typeName = it.implSubstitutedName(decl)

                    printer {
                        p(it.nullAttr(true)) // [Null], [NotNull], [Optional]
                        p(typeName)
                        p(" ")
                        p(sanitize(it.name))
                        defaultValue(it, typeName)
                    }.toString()
                }
        }
        + ") : this ("
        indent {
            + decl.allMembers
                .joinToString (",\n") {
                    val defValue = it.defaultValueAsString()
                    if (!it.hasEmptyConstructor) sanitize(it.name)
                    else "new ${it.implSubstitutedName(decl)}(${(it as? Member.Reactive)?.customSerializers(decl, leadingComma = false) ?: ""}$defValue)"
                }
        }
        + ") {}"
    }

    private fun Member.defaultValueAsString(): String {
        return if (this is Member.Reactive.Stateful.Property && defaultValue != null) {
            if (defaultValue is String)
                ", \"$defaultValue\""
            else
                ", $defaultValue"
        } else
            ""
    }


    private fun PrettyPrinter.equalsTrait(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

        fun IScalar.eq(v : String) = when (this) {
            is IArray, is IImmutableList -> "$v.SequenceEqual(other.$v)"
            is Enum, is PredefinedType -> "$v == other.$v"
            else -> "Equals($v, other.$v)"
        }


        + "public override bool Equals(object obj)"
        + "{"
        indent {
            + "if (ReferenceEquals(null, obj)) return false;"
            + "if (ReferenceEquals(this, obj)) return true;"
            + "if (obj.GetType() != GetType()) return false;"
            + "return Equals((${decl.name}) obj);"
        }
        + "}"


        + "public bool Equals(${decl.name} other)"
        + "{"
        indent {
            + "if (ReferenceEquals(null, other)) return false;"
            + "if (ReferenceEquals(this, other)) return true;"
            val res = decl.allMembers.flatMap { m ->
                m as? Member.Field ?: fail("Must be field but was `$m`")
                if (m.usedInEquals)
                    listOf(m)
                else
                    emptyList()

            }.joinToString(" && ") { f ->
                val t = f.type as? IScalar ?: fail("Field $decl.`$f` must have scalar type but was ${f.type}")
                t.eq(f.encapsulatedName)

            }.takeIf { it.isNotBlank() } ?: "true"

            + "return $res;"
        }
        +"}"
    }



    private fun PrettyPrinter.hashCodeTrait(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

        fun IScalar.hc(v : String) : String = when (this) {
            is Enum -> "(int) $v"
            is IArray, is IImmutableList -> "$v.ContentHashCode()"
            is INullable -> "($v != null ? " + (itemType as IScalar).hc(v) + " : 0)"
            else -> "$v.GetHashCode()"
        }


        + "public override int GetHashCode()"
        + "{"
        indent {
            +"unchecked {"
            indent {
                +"var hash = 0;"

                decl.allMembers.println { m ->
                    val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                    val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")
                    if (f.usedInEquals)
                        "hash = hash * 31 + ${t.hc(f.encapsulatedName)};"
                    else
                        ""
                }

                +"return hash;"
            }
            + "}"
        }
        +"}"
    }




    private fun PrettyPrinter.prettyPrintTrait(decl: Declaration) {
        if (!(decl is Toplevel || decl.isConcrete)) return

        val optOverride = (decl !is Struct).condstr { "override " }
        + "public ${optOverride}void Print(PrettyPrinter printer)"
        + "{"
        indent {
            + "printer.Println(\"${decl.name} (\");"
            decl.allMembers.printlnWithPrefixSuffixAndIndent("using (printer.IndentCookie()) {", "}") { "printer.Print(\"${it.name} = \"); ${it.encapsulatedName}.PrintEx(printer); printer.Println();"}
            + "printer.Print(\")\");"
        }
        + "}"
    }

    private fun PrettyPrinter.toStringTrait(decl: Declaration) {
        if (!(decl is Toplevel || decl.isConcrete)) return
        
        + "public override string ToString()"
        + "{"
        indent {
            + "var printer = new SingleLinePrettyPrinter();"
            + "Print(printer);"
            + "return printer.ToString();"
        }
        +"}"
    }


    private fun PrettyPrinter.primaryConstructor(decl: Declaration) {
        if (decl !is Toplevel && decl.allMembers.isEmpty()) return //no constructors

        val accessModifier = when {
            decl.hasSetting(PublicCtors) -> "public"
            decl.isAbstract -> "protected"
            decl.hasSecondaryCtor -> "private"
            decl.isExtension -> "internal"
            decl is Toplevel -> "private"
            else -> "public"
        }

        + "$accessModifier ${decl.name}("
        indent {
            + decl.allMembers.joinToString(",\r\n") { "${it.nullAttr(true)}${it.implSubstitutedName(decl)} ${sanitize(it.name)}" }
        }
        p(")")
        val base = decl.base
        if (base != null && !base.allMembers.isEmpty()) {
            + " : base ("
            indent {
                + base.allMembers.joinToString(",\r\n") { sanitize(it.name) }
            }
            p(" ) ")
        }
        println()

        + "{"
        indent {
            decl.ownMembers.filter { it.needNullCheck()  }.printlnWithBlankLine { "if (${sanitize(it.name)} == null) throw new ArgumentNullException(\"${it.name}\");" }

            decl.ownMembers.println { "${it.encapsulatedName} = ${sanitize(it.name)};" }

            decl.ownMembers
                .filterIsInstance<Member.Reactive.Stateful>()
                .filter { it !is Member.Reactive.Stateful.Extension && it.genericParams.none { it is IBindable }}
                .println { "${it.encapsulatedName}.OptimizeNested = true;" }

            decl.ownMembers
                .filterIsInstance<Member.Reactive>()
                .filter {it.freeThreaded}.println {"${it.encapsulatedName}.Async = true;"}

            decl.ownMembers
                .filterIsInstance<Member.Reactive>()
                .filter {it.genericParams.any {it is INullable}}.println {"${it.encapsulatedName}.ValueCanBeNull = true;"}

            decl.ownMembers
                .filter { it.isBindable }
                .println { """BindableChildren.Add(new KeyValuePair<string, object>("${it.name}", ${it.encapsulatedName}));""" }
        }
        + "}"
    }


    protected open fun PrettyPrinter.baseClassTrait(decl: Declaration) {

        val base = decl.base
        val baseClassesStr =
                if (base == null) {
                    when (decl) {
                        is Toplevel -> "RdExtBase"
                        is BindableDeclaration -> "RdBindableBase"
                        is Struct.Concrete -> "IPrintable, IEquatable<${decl.name}>"
                        else -> "" //abstract struct doesn't implement these methods, enum must not reach this place
                    }
                } else {
                    base.sanitizedName(decl)
                }

        var res = baseClassesStr +
                (decl.getSetting(Inherits)?.let { ", $it" } ?: "") +
                (if(decl.getSetting(InheritsAutomation) == true) ", JetBrains.Application.UI.UIAutomation.IAutomation" else "") //todo remove

        if (res.startsWith(','))
            res = res.substring(1)

        if (!res.isBlank()) {
            + " : $res"
        }
    }


    protected open fun PrettyPrinter.enum(decl: Enum) {
        if (decl.flags)
            + "[Flags]"
        + "public enum ${decl.name} {"
        indent {
            + decl.constants.withIndex().joinToString(separator = ",\r\n") { (idx, enumConst) ->
                docComment(enumConst.documentation) + sanitize(enumConst.name) + decl.flags.condstr { " = 1 << $idx" }
            }
        }
        + "}"
    }

    private fun PrettyPrinter.extensionTrait(decl: Ext) {
        val pointcut = decl.pointcut ?: return
        val ownerLowerName = pointcut.name.decapitalize()

        + "public static class ${pointcut.name}${decl.name}Ex"
        + " {"
        indent {
            val lowerName = decl.name.decapitalize()
            val extName = decl.extName?.capitalize() ?: decl.name
            + "public static ${decl.name} Get$extName(this ${pointcut.sanitizedName(decl)} $ownerLowerName)"
            + "{"
            indent {
                + """return $ownerLowerName.GetOrCreateExtension("$lowerName", () => new ${decl.name}());"""
            }
            + "}"
        }
        + "}"
    }

    override fun toString(): String {
        return "CSharp50Generator(defaultFlowTransform=$defaultFlowTransform, defaultNamespace='$defaultNamespace', folder=${folder.canonicalPath})"
    }


}
