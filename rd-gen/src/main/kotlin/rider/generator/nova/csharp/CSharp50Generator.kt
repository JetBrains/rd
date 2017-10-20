package com.jetbrains.rider.generator.nova.csharp

import com.jetbrains.rider.generator.nova.*
import com.jetbrains.rider.generator.nova.Enum
import com.jetbrains.rider.generator.nova.FlowKind.*
import com.jetbrains.rider.util.condstr
import com.jetbrains.rider.util.joinToOptString
import com.jetbrains.rider.util.string.Eol
import com.jetbrains.rider.util.string.PrettyPrinter
import java.io.File

open class CSharp50Generator(val defaultFlowTransform: FlowTransform, val defaultNamespace: String, override val folder : File, val extAttr : String = "") : GeneratorBase() {


    object Inherits : IGeneratorProperty<String>
    object InheritsAutomation : IGeneratorProperty<Boolean>

    //language specific properties
    object Namespace : IGeneratorProperty<String>
    val Declaration.namespace: String get() = getSetting(Namespace) ?: defaultNamespace

    object FsPath : IGeneratorProperty<(CSharp50Generator) -> File>
    val Toplevel.fsPath: File get() = getSetting(FsPath)?.invoke(this@CSharp50Generator) ?: File(folder, "$name.Generated.cs")

    object FlowTransformProperty : IGeneratorProperty<FlowTransform>
    val Member.Reactive.flowTransform: FlowTransform get() = owner.getSetting(FlowTransformProperty) ?: defaultFlowTransform

    object Intrinsic : IGeneratorProperty<SerializationDelegates>
    object Partial : IGeneratorProperty<Unit>


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
            PredefinedType.rdId
            //"string" and "uri" are reference types

        ).contains(this)


    protected fun Declaration.allTypesForDelegation() : Iterable<IType> {
        fun needDelegate(type : IType, memberIsReactive: Boolean)  =
            type is IArray && !(type.isPrimitivesArray)
        ||  type is IImmutableList
        ||  type is INullable
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
              else if (this == PredefinedType.void) "RdVoid"
              else name
          }

          else -> fail("Unsupported type ${javaClass.simpleName}")
        }
    }

    protected open val IType.hasEmptyConstructor : Boolean get() = when (this) {
        is Class.Concrete -> allMembers.all { it.hasEmptyConstructor }

        else -> false
    }

    //declarations
    protected val Declaration.hasSecondaryCtor : Boolean get () = (this is Class.Concrete || this is Struct.Concrete) && this.allMembers.any { it.hasEmptyConstructor }

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



    protected open val Member.Reactive.intfSimpleName : String get () {
        val async = this.freeThreaded.condstr { "Async" }
        return when (this) {
            is Member.Reactive.Task -> when (actualFlow) {
                Source -> "IRdCall"
                Sink -> "RdEndpoint"
                Both -> "IRdRpc" //todo
            }
            is Member.Reactive.Signal -> when (actualFlow) {
                Source -> "ISource"
                Sink -> "I${async}Sink"
                Both -> "I${async}RdSignal"
            }
            is Member.Reactive.Stateful.Property -> when (actualFlow) {
                Sink -> "IRdProperty"
                Source, Both -> "IRdProperty"
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

            is Member.Reactive.Stateful.Text -> "RdTextBuffer"

            else -> fail("Unsupported member: $this")
        }
    }

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
        is Member.Reactive.Stateful.Text -> "RdTextBuffer"

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

    override fun generate(root: Root, clearFolderIfExists: Boolean) {
        prepareGenerationFolder(folder, clearFolderIfExists)

        val toplevels : MutableList<Toplevel> = root.singletons.toMutableList()
        /*if (root.ownMembers.isNotEmpty()) */toplevels.add(root)

        toplevels.sortedBy { it.name }.forEach { tl ->
            tl.fsPath.bufferedWriter().use { writer ->
                PrettyPrinter().apply {
                    eol = Eol.linux
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

        + "{"
        indent {
            if (tl.isLibrary)
                libdef(tl)
            else
                typedef(tl)

            tl.declaredTypes.sortedBy { it.name }.forEach { type ->
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
        + "using JetBrains.Application;"
        + "using JetBrains.Annotations;"
        println()

        + "using JetBrains.Platform.RdFramework;"
        + "using JetBrains.Platform.RdFramework.Base;"
        + "using JetBrains.Platform.RdFramework.Impl;"
        + "using JetBrains.Platform.RdFramework.Tasks;"
        + "using JetBrains.Platform.RdFramework.Util;"
        + "using JetBrains.Platform.RdFramework.Text;"
        println()

        + "using JetBrains.Util;"
        + "using JetBrains.Util.PersistentMap;"
        + "using JetBrains.Util.Special;"
        + "using Lifetime = JetBrains.DataFlow.Lifetime;"
        println()

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


    protected open fun PrettyPrinter.libdef(decl: Toplevel) {
        if (decl.getSetting(CSharp50Generator.Intrinsic) != null) return
        + "public static class ${decl.name} {"
        indent {
            registerSerializersTrait(decl)
        }
        + "}"
    }

    protected open fun PrettyPrinter.typedef(decl: Declaration) {
        if (decl.getSetting(Intrinsic) != null) return

        println()
        println()

        if (decl is Enum) {
            enum(decl)
            return
        }

        if (decl is Toplevel)
        {
            + extAttr //[ShellComponent]
        }
        p("public ")
        if (decl.isAbstract) p("abstract ")


        if (decl.getSetting(Partial) != null) p("partial ")

        p ("class ${decl.name}")

        baseClassTrait(decl)

        + " {"
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

            + "//init method"
            initMethodTrait(decl)
            + "//identify method"
            identifyMethodTrait(decl)
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
    }



    protected fun PrettyPrinter.staticsTrait(decl: Declaration) {

        println()
        readerAndDelegatesTrait(decl)

        println()
        writerAndDelegatesTrait(decl)


        if (decl is Toplevel) {

            println()
            registerSerializersTrait(decl)
            println()
            createMethodTrait(decl)
        }
    }

    protected fun PrettyPrinter.registerSerializersTrait(decl: Toplevel) {
        + "public static void Register(ISerializers serializers)"
        + "{"
        indent {
            + "if (!serializers.Toplevels.Add(typeof(${decl.name}))) return;"
            + "Protocol.InitializationLogger.Trace(\"REGISTER serializers for {0}\", typeof(${decl.name}).Name);"
            println()

            decl.declaredTypes.filter{ !it.isAbstract }.println {
                if (it is Enum) "serializers.RegisterEnum<${it.sanitizedName(decl)}>();"
                else "serializers.Register(${it.sanitizedName(decl)}.Read, ${it.sanitizedName(decl)}.Write);"
            }

            if (decl is Root) {
                decl.singletons.println { it.sanitizedName(decl) + ".Register(serializers);" }
            }
        }
        + "}"
    }

    //todo rewrite
    var staticId = 1000
    protected fun PrettyPrinter.createMethodTrait(decl: Toplevel) {
        if (decl is Class && decl.isInternRoot)
            fail("Top-level entites can not (and should not) be interning roots")

        p("public ${decl.name}(Lifetime lifetime, IProtocol protocol)")
        decl.ownMembers.filterIsInstance<Member.Reactive>()
            .joinWithPrefixSuffixAndIndent(",\n", " : this (", ")") {
                "new ${it.implSubstitutedName(decl)}(${it.customSerializers(decl, leadingComma = false)}).Static(${ ++ staticId})"
            }
        + "{"

        indent {
            + (decl.root.sanitizedName(decl) + ".Register(protocol.Serializers);")
            + "Register(protocol.Serializers);"
            + "Bind(lifetime, protocol, GetType().Name);"
            + "if (Protocol.InitializationLogger.IsTraceEnabled())"
            indent {
                + "Protocol.InitializationLogger.Trace (\"CREATED toplevel object {0}\", this.PrintToString());"
            }
        }
        + "}"

    }






    fun IType.readerDeclaredElsewhereDelegateRef(containing: Declaration) = when (this) {
        is Enum -> null //to overwrite Declaration
        is PredefinedType -> "Serializers.Read$name"
        is Declaration -> getSetting(Intrinsic)?.read ?: "${sanitizedName(containing)}.Read"
        is IArray -> if (this.isPrimitivesArray) "Serializers.Read$name" else null
        else -> null
    }                                                           

    fun IType.readerDelegateRef(containing: Declaration) = readerDeclaredElsewhereDelegateRef(containing) ?: "Read$name" //must be constructed here
    
    protected fun PrettyPrinter.readerAndDelegatesTrait(decl: Declaration) {

        fun IType.complexDelegateBuilder() : String = readerDeclaredElsewhereDelegateRef(decl) ?: when (this) {
            is Enum -> "new CtxReadDelegate<${sanitizedName(decl)}>(Serializers.ReadEnum<${sanitizedName(decl)}>)"
            is IArray -> itemType.complexDelegateBuilder()+".Array()"
            is InternedScalar -> itemType.complexDelegateBuilder()+".Interned()"
            is IImmutableList -> itemType.complexDelegateBuilder()+".List()"
            is INullable -> itemType.complexDelegateBuilder() +
                ".Nullable" + (if (itemType.isValueType) "Struct" else "Class") + "()"
            else -> fail("Unknown type: $this")
        }


        fun IType.reader() : String  = when (this) {
            is Enum -> "(${sanitizedName(decl)})reader.ReadInt()"
            is PredefinedType -> "reader.Read$name()"
            is InternedScalar -> "ctx.ReadInterned(reader, ${itemType.complexDelegateBuilder()})"
            else ->  readerDelegateRef(decl) +"(ctx, reader)"
        }

        fun Member.reader() : String  = when (this) {
            is Member.Field -> type.reader()
            is Member.Reactive -> "${implSubstitutedName(decl)}.Read(ctx, reader${customSerializers(decl, leadingComma = true)})"

            else -> fail("Unknown member: $this")
        }



        val modifiers = "public static" + (decl.base?.let {" new"}?:"")

        if (decl.isAbstract) {
            + "$modifiers CtxReadDelegate<${decl.name}> Read = Polymorphic<${decl.name}>.Read;"
            return
        }


        if (decl is Struct || decl is Class) {
            +"$modifiers CtxReadDelegate<${decl.name}> Read = (ctx, reader) => "
            +"{"
            indent {
                if(decl is Class && decl.isInternRoot) {
                    + "ctx = ctx.WithInternRootHere(false);"
                }

                decl.allMembers.println { "var ${sanitize(it.name, "ctx", "reader")} = ${it.reader()};" }
                +"return new ${decl.name}(${decl.allMembers.joinToString(", ") { sanitize(it.name, "ctx", "reader") }})${(decl is Class && decl.isInternRoot).condstr { " { mySerializationContext = ctx }" }};"
            }
            +"};"
        }

        decl.allTypesForDelegation().forEach {
            + "public static CtxReadDelegate<${it.substitutedName(decl)}> Read${it.name} = ${it.complexDelegateBuilder()};"
        }
    }



    fun IType.writerDeclaredElsewhereDelegateRef(containing: Declaration) = when (this) {
        is Enum -> null //to overwrite Declaration
        is PredefinedType -> "Serializers.Write$name"
        is Declaration -> getSetting(Intrinsic)?.write ?: "${sanitizedName(containing)}.Write"
        is IArray -> if (this.isPrimitivesArray) "Serializers.Write$name" else null
        else -> null
    }

    fun IType.writerDelegateRef(containing: Declaration) = writerDeclaredElsewhereDelegateRef(containing) ?: "Write$name" //must be constructed here

    protected fun PrettyPrinter.writerAndDelegatesTrait(decl: Declaration) {

        fun IType.complexDelegateBuilder() : String = writerDeclaredElsewhereDelegateRef(decl) ?: when (this) {
            is Enum -> "new CtxWriteDelegate<${sanitizedName(decl)}>(Serializers.WriteEnum<${sanitizedName(decl)}>)"
            is IArray -> itemType.complexDelegateBuilder()+".Array()"
            is IImmutableList -> itemType.complexDelegateBuilder()+".List()"
            is InternedScalar -> itemType.complexDelegateBuilder()+".Interned()"
            is INullable -> itemType.complexDelegateBuilder() +
                ".Nullable" + (if (itemType.isValueType) "Struct" else "Class") + "()"
            else -> fail("Unknown type: $this")
        }


        fun IType.writer(field: String) : String  = when (this) {
            is Enum -> "writer.Write((int)$field)"
            is PredefinedType -> "writer.Write($field)"
            is InternedScalar -> "ctx.WriteInterned(writer, $field, ${itemType.complexDelegateBuilder()})"
            else ->  writerDelegateRef(decl) +"(ctx, writer, $field)"
        }

        fun Member.writer() : String = when (this) {
            is Member.Field -> type.writer("value.$encapsulatedName")
            is Member.Reactive -> "${implSubstitutedName(decl)}.Write(ctx, writer, value.$encapsulatedName)"

            else -> fail("Unknown member: $this")
        }


        val modifiers = "public static" + (decl.base?.let {" new"}?:"")
        if (decl.isAbstract) {
            + "$modifiers CtxWriteDelegate<${decl.name}> Write = Polymorphic<${decl.name}>.Write;"
            return
        }


        if (decl is Struct || decl is Class) {
            +"$modifiers CtxWriteDelegate<${decl.name}> Write = (ctx, writer, value) => "
            +"{"
            indent {
                if(decl is Class && decl.isInternRoot) {
                    + "ctx = ctx.WithInternRootHere(true);"
                    + "value.mySerializationContext = ctx;"
                }
                decl.allMembers.println { it.writer() + ";" }
            }
            +"};"
        }

        decl.allTypesForDelegation().forEach {
            + "public static CtxWriteDelegate<${it.substitutedName(decl)}> Write${it.name} = ${it.complexDelegateBuilder()};"
        }
    }



    protected fun PrettyPrinter.fieldsTrait(decl: Declaration) {

        + "//public fields"
        decl.ownMembers
            .printlnWithBlankLine { when (it) {
                is Member.Reactive ->
                    it.nullAttr() + "public ${it.intfSubstitutedName(decl)} ${it.publicName} { get { return ${it.encapsulatedName}; }}"
                is Member.Field ->
                    it.nullAttr() + "public ${it.intfSubstitutedName(decl)} ${it.publicName} {get; private set;}"
                else -> fail("Unsupported member: $it")
            }}

        + "//private fields"
        decl.ownMembers.filterIsInstance<Member.Reactive>().printlnWithBlankLine {
            it.nullAttr() + (if (decl.isAbstract) "protected" else "private") + " readonly ${it.implSubstitutedName(decl)} ${it.encapsulatedName};"
        }

        if (decl is Class && decl.isInternRoot) {
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

        + "public ${decl.name} ("
        indent {
            + decl.allMembers
                .filter { !it.hasEmptyConstructor }
                .joinToString(",\n") { "${it.nullAttr(true)}${it.implSubstitutedName(decl)} ${sanitize(it.name)}" }
        }
        + ") : this ("
        indent {
            + decl.allMembers
                .joinToString (",\n") {
                    if (!it.hasEmptyConstructor) sanitize(it.name)
                    else "new ${it.implSubstitutedName(decl)}(${(it as? Member.Reactive)?.customSerializers(decl, leadingComma = false) ?: ""})"
                }
        }
        + ") {}"
    }


    protected fun PrettyPrinter.initMethodTrait(decl: Declaration) {
        if (!(decl is Class.Concrete || decl is Toplevel)) return

        + "protected override void Init(Lifetime lifetime) {"
        indent {
            decl.allMembers.filter { it.isBindable } .println { "${it.encapsulatedName}.BindEx(lifetime, this, \"${it.name}\");" }
        }
        + "}"

    }

    protected fun PrettyPrinter.identifyMethodTrait(decl: Declaration) {
        if (!(decl is Class.Concrete || decl is Toplevel)) return

        + "public override void Identify(IIdentities ids) {"
        indent {
            decl.allMembers.filter { it.isBindable } .println { "${it.encapsulatedName}.IdentifyEx(ids);" }
        }
        + "}"
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
            val res =
                if (decl.allMembers.isEmpty()) "true"
                else decl.allMembers.joinToString(" && ") { m ->
                    val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                    val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")
                    t.eq(f.encapsulatedName)
                }
            + "return $res;"
        }
        +"}"
    }



    private fun PrettyPrinter.hashCodeTrait(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

        fun IScalar.hc(v : String) : String = when (this) {
            is Enum -> "(int) $v"
            is IArray, is IImmutableList -> "CollectionUtil.GetHashCode($v)"
            is INullable -> "($v != null ?" + (itemType as IScalar).hc(v) + " : 0)"
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
                    "hash = hash * 31 + ${t.hc(f.encapsulatedName)};"
                }

                +"return hash;"
            }
            + "}"
        }
        +"}"
    }




    private fun PrettyPrinter.prettyPrintTrait(decl: Declaration) {
        if (!(decl is Toplevel || decl is Class.Concrete || decl is Struct.Concrete)) return

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
        if (!(decl is Toplevel || decl is Class.Concrete || decl is Struct.Concrete)) return
        
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
        if (decl.allMembers.isEmpty()) return

        val accessModifier =
            if (decl.isAbstract) "protected"
            else if (decl.hasSecondaryCtor) "private"
            else "public"

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
                .filter {it !is Member.Reactive.Stateful.Text && it.genericParams.none { it is IBindable }}
                .println { "${it.encapsulatedName}.OptimizeNested = true;" }

            decl.ownMembers
                .filterIsInstance<Member.Reactive>()
                .filter {it.freeThreaded}.println {"${it.encapsulatedName}.Async = true;"}
        }
        + "}"
    }


    protected open fun PrettyPrinter.baseClassTrait(decl: Declaration) {
        val base = decl.base ?: let {
            val inherits = decl.getSetting(Inherits)?.let { ", $it" } ?: ""
            val inheritsAutomation = if(decl.getSetting(InheritsAutomation) ?: false) ", JetBrains.Application.UI.UIAutomation.IAutomation" else ""

            if (decl is Class || decl is Toplevel) p(" : RdBindableBase$inherits$inheritsAutomation")
            if (decl is Struct.Concrete) p(" : IPrintable, IEquatable<${decl.name}>$inherits$inheritsAutomation")
            return
        }

        + " : ${base.name}"
    }


    protected open fun PrettyPrinter.enum(decl: Enum) {
        + "public enum ${decl.name} {"
        indent {
            + decl.constants.joinToString(separator = ",\r\n") { sanitize(it.name) }
        }
        + "}"
    }

    override fun toString(): String {
        return "CSharp50Generator(defaultFlowTransform=$defaultFlowTransform, defaultNamespace='$defaultNamespace', folder=$folder, extAttr=$extAttr)"
    }


}
