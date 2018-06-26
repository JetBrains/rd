package com.jetbrains.rider.generator.nova.kotlin

import com.jetbrains.rider.generator.nova.*
import com.jetbrains.rider.generator.nova.Enum
import com.jetbrains.rider.generator.nova.FlowKind.*
import com.jetbrains.rider.generator.nova.util.joinToOptString
import com.jetbrains.rider.util.hash.IncrementalHash64
import com.jetbrains.rider.util.string.Eol
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.string.condstr
import java.io.File

open class Kotlin11Generator(val flowTransform: FlowTransform, val defaultNamespace: String, override val folder : File) : GeneratorBase() {

    //language specific properties
    object Namespace : ISetting<String, Declaration>
    val Declaration.namespace: String get() = getSetting(Namespace) ?: defaultNamespace

    object Intrinsic : SettingWithDefault<KotlinIntrinsicMarshaller, Declaration>(KotlinIntrinsicMarshaller.default)

    object Attributes : ISetting<Array<String>, SettingsHolder>
    object PublicCtors: ISetting<Unit, Declaration>

    object FsPath : ISetting<(Kotlin11Generator) -> File, Toplevel>
    protected open val Toplevel.fsPath: File get() = getSetting(FsPath)?.invoke(this@Kotlin11Generator) ?: File(folder, "$name.Generated.kt")


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
        is PredefinedType.secureString -> "RdSecureString"
        is PredefinedType.void -> "Unit"
        is PredefinedType -> name.capitalize()

        else -> fail("Unsupported type ${javaClass.simpleName}")
    }



    //members
    val Member.Reactive.actualFlow : FlowKind get() = flowTransform.transform(flow)

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open val Member.Reactive.intfSimpleName : String get () {
        val async = this.freeThreaded.condstr { "Async" }
        return when (this) {
            is Member.Reactive.Task -> when (actualFlow) {
                Sink -> "RdEndpoint"
                Source -> "IRdCall"
                Both -> fail("Unsupported flow direction for tasks")
            }
            is Member.Reactive.Signal -> when (actualFlow) {
                Sink -> "I${async}Source"
                Source, Both -> "I${async}Signal"
            }
            is Member.Reactive.Stateful.Property -> when (actualFlow) {
                Sink -> if (isNullable || defaultValue != null) "IPropertyView" else "IOptPropertyView"
                Source, Both -> if (isNullable || defaultValue != null) "IProperty" else "IOptProperty"
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
        is Member.Reactive.Stateful.Property -> if (isNullable || defaultValue != null) "RdProperty" else "RdOptionalProperty"
        is Member.Reactive.Stateful.List -> "RdList"
        is Member.Reactive.Stateful.Set -> "RdSet"
        is Member.Reactive.Stateful.Map -> "RdMap"
        is Member.Reactive.Stateful.Extension -> fqn(this@Kotlin11Generator, flowTransform)

        else -> fail ("Unsupported member: $this")
    }


    protected open val Member.Reactive.ctorSimpleName : String get () = when (this) {
        is Member.Reactive.Stateful.Extension -> factoryFqn(this@Kotlin11Generator, flowTransform)
        else -> implSimpleName
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


    protected open fun Member.ctorSubstitutedName(scope: Declaration) = when (this) {
        is Member.Reactive.Stateful.Extension -> ctorSimpleName + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) }
        else -> implSubstitutedName(scope)
    }


    protected open val Member.isBindable : Boolean get() = when (this) {
        is Member.Field -> type is IBindable
        is Member.Reactive -> true

        else -> false
    }


    protected open val Member.publicName : String get() = name
    protected open val Member.encapsulatedName : String get() = isEncapsulated.condstr { "_" } + publicName
    protected open val Member.isEncapsulated : Boolean get() = this is Member.Reactive

    protected fun Member.ctorParam(containing: Declaration): String {
        val typeName = implSubstitutedName(containing)
        return StringBuilder().also {
            it.append("$encapsulatedName: $typeName")
            if (this is Member.Field && (isOptional || defaultValue != null)) {
                it.append(" = ")
                val defaultValue = this.defaultValue
                when (defaultValue) {
                    is String -> it.append(if (type is Enum) "$typeName.$defaultValue" else "\"$defaultValue\"")
                    is Long -> it.append(defaultValue)
                    else -> if (isOptional) it.append("null")
                }
            }
        }.toString()
    }

    protected fun Member.Reactive.customSerializers(scope: Declaration) : List<String> {
        return genericParams.asList().map { it.serializerRef(scope) }
    }

    protected fun Declaration.sanitizedName(scope: Declaration) : String {
        val needQualification = namespace != scope.namespace
        return needQualification.condstr { namespace + "." } + name
    }

    protected fun IType.leafSerializerRef(scope: Declaration) : String? = when (this) {
        is Enum -> "${sanitizedName(scope)}.marshaller"
        is PredefinedType -> "FrameworkMarshallers.$name"
        is Declaration ->
            this.getSetting(Intrinsic)?.marshallerObjectFqn ?:
                if (isAbstract) "Polymorphic<${sanitizedName(scope)}>()" else sanitizedName(scope)
        is IArray -> if (this.isPrimitivesArray) "FrameworkMarshallers.$name" else null
        else -> null
    }

    protected fun IType.serializerRef(scope: Declaration) : String = leafSerializerRef(scope) ?: "__${name}Serializer"




    //generation
    override fun generate(root: Root, clearFolderIfExists: Boolean, toplevels: List<Toplevel>) {
        prepareGenerationFolder(folder, clearFolderIfExists)

        toplevels.sortedBy { it.name }.forEach { tl ->
            tl.fsPath.bufferedWriter().use { writer ->
                PrettyPrinter().apply {
                    eolKind = Eol.linux
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
        + """@file:Suppress("PackageDirectoryMismatch", "UnusedImport", "unused", "LocalVariableName")"""
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
        + "import kotlin.reflect.KClass"

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
        + "object ${decl.name} : ISerializersOwner {"
        indent {
            registerSerializersTrait(decl)
        }
        + "}"
    }

    protected open fun PrettyPrinter.typedef(decl: Declaration) {
        if (decl.getSetting(Kotlin11Generator.Intrinsic) != null) return

        println()
        println()


        decl.getSetting(Attributes)?.forEach {
            + "@$it"
        }

        if (decl is Enum) {
            enum(decl)
            return
        }

        if (decl.isAbstract) p("abstract ")
        if (decl is Struct.Concrete && decl.base == null) p("data ")


        + "class ${decl.name} ${decl.primaryCtorVisibility}("
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
            + "//equals trait"
            equalsTrait(decl)
            + "//hash code trait"
            hashCodeTrait(decl)
            + "//pretty print"
            prettyPrintTrait(decl)
        }
        + "}"

        if (decl.isExtension) {
            extensionTrait(decl as Ext)
        }
    }



    protected fun PrettyPrinter.companionTrait(decl: Declaration) {
        if (decl.isConcrete) {
            println()
            + "companion object : IMarshaller<${decl.name}> {"
            indent {
                + "override val _type: KClass<${decl.name}> = ${decl.name}::class"
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
            + "companion object : ISerializersOwner {"
            indent {
                println()
                registerSerializersTrait(decl)
                println()
                println()
                createMethodTrait(decl)
                println()
                customSerializersTrait(decl)
            }
            + "}"
            + "override val serializersOwner: ISerializersOwner get() = ${decl.name}"
            + "override val serializationHash: Long get() = ${decl.serializationHash(IncrementalHash64()).result}L"
            println()
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
        + "override fun registerSerializersCore(serializers: ISerializers) {"
        indent {
            decl.declaredTypes.filter { !it.isAbstract }.filterIsInstance<IType>().println {
                "serializers.register(${it.serializerRef(decl)})"
            }

            if (decl is Root) {
                decl.toplevels.println { it.sanitizedName(decl) + ".register(serializers)" }
            }
        }
        + "}"
    }


    //only for toplevel Exts
    protected fun PrettyPrinter.createMethodTrait(decl: Toplevel) {
        if (decl.isExtension) return

        + "fun create(lifetime: Lifetime, protocol: IProtocol): ${decl.name} {"
        indent {
            + "${decl.root.sanitizedName(decl)}.register(protocol.serializers)"
            println()

            + "return ${decl.name}().apply {"
            indent {
                val quotedName = """"${decl.name}""""
               + "identify(protocol.identity, RdId.Null.mix($quotedName))"
               + "bind(lifetime, protocol, $quotedName)"
            }
            +"}"
        }
        + "}"

    }

    private fun getDefaultValue(containing: Declaration, member: Member): String? =
            if (member is Member.Reactive.Stateful.Property) {
                when {
                    member.defaultValue is String -> "\"" + member.defaultValue + "\""
                    member.defaultValue != null -> member.defaultValue.toString()
                    member.isNullable -> "null"
                    else -> null
                }
            }
            else if (member is Member.Reactive.Stateful.Extension)
                member.delegatedBy.sanitizedName(containing) + "()"
            else
                null


    protected fun PrettyPrinter.readerTrait(decl: Declaration) {


        fun IType.reader() : String  = when (this) {
            is Enum -> "buffer.readEnum<${substitutedName(decl)}>()"
            is InternedScalar -> "ctx.readInterned(buffer) { _, _ -> ${itemType.reader()} }"
            is PredefinedType -> "buffer.read${name.capitalize()}()"
            is Declaration ->
                this.getSetting(Intrinsic)?.marshallerObjectFqn?.let {"$it.Read(ctx,buffer)"} ?:
                    if (isAbstract) "ctx.serializers.readPolymorphic<${substitutedName(decl)}>(ctx, buffer)"
                    else "${substitutedName(decl)}.read(ctx, buffer)"
            is INullable -> "buffer.readNullable {${itemType.reader()}}"
            is IArray ->
                if (isPrimitivesArray) "buffer.read${substitutedName(decl)}()"
                else "buffer.readArray {${itemType.reader()}}"
            is IImmutableList -> "buffer.readList {${itemType.reader()}}"

            else -> fail("Unknown declaration: $decl")
        }


        fun Member.reader() : String  = when (this) {
            is Member.Field -> type.reader()
            is Member.Reactive.Stateful.Extension -> "$ctorSimpleName(${delegatedBy.reader()})"
            is Member.Reactive -> {
                val params = (listOf("ctx", "buffer") + customSerializers(decl)).joinToString (", ")
                "$implSimpleName.read($params)"
            }
            else -> fail("Unknown member: $this")
        }

        fun Member.valName() : String = encapsulatedName.let { it + (it == "ctx" || it == "buffer").condstr { "_" } }

        + "@Suppress(\"UNCHECKED_CAST\")"
        + "override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ${decl.name} {"
        indent {
            if(decl is Class && decl.isInternRoot) {
                + "val ctx = ctx.withInternRootHere(false)"
            }
            if (decl is Class || decl is Aggregate) {
                + "val _id = RdId.read(buffer)"
            }
            decl.allMembers.println {"val ${it.valName()} = ${it.reader()}"}
            p("return ${decl.name}(${decl.allMembers.joinToString(", ") { it.valName() }})${(decl is Class && decl.isInternRoot).condstr { ".apply { mySerializationContext = ctx }" }}")
            if (decl is Class || decl is Aggregate) {
                p(".withId(_id)")
            }
            println()
        }
        + "}"
    }



    protected fun PrettyPrinter.writerTrait(decl: Declaration) {


        fun IType.writer(field: String) : String  = when (this) {
            is Enum -> "buffer.writeEnum($field)"
            is InternedScalar -> "ctx.writeInterned(buffer, $field) { _, _, _ -> ${itemType.writer("value")} }"
            is PredefinedType -> "buffer.write${name.capitalize()}($field)"
            is Declaration ->
                this.getSetting(Intrinsic)?.marshallerObjectFqn?.let {"$it.Write(ctx,buffer, $field)"} ?:
                    if (isAbstract) "ctx.serializers.writePolymorphic(ctx, buffer, $field)"
                    else "${substitutedName(decl)}.write(ctx, buffer, $field)"
            is INullable -> "buffer.writeNullable($field) { ${itemType.writer("it")} }"
            is IArray ->
                if (isPrimitivesArray) "buffer.write${substitutedName(decl)}($field)"
                else "buffer.writeArray($field) { ${itemType.writer("it")} }"
            is IImmutableList -> "buffer.writeList($field) { v -> ${itemType.writer("v")} }"

            else -> fail("Unknown declaration: $decl")
        }



        fun Member.writer() : String = when (this) {
            is Member.Field -> type.writer("value.$encapsulatedName")
            is Member.Reactive.Stateful.Extension -> delegatedBy.writer(("value.$encapsulatedName.delegatedBy"))
            is Member.Reactive -> "$implSimpleName.write(ctx, buffer, value.$encapsulatedName)"

            else -> fail("Unknown member: $this")
        }


        + "override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ${decl.name}) {"
        indent {
            if(decl is Class && decl.isInternRoot) {
                + "val ctx = ctx.withInternRootHere(true)"
                + "value.mySerializationContext = ctx"
            }
            if (decl is Class || decl is Aggregate) {
                + "value.rdid.write(buffer)"
            }
            decl.allMembers.println(Member::writer)
        }
        + "}"
    }



    protected fun  PrettyPrinter.fieldsTrait(decl: Declaration) {
        decl.ownMembers
            .filter { it.isEncapsulated }
            .printlnWithBlankLine { "val ${it.publicName}: ${it.intfSubstitutedName(decl)} get() = ${it.encapsulatedName}" }

        if (decl is Class && decl.isInternRoot) {
            + "private var mySerializationContext: SerializationCtx? = null"
            + "override val serializationContext: SerializationCtx"
            indent {
                + "get() = mySerializationContext ?: throw IllegalStateException(\"Attempting to get serialization context too soon for \$location\")"
            }
        }
    }



    protected fun PrettyPrinter.initializerTrait(decl: Declaration) {
        decl.ownMembers
            .filterIsInstance<Member.Reactive.Stateful>()
            .filter { it !is Member.Reactive.Stateful.Extension && it.genericParams.none { it is IBindable }}
            .printlnWithPrefixSuffixAndIndent("init {", "}\n") { "${it.encapsulatedName}.optimizeNested = true" }

        decl.ownMembers
            .filterIsInstance<Member.Reactive>()
            .filter {it.freeThreaded}
            .printlnWithPrefixSuffixAndIndent("init {", "}\n") { "${it.encapsulatedName}.async = true" }

        decl.ownMembers
            .filter { it.isBindable }
            .printlnWithPrefixSuffixAndIndent("init {", "}\n") { """bindableChildren.add("${it.name}" to ${it.encapsulatedName})""" }
    }



    protected fun PrettyPrinter.secondaryConstructorTrait(decl: Declaration) {
        if (!decl.hasSecondaryCtor) return

        p((decl is Toplevel && !decl.hasSetting(PublicCtors)).condstr { if (!decl.isExtension) "private " else "internal " })

        + "constructor("
        indent {
            + decl.allMembers
                .filter { !it.hasEmptyConstructor }
                .joinToString(",\n") { it.ctorParam(decl) }
        }
        + ") : this("
        indent {
            + decl.allMembers
                .joinToString (",\n") {
                    if (!it.hasEmptyConstructor) it.encapsulatedName
                    else {
                        val initialValue = getDefaultValue(decl, it)
                        val params = (listOf(initialValue) + ((it as? Member.Reactive)?.customSerializers(decl)?: emptyList())).filterNotNull().joinToString (", ")
                        "${it.ctorSubstitutedName(decl)}($params)"
                    }
                }
        }
        + ")"
        println()
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
        if (!(decl is Toplevel || decl.isConcrete)) return

        + "override fun print(printer: PrettyPrinter) {"
        indent {
            + "printer.println(\"${decl.name} (\")"
            decl.allMembers.printlnWithPrefixSuffixAndIndent("printer.indent {", "}") { "print(\"${it.name} = \"); ${it.encapsulatedName}.print(printer); println()"}
            + "printer.print(\")\")"
        }
        + "}"
    }




    private val Declaration.primaryCtorVisibility : String get() {
        val modifier =
            when {
                hasSetting(PublicCtors) -> ""
                hasSecondaryCtor -> "private"
                isExtension -> "internal"
                this is Toplevel -> "private"
                else -> ""
            }
        return modifier.isNotEmpty().condstr { "$modifier constructor" }
    }

    protected val Declaration.hasSecondaryCtor : Boolean get () = (this is Toplevel || this.isConcrete) && this.allMembers.any { it.hasEmptyConstructor }

    protected open val Member.hasEmptyConstructor : Boolean get() = when (this) {
        is Member.Field -> type.hasEmptyConstructor && !emptyCtorSuppressed
        is Member.Reactive -> true

        else -> fail ("Unsupported member: $this")
    }

    private fun PrettyPrinter.primaryCtorParamsTrait(decl: Declaration) {
        fun ctorParamAccessModifier(member: Member) = member.isEncapsulated.condstr { if (decl.isAbstract) "protected " else "private " }

        val own = decl.ownMembers.map {
            val attrs = it.getSetting(Kotlin11Generator.Attributes)?.fold("") { acc,attr -> "$acc@$attr${eolKind.value}" }
            (attrs?:"") + "${ctorParamAccessModifier(it)}val ${it.ctorParam(decl)}"
        }
        val base = decl.membersOfBaseClasses.map { it.ctorParam(decl) }
        
        + own.plus(base).joinToString(",\n")
    }


    protected open fun PrettyPrinter.baseClassTrait(decl: Declaration) {
        val base = decl.base ?: let {
            if (decl is Toplevel) p( " : RdExtBase()")
            else if (decl is Class || decl is Aggregate || decl is Toplevel) p(" : RdBindableBase()")
            else if (decl is Struct) p(" : IPrintable")
            return
        }

        + " : ${base.sanitizedName(decl)} ("
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

    private fun PrettyPrinter.extensionTrait(decl: Ext) {
        val pointcut = decl.pointcut ?: return
        val lowerName = decl.name.decapitalize()
        val extName = decl.extName ?: lowerName
        + """val ${pointcut.name}.$extName get() = getOrCreateExtension("$lowerName", ::${decl.name})"""
        println()
    }

    override fun toString(): String {
        return "Kotlin11Generator(flowTransform=$flowTransform, defaultNamespace='$defaultNamespace', folder=$folder)"
    }


}
