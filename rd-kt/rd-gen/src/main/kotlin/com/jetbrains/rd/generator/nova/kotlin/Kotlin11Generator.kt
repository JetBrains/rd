package com.jetbrains.rd.generator.nova.kotlin

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.Enum
import com.jetbrains.rd.generator.nova.FlowKind.*
import com.jetbrains.rd.generator.nova.util.joinToOptString
import com.jetbrains.rd.util.eol
import com.jetbrains.rd.util.hash.IncrementalHash64
import com.jetbrains.rd.util.string.Eol
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.string.printer

import java.io.File

fun PrettyPrinter.block(title: String, body: PrettyPrinter.() -> Unit) {
    + "$title {"
    indent(body)
    + "}"
}

open class Kotlin11Generator(
    flowTransform: FlowTransform,
    private val defaultNamespace: String,
    override val folder: File
) : GeneratorBase(flowTransform) {


    //language specific properties
    object Namespace : ISetting<String, Declaration>
    val Declaration.namespace: String get() = getSetting(Namespace) ?: defaultNamespace

    object Intrinsic : SettingWithDefault<KotlinIntrinsicMarshaller, Declaration>(KotlinIntrinsicMarshaller.default)

    object Attributes : ISetting<Array<String>, SettingsHolder>
    object PublicCtors: ISetting<Unit, Declaration>

    object RefineFieldType: ISetting<Pair<String, IType>, SettingsHolder>

    object FsPath : ISetting<(Kotlin11Generator) -> File, Toplevel>
    protected open val Toplevel.fsPath: File get() = getSetting(FsPath)?.invoke(this@Kotlin11Generator) ?: File(folder, "$name.Generated.kt")


    private val Member.Reactive.Stateful.optimizeNested : Boolean
        get() = (this !is Member.Reactive.Stateful.Extension && this.genericParams.none { it is IBindable })

//    object MasterStateful : ISetting<Boolean, Declaration>
//    private val Member.Reactive.Stateful.Property.master : Boolean
//        get() = owner.getSetting(MasterStateful) ?: this@Kotlin11Generator.master
//
//    private val Member.Reactive.Stateful.Map.master : Boolean
//        get() = (owner.getSetting(MasterStateful) ?: this@Kotlin11Generator.master)

    private val IType.isPredefinedNumber: Boolean
        get() = this is PredefinedType.UnsignedIntegral ||
                this is PredefinedType.NativeIntegral ||
                this is PredefinedType.bool ||
                this is PredefinedType.char

    private val IType.isPrimitive: Boolean
        get() = this is PredefinedType.NativeFloatingPointType || this.isPredefinedNumber

    private val IType.isPrimitivesArray : Boolean get() =
        this is IArray && itemType.isPrimitive

    ///types
    protected open fun IType.substitutedName(scope: Declaration) : String = when (this) {
        is Enum -> if (flags) "EnumSet<${sanitizedName(scope)}>" else sanitizedName(scope)
        is Declaration -> sanitizedName(scope)
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
        is PredefinedType.UnsignedIntegral -> "U${itemType.substitutedName(scope)}"
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
                Sink -> "IRdEndpoint"
                Source -> "IRdCall"
                Both -> "RdCall"
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
        is Member.Reactive.Task -> "RdCall"
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
        is Member.Const -> type.substitutedName(scope)
    }

    protected open fun Member.Reactive.intfSubstitutedMapName(scope: Declaration) =
        "IPerContextMap<${context!!.type.substitutedName(scope)}, " + intfSubstitutedName(scope) + ">"


    protected open fun Member.implSubstitutedName(scope: Declaration, perContextRawName: Boolean = false) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
        is Member.Const -> type.substitutedName(scope)
        is Member.Reactive ->
            (implSimpleName + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) }).let { baseTypeName ->
                if (context != null && !perContextRawName) "RdPerContextMap<${context!!.type.substitutedName(scope)}, $baseTypeName>" else baseTypeName
            }
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

        fun PrettyPrinter.defaultValue(member: Member) {
            if (member is Member.Field && (member.isOptional || member.defaultValue != null))
                member.defaultValue.let { defaultValue ->
                    p(" = ")
                    when (defaultValue) {
                        is String -> p (
                                if (member.type is Enum) {
                                    var res = "$typeName.$defaultValue"
                                    if (member.type.flags)
                                        res = "enumSetOf(${(defaultValue != "").condstr { res }})"
                                    res
                                }
                                else
                                    "\"$defaultValue\""
                        )
                        else -> p("$defaultValue")
                    }
                }
        }

        return printer {
            p("$encapsulatedName: $typeName")
            defaultValue(this@ctorParam)
        }.toString()
    }

    protected fun Context.longRef(scope: Declaration): String {
        return when(this) {
            is Context.Generated -> pointcut!!.sanitizedName(scope) + "." + sanitizedName(scope)
            is Context.External -> fqnFor(this@Kotlin11Generator)
        }
    }

    protected fun Member.Reactive.customSerializers(scope: Declaration, ignorePerClientId: Boolean = false) : List<String> {
        if (context != null && !ignorePerClientId)
            return listOf(context!!.longRef(scope), perClientIdMapValueFactory(scope))
        return genericParams.asList().map { it.serializerRef(scope) }
    }

    protected fun Declaration.sanitizedName(scope: Declaration) : String {
        val needQualification = namespace != scope.namespace
        return needQualification.condstr { namespace + "." } + name
    }

    protected fun IType.leafSerializerRef(scope: Declaration): String? {
        return when (this) {
            is Enum -> "${sanitizedName(scope)}.marshaller"
            is PredefinedType -> "FrameworkMarshallers.$name"
            is Declaration ->
                this.getSetting(Intrinsic)?.marshallerObjectFqn ?: run {
                    val name = sanitizedName(scope)
                    if (isAbstract) "AbstractPolymorphic($name)" else name
                }

            is IArray -> if (this.isPrimitivesArray) "FrameworkMarshallers.${substitutedName(scope)}" else null
            else -> null
        }
    }

    protected fun IType.serializerRef(scope: Declaration) : String = leafSerializerRef(scope) ?: when(this) {
        is InternedScalar -> "__${name}At${internKey.keyName}Serializer"
        else -> "__${name}Serializer"
    }




    //generation
    override fun realGenerate(toplevels: List<Toplevel>) {
        toplevels.forEach { tl ->
            tl.fsPath.bufferedWriter().use { writer ->
                PrettyPrinter().apply {
                    eolKind = Eol.osSpecified
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

        val types = tl.declaredTypes + unknowns(tl.declaredTypes)

        if (tl.isLibrary)
            libdef(tl, types)
        else
            typedef(tl)

        types.sortedBy { it.name }.forEach { type ->
            typedef(type)
        }
    }

    private val suppressWarnings = listOf("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "PackageDirectoryMismatch", "UnusedImport", "unused", "LocalVariableName", "CanBeVal", "PropertyName", "EnumEntryName", "ClassName", "ObjectPropertyName", "UnnecessaryVariable")

    protected open fun PrettyPrinter.namespace(decl: Declaration) {
        val warnings = suppressWarnings.joinToString(separator = ",") { "\"$it\"" }
        +"@file:Suppress($warnings)"
        + "package ${decl.namespace}"
    }

    protected open fun PrettyPrinter.imports(tl: Toplevel) {
        + "import com.jetbrains.rd.framework.*"
        + "import com.jetbrains.rd.framework.base.*"
        + "import com.jetbrains.rd.framework.impl.*"

        println()
        + "import com.jetbrains.rd.util.lifetime.*"
        + "import com.jetbrains.rd.util.reactive.*"
        + "import com.jetbrains.rd.util.string.*"
        + "import com.jetbrains.rd.util.*"
        + "import kotlin.reflect.KClass"

//        tl.referencedTypes.plus(tl.declaredTypes.flatMap { it.referencedTypes })
//            .filterIsInstance(Declaration::class.java)
//            .map {
//                it.namespace
//            }
//            .filterNot { it == tl.namespace }
//            .distinct()
//            .printlnWithBlankLine { "import $it.*;" }
    }


    protected open fun PrettyPrinter.libdef(decl: Toplevel, types: List<Declaration>) {
        if (decl.getSetting(Intrinsic) != null) return
        block("object ${decl.name} : ISerializersOwner ") {
            registerSerializersTrait(decl, types)
        }
    }

    protected open fun PrettyPrinter.typedef(decl: Declaration) {
        if (decl.getSetting(Intrinsic) != null || decl is Context) return

        println()
        println()

        docTrait(decl)

        decl.getSetting(Attributes)?.forEach {
            + "@$it"
        }

        if (decl is Enum) {
            enum(decl)
            return
        }

        if (decl.isAbstract) p("abstract ")
        if (decl.isDataClass) p("data ")


        + "class ${decl.name} ${decl.primaryCtorVisibility}("
        indent {
            primaryCtorParamsTrait(decl)
        }
        p(")")

        baseClassTrait(decl)

        if (isUnknown(decl)) {
            p(", IUnknownInstance")
        }

        block("") {
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
            + "//deepClone"
            deepCloneTrait(decl)
            + "//contexts"
            contextsTrait(decl)
        }

        if (decl.isExtension) {
            extensionTrait(decl as Ext)
        }
    }

    protected fun PrettyPrinter.contextsTrait(decl: Declaration) {
        if(decl is Toplevel) {
            decl.declaredTypes.forEach {
                if(it is Context.Generated)
                    +"object ${it.keyName}: RdContext<${it.type.substitutedName(decl)}>(\"${it.keyName}\", ${it.isHeavyKey}, ${it.type.serializerRef(decl)})"
            }
        }
    }

    protected fun PrettyPrinter.docTrait(decl: Declaration) {
        if (decl.sourceFileAndLine != null || decl.documentation != null || decl.ownMembers.any { !it.isEncapsulated && it.documentation != null }) {
            + "/**"
            if (decl.documentation != null) {
                + " * ${decl.documentation}"
            }
            for (member in decl.ownMembers.filter { !it.isEncapsulated && it.documentation != null }) {
                + " * @property ${member.name} ${member.documentation}"
            }
            if (decl.sourceFileAndLine != null) {
                + " * #### Generated from [${decl.sourceFileAndLine}]"
            }
            + " */"
        }
    }

    private fun isUnknown(decl: Declaration) =
            decl is Class.Concrete && decl.isUnknown ||
            decl is Struct.Concrete && decl.isUnknown

    private fun unknownMembers(decl: Declaration) =
            if (isUnknown(decl)) arrayOf("override val unknownId: RdId",
                                         "val unknownBytes: ByteArray")
            else emptyArray()

    private fun unknownMembersSecondary(decl: Declaration) =
            if (isUnknown(decl)) arrayOf("unknownId: RdId",
                                         "unknownBytes: ByteArray")
            else emptyArray()

    private fun unknownMemberNames(decl: Declaration) =
            if (isUnknown(decl)) arrayOf("unknownId",
                                         "unknownBytes")
            else emptyArray()

    override fun unknown(it: Declaration): Declaration? = super.unknown(it)?.setting(PublicCtors)

    private fun docComment(doc: String?) = (doc != null).condstr {
                "\n" +
                "/**\n" +
                " * $doc\n" +
                " */\n"
    }

    protected fun PrettyPrinter.constantTrait(decl: Declaration) {
        decl.constantMembers.forEach {
            val name = it.name
            val type = it.type.substitutedName(decl)
            val value = getDefaultValue(decl, it)
            + if (it.type is Enum) {
                "val $name : $type = $value"
            } else {
                "const val $name : $type = $value"
            }
        }
    }

    protected fun PrettyPrinter.companionTrait(decl: Declaration) {
        if (decl.isConcrete) {
            println()
            block("companion object : IMarshaller<${decl.name}>") {
                + "override val _type: KClass<${decl.name}> = ${decl.name}::class"
                println()
                readerTrait(decl)
                println()
                writerTrait(decl)
                println()
                customSerializersTrait(decl)
                println()
                constantTrait(decl)
            }
        } //todo root, singleton
        else if (decl.isAbstract) {
            println()
            block("companion object : IAbstractDeclaration<${decl.name}>") {
                abstractDeclarationTrait(decl)
                println()
                customSerializersTrait(decl)
                println()
                constantTrait(decl)
            }
        }

        if (decl is Toplevel) {
            println()
            block("companion object : ISerializersOwner") {
                println()
                registerSerializersTrait(decl, decl.declaredTypes + unknowns(decl.declaredTypes))
                println()
                println()
                createMethodTrait(decl)
                println()
                customSerializersTrait(decl)
                println()
                +"const val serializationHash = ${decl.serializationHash(IncrementalHash64()).result}L"
                println()
                constantTrait(decl)
            }
            + "override val serializersOwner: ISerializersOwner get() = ${decl.name}"
            + "override val serializationHash: Long get() = ${decl.name}.serializationHash"
            println()
        }
    }

    protected fun PrettyPrinter.customSerializersTrait(decl: Declaration) {
        fun IType.serializerBuilder() : String = leafSerializerRef(decl)?: when (this) {
            is IArray -> itemType.serializerBuilder() + ".array()"
            is IImmutableList -> itemType.serializerBuilder() + ".list()"
            is INullable -> itemType.serializerBuilder() + ".nullable()"
            is InternedScalar -> itemType.serializerBuilder() + ".interned(\"${internKey.keyName}\")"
            else -> fail("Unknown type: $this")
        }

        val allTypesForDelegation = decl.allMembers
            .filterIsInstance<Member.Reactive>()
            .flatMap { it.genericParams.toList() }
            .distinct()
            .filter { it.leafSerializerRef(decl) == null }

        allTypesForDelegation.println { "private val ${it.serializerRef(decl)} = ${it.serializerBuilder()}" }
    }

    protected fun PrettyPrinter.abstractDeclarationTrait(decl: Declaration) {
        block("override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): ${decl.name} ") {
            readerBodyTrait(unknown(decl)!!)
        }
    }

    protected fun PrettyPrinter.registerSerializersTrait(decl: Toplevel, types: List<Declaration>) {
        block("override fun registerSerializersCore(serializers: ISerializers) ") {
            types.filter { !it.isAbstract }.filterIsInstance<IType>().println {
                "serializers.register(${it.serializerRef(decl)})"
            }

            if (decl is Root) {
                decl.toplevels.println { it.sanitizedName(decl) + ".register(serializers)" }
            }
        }
    }


    private fun Member.Reactive.perClientIdMapValueFactory(containing: Declaration): String {
        require(this.context != null)
        val params = (listOf(getDefaultValue(containing, this, true)) + customSerializers(containing, true)).filterNotNull().joinToString(", ")
        return "{ ${this.ctorSimpleName}($params)${(this is Member.Reactive.Stateful.Map).condstr { ".apply { master = it }" }} }"
    }

    //only for toplevel Exts
    protected fun PrettyPrinter.createMethodTrait(decl: Toplevel) {
        if (decl.isExtension) return

        block("fun create(lifetime: Lifetime, protocol: IProtocol): ${decl.name} ") {
            + "${decl.root.sanitizedName(decl)}.register(protocol.serializers)"
            println()

            + "return ${decl.name}().apply {"
            indent {
                val quotedName = "\"${decl.name}\""
               + "identify(protocol.identity, RdId.Null.mix($quotedName))"
               + "bind(lifetime, protocol, $quotedName)"
            }
            +"}"
        }

    }

    private fun getDefaultValue(containing: Declaration, member: Member, ignorePerClientId: Boolean = false): String? =
            if (!ignorePerClientId && member is Member.Reactive && member.context != null)
                null
            else

            when (member) {
                is Member.Reactive.Stateful.Property -> when {
                    member.defaultValue is String -> "\"" + member.defaultValue + "\""
                    member.defaultValue is Member.Const -> member.defaultValue.name
                    member.defaultValue != null -> member.defaultValue.toString()
                    member.isNullable -> "null"
                    else -> null
                }
                is Member.Const -> {
                    val value = member.value
                    when (member.type) {
                        is PredefinedType.char -> "\'$value\'"
                        is PredefinedType.string -> "\"$value\""
                        is PredefinedType.long -> "${value}L"
                        is PredefinedType.float -> "${value}f"
                        is PredefinedType.UnsignedIntegral -> "${value}u"
                        is Enum -> "${member.type.substitutedName(containing)}.$value"
                        else -> value
                    }
                }
                is Member.Reactive.Stateful.Extension -> member.delegatedBy.sanitizedName(containing) + "()"
                else -> null
            }


    protected fun PrettyPrinter.readerTrait(decl: Declaration) {
        + "@Suppress(\"UNCHECKED_CAST\")"
        block("override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ${decl.name} ") {
            if (isUnknown(decl)) {
                + "throw NotImplementedError(\"Unknown instances should not be read via serializer\")"
            } else {
                readerBodyTrait(decl)
            }
        }
    }

    fun PrettyPrinter.readerBodyTrait(decl: Declaration) {
        fun IType.reader(): String = when (this) {
            is Enum -> "buffer.readEnum$setOrEmpty<${sanitizedName(decl)}>()"
            is InternedScalar -> "ctx.readInterned(buffer, \"${internKey.keyName}\") { _, _ -> ${itemType.reader()} }"
            is PredefinedType -> "buffer.read${name.capitalize()}()"
            is Declaration ->
                this.getSetting(Intrinsic)?.marshallerObjectFqn?.let {"$it.read(ctx, buffer)"}
                        ?: if (isAbstract)
                            "ctx.serializers.readPolymorphic<${substitutedName(decl)}>(ctx, buffer, ${substitutedName(decl)})"
                        else
                            "${substitutedName(decl)}.read(ctx, buffer)"
            is INullable -> "buffer.readNullable { ${itemType.reader()} }"
            is IArray ->
                if (isPrimitivesArray) "buffer.read${substitutedName(decl)}()"
                else "buffer.readArray {${itemType.reader()}}"
            is IImmutableList -> "buffer.readList { ${itemType.reader()} }"

            else -> fail("Unknown declaration: $decl")
        }

        fun Member.reader(): String = when (this) {
            is Member.Field -> type.reader()
            is Member.Reactive.Stateful.Extension -> "$ctorSimpleName(${delegatedBy.reader()})"
            is Member.Reactive -> {
                val params = (listOf("ctx", "buffer") + customSerializers(decl)).joinToString (", ")
                if(context != null) {
                    "RdPerContextMap.read(${context!!.longRef(decl)}, buffer) ${this.perClientIdMapValueFactory(decl)}"
                } else {
                    "$implSimpleName.read($params)"
                }

            }
            else -> fail("Unknown member: $this")
        }

        fun Member.valName(): String = encapsulatedName.let { it + (it == "ctx" || it == "buffer").condstr { "_" } }

        val unknown = isUnknown(decl)
        if (unknown) {
            +"val objectStartPosition = buffer.position"
        }
        if (decl is Class || decl is Aggregate) {
            +"val _id = RdId.read(buffer)"
        }
        (decl.membersOfBaseClasses + decl.ownMembers).println { "val ${it.valName()} = ${it.reader()}" }
        if (unknown) {
            +"val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)"
            +"buffer.readByteArrayRaw(unknownBytes)"
        }
        val ctorParams = decl.allMembers.asSequence().map { it.valName() }.plus(unknownMemberNames(decl)).joinToString(", ")
        p("return ${decl.name}($ctorParams)")
        if (decl is Class || decl is Aggregate) {
            p(".withId(_id)")
        }
        if (decl is Class && decl.internRootForScopes.isNotEmpty()) {
            p(".apply { mySerializationContext = ctx.withInternRootsHere(this, ${decl.internRootForScopes.joinToString { "\"$it\"" }}) }")
        }
        println()
    }


    protected fun PrettyPrinter.writerTrait(decl: Declaration) {


        fun IType.writer(field: String) : String  = when (this) {
            is Enum -> "buffer.writeEnum$setOrEmpty($field)"
            is InternedScalar -> "ctx.writeInterned(buffer, $field, \"${internKey.keyName}\") { _, _, internedValue -> ${itemType.writer("internedValue")} }"
            is PredefinedType -> "buffer.write${name.capitalize()}($field)"
            is Declaration ->
                this.getSetting(Intrinsic)?.marshallerObjectFqn?.let {"$it.write(ctx,buffer, $field)"} ?:
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
            is Member.Reactive -> if(context != null) {
                "RdPerContextMap.write(buffer, value.$encapsulatedName)"
            } else {
                "$implSimpleName.write(ctx, buffer, value.$encapsulatedName)"
            }

            else -> fail("Unknown member: $this")
        }


        block("override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ${decl.name}) ") {
            if (decl is Class || decl is Aggregate) {
                + "value.rdid.write(buffer)"
            }
            (decl.membersOfBaseClasses + decl.ownMembers).println(Member::writer)
            if (isUnknown(decl)) {
                + "buffer.writeByteArrayRaw(value.unknownBytes)"
            }
            if(decl is Class && decl.internRootForScopes.isNotEmpty()) {
                + "value.mySerializationContext = ctx.withInternRootsHere(value, ${decl.internRootForScopes.joinToString { "\"$it\"" }})"
            }
        }
    }

    protected fun PrettyPrinter.fieldsTrait(decl: Declaration) {
        for (member in decl.ownMembers.filter { it.isEncapsulated }) {
            p(docComment(member.documentation))
            if(member is Member.Reactive && member.context != null) {
                +"val ${member.publicName}: ${member.intfSubstitutedName(decl)} get() = ${member.encapsulatedName}.getForCurrentContext()"
                +"val ${member.publicName}PerContextMap: ${member.intfSubstitutedMapName(decl)} get() = ${member.encapsulatedName}"
            } else
                +"val ${member.publicName}: ${member.intfSubstitutedName(decl)} get() = ${member.encapsulatedName}"
        }

        if (decl is Class && decl.internRootForScopes.isNotEmpty()) {
            + "private var mySerializationContext: SerializationCtx? = null"
            + "override val serializationContext: SerializationCtx"
            indent {
                + "get() = mySerializationContext ?: throw IllegalStateException(\"Attempting to get serialization context too soon for \$location\")"
            }
        }
    }



    protected fun PrettyPrinter.initializerTrait(decl: Declaration) {
//        decl.ownMembers
//            .filterIsInstance<Member.Reactive.Stateful.Property>()
//            .filter { it.perContextKey == null }
//            .printlnWithPrefixSuffixAndIndent("init {", "}\n") { "${it.encapsulatedName}.isMaster = ${it.master}" }
//
//        decl.ownMembers
//            .filterIsInstance<Member.Reactive.Stateful.Map>()
//            .filter { it.perContextKey == null }
//            .printlnWithPrefixSuffixAndIndent("init {", "}\n") { "${it.encapsulatedName}.master = ${it.master}" }

        decl.ownMembers
            .filterIsInstance<Member.Reactive.Stateful>()
            .filter { it !is Member.Reactive.Stateful.Extension && it.genericParams.none { it is IBindable } && it.context == null }
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
                    .asSequence()
                    .filter { !it.hasEmptyConstructor }.map { it.ctorParam(decl) }.plus(unknownMembersSecondary(decl))
                .joinToString(",\n")
        }
        + ") : this("
        indent {
            + decl.allMembers
                    .asSequence()
                    .map {
                        if (!it.hasEmptyConstructor) it.encapsulatedName
                        else {
                            val initialValue = getDefaultValue(decl, it)
                            val params = (listOf(initialValue) + ((it as? Member.Reactive)?.customSerializers(decl)?: emptyList())).filterNotNull().joinToString (", ")
                            "${it.ctorSubstitutedName(decl)}($params)"
                        }
                    }.plus(unknownMemberNames(decl)).joinToString(",\n")
        }
        + ")"
        println()
    }

    private fun PrettyPrinter.equalsTrait(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

        fun IScalar.eq(v : String) = when (this) {
            is IArray ->
                if (isPrimitivesArray) "!($v contentEquals other.$v)"
                else "!($v contentDeepEquals other.$v)"
            else -> "$v != other.$v"
        }


        block("override fun equals(other: Any?): Boolean ") {
            + "if (this === other) return true"
            + "if (other == null || other::class != this::class) return false"
            println()
            + "other as ${decl.name}"
            println()

            decl.allMembers.println { m ->
                val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")

                if (f.usedInEquals)
                    "if (${t.eq(f.encapsulatedName)}) return false"
                else
                    ""
            }
            println()
            + "return true"
        }
    }



    private fun PrettyPrinter.hashCodeTrait(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

        fun IScalar.hc(v : String) : String = when (this) {
            is IArray ->
                if (isPrimitivesArray) "$v.contentHashCode()"
                else "$v.contentDeepHashCode()"
            is INullable -> "if ($v != null) " + (itemType as IScalar).hc(v) + " else 0"
            else -> "$v.hashCode()"
        }


        block("override fun hashCode(): Int ") {
            + "var __r = 0"

            decl.allMembers.println { m ->
                val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")
                if (f.usedInEquals)
                    "__r = __r*31 + ${t.hc(f.encapsulatedName)}"
                else
                    ""
            }

            + "return __r"
        }
    }


    private fun PrettyPrinter.prettyPrintTrait(decl: Declaration) {
        if (!(decl is Toplevel || decl.isConcrete)) return

        block("override fun print(printer: PrettyPrinter) ") {
            + "printer.println(\"${decl.name} (\")"
            decl.allMembers.printlnWithPrefixSuffixAndIndent("printer.indent {", "}") { "print(\"${it.name} = \"); ${it.encapsulatedName}.print(printer); println()"}
            + "printer.print(\")\")"
        }

        if (decl is Struct.Concrete && decl.base != null) {
            println()
            + "override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()"
        }
    }


    private fun PrettyPrinter.deepCloneTrait(decl: Declaration) {

        if (!(decl is BindableDeclaration && (decl is Toplevel || decl.isConcrete))) return

        block("override fun deepClone(): ${decl.name}  ") {

            + "return ${decl.name}("
            indent {
                + decl.allMembers
                    .asSequence()
                    .map {
                        it.encapsulatedName + it.isBindable.condstr {".deepClonePolymorphic()" }
                    }.plus(unknownMemberNames(decl)).joinToString(",\n")
            }
            + ")"
        }

        if (decl is Struct.Concrete && decl.base != null) {
            println()
            + "override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()"
        }
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
            val attrs = it.getSetting(Attributes)?.fold("") { acc,attr -> "$acc@$attr${eolKind.value}" }
            (attrs?:"") + "${ctorParamAccessModifier(it)}val ${it.ctorParam(decl)}"
        }
        val base = decl.membersOfBaseClasses.map { it.ctorParam(decl) }

        + own.asSequence().plus(base).plus(unknownMembers(decl)).joinToString(",\n")
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
        block("enum class ${decl.name}") {
            +decl.constants.joinToString(separator = ", $eol", postfix = ";") {
                docComment(it.documentation) + it.name
            }
            println()
            block("companion object") {
                +"val marshaller = FrameworkMarshallers.enum${decl.setOrEmpty}<${decl.name}>()"
                println()
                constantTrait(decl)
            }
        }
    }

    private fun PrettyPrinter.extensionTrait(decl: Ext) {
        val pointcut = decl.pointcut ?: return
        val lowerName = decl.name.decapitalize()
        val extName = decl.extName ?: lowerName
        + """val ${pointcut.sanitizedName(decl)}.$extName get() = getOrCreateExtension("$lowerName", ::${decl.name})"""
        println()
    }

    override fun toString(): String {
        return "Kotlin11($flowTransform, \"$defaultNamespace\", '${folder.canonicalPath}')"
    }


}
