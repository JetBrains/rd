package com.jetbrains.rider.generator.nova.kotlin

import com.jetbrains.rider.generator.nova.*
import com.jetbrains.rider.generator.nova.Enum
import com.jetbrains.rider.generator.nova.FlowKind.*
import com.jetbrains.rider.util.condstr
import com.jetbrains.rider.util.joinToOptString
import com.jetbrains.rider.util.optSpace
import com.jetbrains.rider.util.string.Eol
import com.jetbrains.rider.util.string.PrettyPrinter
import java.io.File

open class Kotlin11Generator(val flowTransform: FlowTransform, val defaultNamespace: String, override val folder : File) : GeneratorBase() {

    //language specific properties
    object Namespace : IGeneratorProperty<String>
    val Declaration.namespace: String get() = getSetting(Namespace) ?: defaultNamespace

    object Intrinsic : IGeneratorProperty<Unit>

    ///toplevel
    protected open val Toplevel.fsPath: File get() = File(folder, "$name.Generated.kt")

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

    ///types
    protected open fun IType.substitutedName(scope: Declaration) : String = when (this) {
        is Declaration -> (namespace != scope.namespace).condstr { namespace + "." } + name
        is INullable    -> "${itemType.substitutedName(scope)}?"
        is InternedScalar -> itemType.substitutedName(scope)
        is IArray       ->
            if (isPrimitivesArray) itemType.substitutedName(scope) + "Array"
            else "Array<${itemType.substitutedName(scope)}>"
        is IImmutableList -> "List<${itemType.substitutedName(scope)}>"

        is PredefinedType.bool -> "Boolean"
        is PredefinedType.dateTime -> "Date"
        is PredefinedType.guid -> "UUID"
        is PredefinedType.uri -> "URI"
        is PredefinedType.void -> "Unit"
        is PredefinedType -> name.capitalize()

        else -> fail("Unsupported type ${javaClass.simpleName}")
    }

    protected open val IType.hasEmptyConstructor : Boolean get() = when (this) {
        is Class.Concrete -> allMembers.all { it.hasEmptyConstructor }

        else -> false
    }


    //members
    val Member.Reactive.actualFlow : FlowKind get() = flowTransform.transform(flow)

    protected open val Member.Reactive.intfSimpleName : String get () {
        val async = this.freeThreaded.condstr { "Async" }
        return when (this) {
            is Member.Reactive.Task -> when (actualFlow) {
                Sink -> "RdEndpoint"
                Source -> "IRdCall"
                Both -> "IRdRpc" //todo
            }
            is Member.Reactive.Signal -> when (actualFlow) {
                Sink -> "I${async}Sink"
                Source -> "ISource"
                Both -> "I${async}Signal"
            }
            is Member.Reactive.Stateful.Property -> when (actualFlow) {
                Sink -> "IReadonlyProperty"
                Source, Both -> "IProperty"
            }
            is Member.Reactive.Stateful.List -> when (actualFlow) {
                Sink -> "IViewableList"
                Source, Both -> "IMutableViewableList"
            }
            is Member.Reactive.Stateful.Set -> when (actualFlow) {
                Sink -> "IViewableSet"
                Source, Both -> "IMutableViewableSet"
            }
            is Member.Reactive.Stateful.Map -> when (actualFlow) {
                Sink -> "I${async}ViewableMap"
                Source, Both -> "IMutableViewableMap"
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


    protected open fun Member.intfSubstitutedName(scope: Declaration) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
        is Member.Reactive -> intfSimpleName + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) }
    }

    protected open fun Member.implSubstitutedName(scope: Declaration) = when (this) {
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


    protected open val Member.publicName : String get() = name
    protected open val Member.encapsulatedName : String get() = isEncapsulated.condstr { "_" } + publicName
    protected open val Member.isEncapsulated : Boolean get() = this is Member.Reactive

    protected fun Member.ctorParam(containing: Declaration) : String {
        return "$encapsulatedName : ${implSubstitutedName(containing)}" + (this is Member.Field && isOptional).condstr { " = null" }
    }


    protected fun Member.Reactive.customSerializers(scope: Declaration, leadingComma: Boolean) : String {
        val res =  genericParams.joinToString { it.serializerRef(scope) }
        return (genericParams.isNotEmpty() && leadingComma).condstr { ", " } + res
    }

    protected fun Declaration.sanitizedName(scope: Declaration) : String {
        val needQualification = namespace != scope.namespace
        return needQualification.condstr { namespace + "." } + name
    }

    protected fun IType.leafSerializerRef(scope: Declaration) : String? = when (this) {
        is Enum -> "${sanitizedName(scope)}.marshaller"
        is PredefinedType -> "FrameworkMarshallers.$name"
        is Declaration -> if (isAbstract) "Polymorphic<${sanitizedName(scope)}>()" else sanitizedName(scope)
        is IArray -> if (this.isPrimitivesArray) "FrameworkMarshallers.$name" else null
        else -> null
    }

    protected fun IType.serializerRef(scope: Declaration) : String = leafSerializerRef(scope) ?: "__${name}Serializer"




    //generation
    override fun generate(root: Root, clearFolderIfExists: Boolean) {
        prepareGenerationFolder(folder, clearFolderIfExists)

        val toplevels : MutableList<Toplevel> = root.singletons.toMutableList()
        /*if (root.ownMembers.isNotEmpty())*/ toplevels.add(root)

        toplevels.sortedBy { it.name }.forEach { tl ->
            tl.fsPath.bufferedWriter().use { writer ->
                PrettyPrinter().apply {
                    eol = Eol.linux
                    step = 4

                    //actual generation
                    file(tl)

                    writer.write(toString())
                }
            }
        }
    }





    protected open fun PrettyPrinter.file(tl : Toplevel) {
        namespace(tl)

        println()
        imports(tl)

        println()

        if (tl.isLibrary)
            libdef(tl)
        else
            typedef(tl)

        tl.declaredTypes.sortedBy { it.name }.forEach { type ->
            typedef(type)
        }
    }

    protected open fun PrettyPrinter.namespace(decl: Declaration) {
        + """@file:Suppress("PackageDirectoryMismatch", "UnusedImport", "unused")"""
        + "package ${decl.namespace}"
    }

    protected open fun PrettyPrinter.imports(tl: Toplevel) {
        + "import com.jetbrains.rider.framework.*"
        + "import com.jetbrains.rider.framework.base.*"
        + "import com.jetbrains.rider.framework.impl.*"

        println()
        + "import com.jetbrains.rider.util.lifetime.*"
        + "import com.jetbrains.rider.util.reactive.*"
        + "import com.jetbrains.rider.util.string.*"
        + "import com.jetbrains.rider.util.trace"

//        tl.referencedTypes.plus(tl.declaredTypes.flatMap { it.referencedTypes })
//            .filterIsInstance(Declaration::class.java)
//            .map {
//                it.namespace
//            }
//            .filterNot { it == tl.namespace }
//            .distinct()
//            .printlnWithBlankLine { "import $it.*;" }

        println()
        + "import java.io.*"
        + "import java.util.*"
        + "import java.net.*"
    }


    protected open fun PrettyPrinter.libdef(decl: Toplevel) {
        if (decl.getSetting(Kotlin11Generator.Intrinsic) != null) return
        + "object ${decl.name} {"
        indent {
            registerSerializersTrait(decl)
        }
        + "}"
    }

    protected open fun PrettyPrinter.typedef(decl: Declaration) {
        if (decl.getSetting(Kotlin11Generator.Intrinsic) != null) return

        println()
        println()

        if (decl is Enum) {
            enum(decl)
            return
        }

        if (decl.isAbstract) p("abstract ")
        if (decl is Struct.Concrete && decl.base == null) p("data ")


        + "class ${decl.name} ("
        indent {
            primaryCtorParamsTrait(decl)
        }
        p(")")

        baseClassTrait(decl)

        + " {"
        indent {
            + "//companion"
            companionTrait(decl)
            + "//fields"
            fieldsTrait(decl)
            + "//initializer"
            initializerTrait(decl)
            + "//secondary constructor"
            secondaryConstructorTrait(decl)
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
        }
        + "}"
    }



    protected fun  PrettyPrinter.companionTrait(decl: Declaration) {
        if (decl is Class.Concrete || decl is Struct.Concrete) {
            println()
            + "companion object : IMarshaller<${decl.name}> {"
            indent {
                + "override val _type: Class<${decl.name}> = ${decl.name}::class.java"
                println()
                readerTrait(decl)
                println()
                writerTrait(decl)
                println()
                customSerializersTrait(decl)
            }
            + "}"
        } //todo root, singleton

        if (decl is Toplevel) {
            println()
            + "companion object {"
            indent {
                println()
                registerSerializersTrait(decl)
                println()
                createMethodTrait(decl)
                println()
                customSerializersTrait(decl)
            }
            + "}"
        }
    }

    protected fun PrettyPrinter.customSerializersTrait(decl: Declaration) {
        fun IType.serializerBuilder() : String = leafSerializerRef(decl)?: when (this) {
            is IArray -> itemType.serializerBuilder() + ".array()"
            is IImmutableList -> itemType.serializerBuilder() + ".list()"
            is INullable -> itemType.serializerBuilder() + ".nullable()"
            is InternedScalar -> itemType.serializerBuilder() + ".interned()"
            else -> fail("Unknown type: $this")
        }

        val allTypesForDelegation = decl.allMembers
            .filterIsInstance<Member.Reactive>()
            .flatMap { it.genericParams.toList() }
            .distinct()
            .filter { it.leafSerializerRef(decl) == null }

        allTypesForDelegation.println { "private val ${it.serializerRef(decl)} = ${it.serializerBuilder()}" }
    }

    protected fun  PrettyPrinter.registerSerializersTrait(decl: Toplevel) {
        + "public fun register(serializers : ISerializers) {"
        indent {
            + "if (!serializers.toplevels.add(${decl.name}::class.java)) return"
            + "Protocol.initializationLogger.trace { \"REGISTER serializers for \"+${decl.name}::class.java.simpleName }"
            decl.declaredTypes.filter{ !it.isAbstract }.filterIsInstance<IType>().println {
                "serializers.register(${it.serializerRef(decl)})"
            }

            if (decl is Root) {
                decl.singletons.println { it.sanitizedName(decl) + ".register(serializers)" }
            }
        }
        + "}"
    }

    //todo rewrite
    var staticId = 1000
    protected fun PrettyPrinter.createMethodTrait(decl: Toplevel) {
        if (decl is Class && decl.isInternRoot)
            fail("Top-level objects can not (and should not) be interning roots")

        + "fun create(lifetime : Lifetime, protocol : IProtocol) : ${decl.name} {"
        indent {
            + "${decl.root.sanitizedName(decl)}.register(protocol.serializers)"
            + "register(protocol.serializers)"
            println()

            + "val __res = ${decl.name} ("
            indent {
                p(decl.ownMembers.filterIsInstance<Member.Reactive>().joinToString(",\n") {
                    "${it.implSubstitutedName(decl)}(${it.customSerializers(decl, leadingComma = false)}).static(${ ++ staticId})"
                })
            }
            + ")"
            + "__res.bind(lifetime, protocol, ${decl.name}::class.java.simpleName)"
            println()
            + "Protocol.initializationLogger.trace { \"CREATED toplevel object \"+__res.printToString() }"
            println()
            + "return __res"
        }
        + "}"

    }





    protected fun PrettyPrinter.readerTrait(decl: Declaration) {


        fun IType.reader() : String  = when (this) {
            is Enum -> "stream.readEnum<${substitutedName(decl)}>()"
            is InternedScalar -> "ctx.readInterned(stream, { ctx, stream -> ${itemType.reader()} })"
            is PredefinedType -> "stream.read${name.capitalize()}()"
            is Declaration ->
                if (isAbstract) "ctx.serializers.readPolymorphic<${substitutedName(decl)}>(ctx, stream)"
                else "${substitutedName(decl)}.read(ctx, stream)"
            is INullable -> "stream.readNullable {${itemType.reader()}}"
            is IArray ->
                if (isPrimitivesArray) "stream.read${substitutedName(decl)}()"
                else "stream.readArray {${itemType.reader()}}"
            is IImmutableList -> "stream.readList {${itemType.reader()}}"

            else -> fail("Unknown declaration: $decl")
        }


        fun Member.reader() : String  = when (this) {
            is Member.Field -> type.reader()
            is Member.Reactive -> "$implSimpleName.read(ctx, stream${customSerializers(decl, leadingComma = true)})"

            else -> fail("Unknown member: $this")
        }


        + "@Suppress(\"UNCHECKED_CAST\")"
        + "override fun read(ctx: SerializationCtx, stream: InputStream): ${decl.name} {"
        indent {
            if(decl is Class && decl.isInternRoot) {
                + "val ctx = ctx.withInternRootHere(false)"
            }
            decl.allMembers.println {"val ${it.encapsulatedName} = ${it.reader()}"}
            + "return ${decl.name}(${decl.allMembers.joinToString(", ") { it.encapsulatedName }})${(decl is Class && decl.isInternRoot).condstr { ".apply { mySerializationContext = ctx }" }}"
        }
        + "}"
    }



    protected fun PrettyPrinter.writerTrait(decl: Declaration) {


        fun IType.writer(field: String) : String  = when (this) {
            is Enum -> "stream.writeEnum($field)"
            is InternedScalar -> "ctx.writeInterned(stream, $field, { ctx, stream, value -> ${itemType.writer("value")} })"
            is PredefinedType -> "stream.write${name.capitalize()}($field)"
            is Declaration ->
                if (isAbstract) "ctx.serializers.writePolymorphic(ctx, stream, $field)"
                else "${substitutedName(decl)}.write(ctx, stream, $field)"
            is INullable -> "stream.writeNullable($field) {${itemType.writer("it")}}"
            is IArray ->
                if (isPrimitivesArray) "stream.write${substitutedName(decl)}($field)"
                else "stream.writeArray($field) {${itemType.writer("it")}}"
            is IImmutableList -> "stream.writeList($field) {v -> ${itemType.writer("v")}}"

            else -> fail("Unknown declaration: $decl")
        }



        fun Member.writer() : String = when (this) {
            is Member.Field -> type.writer("value.$encapsulatedName")
            is Member.Reactive -> "$implSimpleName.write(ctx, stream, value.$encapsulatedName)"

            else -> fail("Unknown member: $this")
        }


        + "override fun write(ctx: SerializationCtx, stream: OutputStream, value: ${decl.name}) {"
        indent {
            if(decl is Class && decl.isInternRoot) {
                + "val ctx = ctx.withInternRootHere(true)"
                + "value.mySerializationContext = ctx"
            }
            decl.allMembers.println(Member::writer)
        }
        + "}"
    }



    protected fun  PrettyPrinter.fieldsTrait(decl: Declaration) {
        decl.ownMembers
            .filter { it.isEncapsulated }
            .printlnWithBlankLine { "val ${it.publicName} : ${it.intfSubstitutedName(decl)} get() = ${it.encapsulatedName}" }

        if (decl is Class && decl.isInternRoot) {
            + "private var mySerializationContext : SerializationCtx? = null"
            + "override val serializationContext : SerializationCtx"
            indent {
                + "get() = mySerializationContext ?: throw IllegalStateException(\"Attempting to get serialization context too soon for \$name\")"
            }
        }
    }



    protected fun PrettyPrinter.initializerTrait(decl: Declaration) {
        decl.ownMembers
            .filterIsInstance<Member.Reactive.Stateful>()
            .filter {it !is Member.Reactive.Stateful.Text && it.genericParams.none { it is IBindable }}
            .printlnWithPrefixSuffixAndIndent("init {", "}\n") { "${it.encapsulatedName}.optimizeNested = true" }

        decl.ownMembers
            .filterIsInstance<Member.Reactive>()
            .filter {it.freeThreaded}
            .printlnWithPrefixSuffixAndIndent("init {", "}\n") { "${it.encapsulatedName}.async = true" }
    }



    protected fun PrettyPrinter.secondaryConstructorTrait(decl: Declaration) {
        if ((decl is Class.Concrete || decl is Struct.Concrete) && decl.allMembers.any { it.hasEmptyConstructor }) {
            + "constructor("
            indent {
                + decl.allMembers
                    .filter { !it.hasEmptyConstructor }
                    .joinToString(",\n") { it.ctorParam(decl) }
            }
            + ") : this ("
            indent {
                + decl.allMembers
                    .joinToString (",\n") {
                        if (!it.hasEmptyConstructor) it.encapsulatedName
                        else "${it.implSubstitutedName(decl)}(${(it as? Member.Reactive)?.customSerializers(decl, leadingComma = false) ?: ""})"
                    }
            }
            + ")"
            println()
        }
    }



    protected fun PrettyPrinter.initMethodTrait(decl: Declaration) {
        if (!(decl is Class.Concrete || decl is Toplevel)) return

        + "override fun init(lifetime: Lifetime) {"
        indent {
            decl.allMembers.filter { it.isBindable } .println { "${it.encapsulatedName}.bind(lifetime, this, \"${it.name}\")" }
        }
        + "}"

    }

    protected fun PrettyPrinter.identifyMethodTrait(decl: Declaration) {
        if (!(decl is Class.Concrete || decl is Toplevel)) return

        + "override fun identify(ids: IIdentities) {"
        indent {
            decl.allMembers.filter { it.isBindable } .println { "${it.encapsulatedName}.identify(ids)" }
        }
        + "}"
    }

    private fun PrettyPrinter.equalsTrait(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

        fun IScalar.eq(v : String) = when (this) {
            is IArray ->
                if (isPrimitivesArray) "!Arrays.equals($v, other.$v)"
                else "!Arrays.deepEquals($v, other.$v)"
            else -> "$v != other.$v"
        }


        + "override fun equals(other: Any?): Boolean {"
        indent {
            + "if (this === other) return true"
            + "if (other?.javaClass != javaClass) return false"
            println()
            + "other as ${decl.name}"
            println()

            decl.allMembers.println { m ->
                val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")
                "if (${t.eq(f.encapsulatedName)}) return false"
            }
            println()
            + "return true"
        }
        +"}"
    }



    private fun PrettyPrinter.hashCodeTrait(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

        fun IScalar.hc(v : String) : String = when (this) {
            is IArray ->
                if (isPrimitivesArray) "Arrays.hashCode($v)"
                else "Arrays.deepHashCode($v)"
            is INullable -> "if ($v != null) " + (itemType as IScalar).hc(v) + " else 0"
            else -> "$v.hashCode()"
        }


        + "override fun hashCode(): Int {"
        indent {
            + "var __r = 0"

            decl.allMembers.println { m ->
                val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")
                "__r = __r*31 + ${t.hc(f.encapsulatedName)}"
            }

            + "return __r"
        }
        +"}"
    }


    private fun PrettyPrinter.prettyPrintTrait(decl: Declaration) {
        if (!(decl is Toplevel || decl is Class.Concrete || decl is Struct.Concrete)) return

        + "override fun print(printer: PrettyPrinter) {"
        indent {
            + "printer.println(\"${decl.name} (\")"
            decl.allMembers.printlnWithPrefixSuffixAndIndent("printer.indent {", "}") { "print(\"${it.name} = \"); ${it.encapsulatedName}.print(printer); println()"}
            + "printer.print(\")\")"
        }
        + "}"
    }



    private fun PrettyPrinter.primaryCtorParamsTrait(decl: Declaration) {
        fun ctorParamAccessModifier(member: Member) = member.isEncapsulated.condstr { if (decl.isAbstract) "protected" else "private" }

        val own = decl.ownMembers.map { "${ctorParamAccessModifier(it).optSpace()}val ${it.ctorParam(decl)}" }
        val base = decl.membersOfBaseClasses.map { it.ctorParam(decl) }
        
        + own.plus(base).joinToString(",\n")
    }


    protected open fun PrettyPrinter.baseClassTrait(decl: Declaration) {
        val base = decl.base ?: let {
            if (decl is Class || decl is Toplevel) p(" : RdBindableBase()")
            if (decl is Struct) p(" : IPrintable")
            return
        }

        + " : ${base.name} ("
        indent {
            + base.allMembers.joinToString(",\n") { it.encapsulatedName }
        }
        p(")")

    }


    protected open fun PrettyPrinter.enum(decl: Enum) {
        + "enum class ${decl.name} {"
        indent {
            + decl.constants.joinToString(separator = ",\r\n", postfix = ";") { it.name }
            println()
            + "companion object { val marshaller = FrameworkMarshallers.enum<${decl.name}>() }"
        }
        + "}"
    }

    override fun toString(): String {
        return "Kotlin11Generator(flowTransform=$flowTransform, defaultNamespace='$defaultNamespace', folder=$folder)"
    }


}
