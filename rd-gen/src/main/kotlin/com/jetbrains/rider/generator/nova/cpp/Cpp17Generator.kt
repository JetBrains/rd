package com.jetbrains.rider.generator.nova.cpp

import com.jetbrains.rider.generator.nova.*
import com.jetbrains.rider.generator.nova.Enum
import com.jetbrains.rider.generator.nova.FlowKind.*
import com.jetbrains.rider.generator.nova.util.appendDefaultValueSetter
import com.jetbrains.rider.generator.nova.util.joinToOptString
import com.jetbrains.rider.util.eol
import com.jetbrains.rider.util.string.Eol
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.string.condstr
import java.io.File

open class Cpp17Generator(val flowTransform: FlowTransform, val defaultNamespace: String, override val folder: File) : GeneratorBase() {

    //region language specific properties
    object Namespace : ISetting<String, Declaration>

    val Declaration.namespace: String get() = getSetting(Namespace) ?: defaultNamespace

    object Intrinsic : SettingWithDefault<CppIntrinsicMarshaller, Declaration>(CppIntrinsicMarshaller.default)

    object Attributes : ISetting<Array<String>, SettingsHolder>
    object PublicCtors : ISetting<Unit, Declaration>

    object FsPath : ISetting<(Cpp17Generator) -> File, Toplevel>

    private fun Declaration.fsName(isDefinition: Boolean) =
            "$name.${if (isDefinition) "cpp" else "h"}"

    protected open fun Declaration.fsPath(isDefinition: Boolean): File = getSetting(FsPath)?.invoke(this@Cpp17Generator)
            ?: File(folder, fsName(isDefinition))
    //endregion

    //region PrettyPrinter
    fun PrettyPrinter.carry(): String {
        return ";" + eolKind.value
    }

    fun PrettyPrinter.block(prefix: String, postfix: String, body: PrettyPrinter.() -> Unit) {
        +prefix
        indent(body)
        +postfix
    }

    fun PrettyPrinter.block(title: String, body: PrettyPrinter.() -> Unit) {
        +"$title {"
        indent(body)
        +"}"
    }

    fun PrettyPrinter.comment(comment: String) {
        +"${eolKind.value}//$comment"
    }

    fun PrettyPrinter.define(returnType: String, signature: String, decl: Declaration) {
        p("$returnType ${decl.scopeResolution()}$signature")
    }
    //endregion

    //region IType.
    protected open fun IType.substitutedName(scope: Declaration): String = when (this) {
        is Declaration -> (namespace != scope.namespace).condstr { "$namespace." } + name
        is INullable -> "std::optional<${itemType.substitutedName(scope)}>"
        is InternedScalar -> itemType.substitutedName(scope)
        is IArray -> "std::vector<${itemType.substitutedName(scope)}>"
        is IImmutableList -> "std::vector<${itemType.substitutedName(scope)}>"

        is PredefinedType.byte -> "signed char"
        is PredefinedType.int -> "int32_t"
        is PredefinedType.long -> "int64_t"
        is PredefinedType.string -> "std::string"
        is PredefinedType.dateTime -> "Date"
        is PredefinedType.guid -> "UUID"
        is PredefinedType.uri -> "URI"
        is PredefinedType.secureString -> "RdSecureString"
        is PredefinedType.void -> "void*"
        is PredefinedType -> name.decapitalize()

        else -> fail("Unsupported type ${javaClass.simpleName}")
    }

    protected val IType.isPrimitivesArray: Boolean
        get() =
            this is IArray && PredefinedIntegrals.contains(itemType) ||
                    this is IImmutableList && PredefinedIntegrals.contains(itemType)

    protected fun IType.leafSerializerRef(scope: Declaration): String? {
        return when (this) {
            is Enum -> "Polymorphic<${sanitizedName(scope)}>"
            is PredefinedType -> "Polymorphic<${substitutedName(scope)}>"
            is Declaration ->
                this.getSetting(Intrinsic)?.marshallerObjectFqn ?: run {
                    val name = "Polymorphic<${sanitizedName(scope)}>"
                    name
//                    if (isAbstract) "AbstractPolymorphic($name)" else name
                }

            is IArray -> if (this.isPrimitivesArray) "Polymorphic<${substitutedName(scope)}>" else null
            else -> null
        }
    }

    protected fun IType.serializerRef(scope: Declaration): String = leafSerializerRef(scope) ?: "__${name}Serializer"

    //endregion

    //region Member.
    val Member.Reactive.actualFlow: FlowKind get() = flowTransform.transform(flow)

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open val Member.Reactive.intfSimpleName: String
        get () {
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
                    Source, Both -> "IViewableList"
                }
                is Member.Reactive.Stateful.Set -> when (actualFlow) {
                    Sink -> "IViewableSet"
                    Source, Both -> "IViewableSet"
                }
                is Member.Reactive.Stateful.Map -> when (actualFlow) {
                    Sink -> "I${async}ViewableMap"
                    Source, Both -> "IViewableMap"
                }

                is Member.Reactive.Stateful.Extension -> implSimpleName

                else -> fail("Unsupported member: $this")
            }
        }

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open val Member.Reactive.implSimpleName: String
        get () = when (this) {
            is Member.Reactive.Task -> when (actualFlow) {
                Sink -> "RdEndpoint"
                Source -> "RdCall"
                Both -> "RdCall" //todo
            }
            is Member.Reactive.Signal -> "RdSignal"
//            is Member.Reactive.Stateful.Property -> if (isNullable || defaultValue != null) "RdProperty" else "RdOptionalProperty"
            is Member.Reactive.Stateful.Property -> if (isNullable || defaultValue != null) "RdProperty" else "RdProperty"
            is Member.Reactive.Stateful.List -> "RdList"
            is Member.Reactive.Stateful.Set -> "RdSet"
            is Member.Reactive.Stateful.Map -> "RdMap"
            is Member.Reactive.Stateful.Extension -> fqn(this@Cpp17Generator, flowTransform)

            else -> fail("Unsupported member: $this")
        }


    protected open val Member.Reactive.ctorSimpleName: String
        get () = when (this) {
            is Member.Reactive.Stateful.Extension -> factoryFqn(this@Cpp17Generator, flowTransform)
            else -> implSimpleName
        }

    protected open fun Member.intfSubstitutedName(scope: Declaration) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
        is Member.Reactive -> implSimpleName + (genericParams.toList().map { it.substitutedName(scope) } + customSerializers(scope)).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")
    }

    protected open fun Member.implSubstitutedName(scope: Declaration) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
//        is Member.Reactive -> implSimpleName + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) } + customSerializers(scope)
        is Member.Reactive -> implSimpleName + (genericParams.toList().map { it.substitutedName(scope) } + customSerializers(scope)).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")
    }


    protected open fun Member.ctorSubstitutedName(scope: Declaration) = when (this) {
        is Member.Reactive.Stateful.Extension -> ctorSimpleName + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.substitutedName(scope) } + customSerializers(scope)
        else -> implSubstitutedName(scope)
    }


    protected open val Member.isBindable: Boolean
        get() = when (this) {
            is Member.Field -> type is IBindable
            is Member.Reactive -> true

            else -> false
        }


    protected open val Member.publicName: String get() = name
    protected open val Member.encapsulatedName: String get() = isEncapsulated.condstr { "_" } + publicName
    open val Member.isEncapsulated: Boolean get() = this is Member.Reactive

    protected fun Member.ctorParam(containing: Declaration): String {
        val typeName = implSubstitutedName(containing)
        return StringBuilder().also {
            it.append("$typeName $encapsulatedName")
            it.appendDefaultValueSetter(this, typeName)
        }.toString()
    }

    protected fun Member.Reactive.customSerializers(scope: Declaration): List<String> {
        return genericParams.asList().map { it.serializerRef(scope) }
    }

    protected open val Member.hasEmptyConstructor: Boolean
        get() = when (this) {
            is Member.Field -> type.hasEmptyConstructor && !emptyCtorSuppressed
            is Member.Reactive -> true

            else -> fail("Unsupported member: $this")
        }
    //endregion

    //region Declaration.
    protected fun Declaration.sanitizedName(scope: Declaration): String {
        val needQualification = namespace != scope.namespace
        return needQualification.condstr { "$namespace." } + name
    }

    protected fun Declaration.scopeResolution(): String {
        return "$name::"
    }

    protected fun Declaration.bases(withMembers: Boolean): List<String> {
        val baseName = baseNames(withMembers)
        return if (this.base == null) {
            listOf("ISerializable" + withMembers.condstr { "()" }) + (baseName?.let { listOf(it) } ?: emptyList())
        } else {
            (baseName?.let { listOf(it) } ?: emptyList())
        }
//        return listOf("ISerializable" + withMembers.condstr { "()" }) + (baseName?.let { listOf(it) } ?: emptyList())
    }

    protected fun Declaration.baseNames(withMembers: Boolean): String? {
        return this.base?.let {
            it.sanitizedName(this) + withMembers.condstr {
                "(${it.allMembers.joinToString(", ") { member -> "std::move(${member.encapsulatedName})" }})"
            }
        } ?: (
                (if (this is Toplevel) "RdExtBase"
                else if (this is Class || this is Aggregate || this is Toplevel) "RdBindableBase"
//            else if (decl is Struct) p(" : IPrintable")
                else null)?.plus(withMembers.condstr { "()" }))
    }

    val Declaration.primaryCtorVisibility: String
        get() {
            val modifier =
                    when {
                        hasSetting(PublicCtors) -> "public"
                        isAbstract -> "protected"
                        hasSecondaryCtor -> "private"
                        isExtension -> "public"
                        this is Toplevel -> "private"
                        else -> "public"
                    } + ":"
            return modifier
        }

    private val Declaration.hasSecondaryCtor: Boolean get () = (this is Toplevel || this.isConcrete) && this.allMembers.any { it.hasEmptyConstructor }
    //endregion

    override fun generate(root: Root, clearFolderIfExists: Boolean, toplevels: List<Toplevel>) {
        prepareGenerationFolder(folder, clearFolderIfExists)

        val fileNames = arrayListOf<String>()

        toplevels.sortedBy { it.name }.forEach { tl ->
            val types = tl.declaredTypes + tl/* + unknowns(decl.declaredTypes)*/
            for (type in types) {
                listOf(false, true).forEach { isDefinition ->
                    type.fsPath(isDefinition).run {
                        fileNames.add(type.fsName(isDefinition))
                        bufferedWriter().use { writer ->
                            PrettyPrinter().apply {
                                eolKind = Eol.linux
                                step = 4

                                //actual generation

                                if (isDefinition) {
                                    if (type !is Enum) {
                                        source(type)
                                    }
                                } else {
                                    header(type, types.minus(type))
                                }

                                writer.write(toString())
                            }
                        }
                    }
                }

            }


        }

        File(folder, "CMakeLists.txt").bufferedWriter().use {
            it.write("cmake_minimum_required(VERSION 3.12)$eol")
            it.write("add_library(rd_model STATIC ${fileNames.joinToString(eol)})" + eol)
            it.write("include_directories(../rd_framework_cpp)\n" +
                    "include_directories(../rd_framework_cpp/src/main)\n" +
                    "include_directories(../rd_framework_cpp/src/main/base)\n" +
                    "include_directories(../rd_framework_cpp/src/main/base/ext)\n" +
                    "include_directories(../rd_framework_cpp/src/main/impl)\n" +
                    "include_directories(../rd_framework_cpp/src/main/serialization)\n" +
                    "include_directories(../rd_framework_cpp/src/main/task)\n" +
                    "target_link_libraries(rd_model rd_framework_cpp)\n")
        }
    }


    //region files
    fun PrettyPrinter.header(decl: Declaration, dependencies: List<Declaration>) {
        +"#ifndef ${decl.name}_H"
        +"#define ${decl.name}_H"
        println()

        includesDecl(decl)
        println()

        dependenciesDecl(decl)
        println()

        /*if (decl.isLibrary)
            libdef(decl, dependencies)
        else*/
        typedecl(decl, false)
        println()

        +"#endif // ${decl.name}_H"
    }

    fun PrettyPrinter.source(decl: Declaration) {
        +"""#include "${decl.name}.h""""

        println()
        /*if (decl.isLibrary)
                libdef(decl, dependencies)
            else*/
        typedef(decl)
    }
    //endregion

    //region declaration
    protected open fun PrettyPrinter.typedecl(decl: Declaration, isDefinition: Boolean) {
        if (decl.getSetting(Cpp17Generator.Intrinsic) != null) return

//        println()
//        println()

        if (decl.documentation != null || decl.ownMembers.any { !it.isEncapsulated && it.documentation != null }) {
            +"/**"
            if (decl.documentation != null) {
                +" * ${decl.documentation}"
            }
            for (member in decl.ownMembers.filter { !it.isEncapsulated && it.documentation != null }) {
                +" * @property ${member.name} ${member.documentation}"
            }
            +" */"
        }

        decl.getSetting(Attributes)?.forEach {
            +"@$it"
        }

        if (decl is Enum) {
            enum(decl)
            return
        }

        if (decl.isAbstract) comment("abstract")
        if (decl is Struct.Concrete && decl.base == null) comment("data")


        p("class ${decl.name} ")
        baseClassTraitDecl(decl)
        block("{", "};") {
            comment("fields")
            fieldsDecl(decl)
            comment("initializer")
            +"private:"
            initializerTraitDecl(decl)
            +";"
            comment("primary ctor")
            +(decl.primaryCtorVisibility)
            p("explicit ")
            primaryCtorTraitDecl(decl)
            +";"
            comment("default ctors and dtors")
            +"public:"
            defaultCtorsDtors(decl)
            comment("reader")
            +"public:"
            readerTraitDecl(decl)
            +";"
            comment("writer")
            +"public:"
            writerTraitDecl(decl)
            +";"

            comment("getters")
            +"public:"
            gettersTraitDecl(decl)

            comment("equals trait")
            +"public:"
            +equalsTraitDecl(decl).condstr { ";" }

            comment("hash code trait")
            hashCodeTraitDecl(decl)
            comment("pretty print")
//            prettyPrintTrait(decl)


            /*if (decl.isExtension) {
                extensionTraitDef(decl as Ext)
            }*/
        }

        comment("hash code trait")
        hashSpecialization(decl)
    }

    protected open fun PrettyPrinter.enum(decl: Enum) { //completed
        +"enum class ${decl.name}"
        block("{", "};") {
            indent {
                +decl.constants.joinToString(separator = ",${eolKind.value}") {
                    docComment(it.documentation) + it.name
                }
            }
        }
    }

    protected fun PrettyPrinter.primaryCtorParams(decl: Declaration) {
//        fun ctorParamAccessModifier(member: Member) = member.isEncapsulated.condstr { if (decl.isAbstract) "protected " else "private " }

        val own = decl.ownMembers.map {
            val attrs = it.getSetting(Cpp17Generator.Attributes)?.fold("") { acc, attr -> "$acc@$attr${eolKind.value}" }
            (attrs ?: "") + it.ctorParam(decl)
        }
        val base = decl.membersOfBaseClasses.map { it.ctorParam(decl) }

        p(own.asSequence().plus(base).plus(unknownMembers(decl)).joinToString(", "))
    }
    //endregion

    //region TraitDecl
    fun PrettyPrinter.includesDecl(tl: Declaration) {
        val standardHeaders = listOf(
                "iostream",
                "cstring",
                "cstdint",
                "vector",
                "type_traits",
                "optional",
                "utility"
        )
        +standardHeaders.joinToString(separator = eolKind.value, transform = { "#include <$it>" })

        println()

        val frameworkHeaders = listOf(
                //root
                "Buffer",
                "Identities",
                "MessageBroker",
                "Protocol",
                "RdId",
                //impl
                "RdList",
                "RdMap",
                "RdProperty",
                "RdSet",
                "RdSignal",
                "RName",
                //serialization
                "IMarshaller",
                "ISerializable",
                "Polymorphic",
                "SerializationCtx",
                "Serializers",
                //ext
                "RdExtBase",
                //task
                "RdCall",
                "RdEndpoint",
                "RdTask",
                "RdTaskResult",
                //gen
                "gen_util"
        )/*.subList(0, 0)*/

        +frameworkHeaders.joinToString(separator = eol) { s -> """#include "$s.h" """ }

//        tl.referencedTypes.plus(tl.declaredTypes.flatMap { it.referencedTypes })
//            .filterIsInstance(Declaration::class.java)
//            .map {
//                it.namespace
//            }
//            .filterNot { it == tl.namespace }
//            .distinct()
//            .printlnWithBlankLine { "import $it.*;" }
    }

    fun PrettyPrinter.dependenciesDecl(tl: Declaration) {
        /*dependencies.forEach {
            +"""#include "${it.name}.h""""
        }*/
        tl.referencedTypes
                .plus(tl.base ?: emptyList<IType>())
                .filterIsInstance(Declaration::class.java)
                .distinct()
                .map { it.name }
                .printlnWithBlankLine { """#include "$it.h" """ }
        tl.referencedTypes
                .plus(tl.base ?: emptyList<IType>())
                .filterIsInstance(NullableScalar::class.java)
                .distinct()
                .map { it.itemType.name }
                .printlnWithBlankLine { """#include "$it.h" """ }
    }

    fun PrettyPrinter.baseClassTraitDecl(decl: Declaration) {
        +decl.bases(false).map { "public $it" }.joinToString(separator = ", ", prefix = ": ")
    }

    fun PrettyPrinter.fieldsDecl(decl: Declaration) {
//        fun ctorParamAccessModifier(member: Member) = member.isEncapsulated.condstr { if (decl.isAbstract) "protected " else "private " }
        +"protected:"
        val own = decl.ownMembers.map {
            val attrs = it.getSetting(Cpp17Generator.Attributes)?.fold("") { acc, attr -> "$acc@$attr${carry()}" }
            (attrs ?: "") + it.ctorParam(decl)
        }

        +own.asSequence().plus(unknownMembers(decl)).joinToString(separator = "") { "$it${carry()}" }
    }

    fun PrettyPrinter.initializerTraitDecl(decl: Declaration) {
        p("void init()")
    }

    fun PrettyPrinter.primaryCtorTraitDecl(decl: Declaration) {
//        if (decl.ownMembers.isEmpty()) return false
        p(decl.name)
        p("(")
        primaryCtorParams(decl)
        p(")")
    }

    fun PrettyPrinter.defaultCtorsDtors(decl: Declaration) {
        val name = decl.name
        println()
        +"$name($name &&) = default;"
        println()
        +"$name& operator=($name &&) = default;"
        println()
        +"virtual ~$name() = default;"
    }

    fun PrettyPrinter.readerTraitDecl(decl: Declaration) {
        if (decl.isConcrete) {
            p("static ${decl.name} read(SerializationCtx const& ctx, Buffer const & buffer)")
        } else if (decl.isAbstract) {
            //todo read abstract
        }
    }

    fun PrettyPrinter.writerTraitDecl(decl: Declaration) {
        if (decl.isConcrete) {
            p("void write(SerializationCtx const& ctx, Buffer const& buffer) const override")
        } else {
            //todo ???
        }
    }

    fun PrettyPrinter.gettersTraitDecl(decl: Declaration) {
        for (member in decl.ownMembers) {
            p(docComment(member.documentation))
            +"${member.intfSubstitutedName(decl)} const & get_${member.publicName}() const;"
        }
    }

    fun PrettyPrinter.equalsTraitDecl(decl: Declaration): Boolean {
        if (decl.isAbstract || decl !is IScalar) return false

        p("friend bool operator==(const ${decl.name} &lhs, const ${decl.name} &rhs)")
        return true
    }

    fun PrettyPrinter.hashCodeTraitDecl(decl: Declaration) {
        if (decl !is IScalar) return
        if (decl.isAbstract) {
            p("virtual size_t hashCode() const = 0;")
        } else {
            if (decl.base == null) {
                p("size_t hashCode() const;")
            } else {
                p("size_t hashCode() const override;")
            }
        }
    }

    fun PrettyPrinter.hashSpecialization(decl: Declaration) {
        if (decl !is IScalar) return;
//        if (decl.isAbstract || decl !is IScalar) return

        fun IScalar.hc(v: String): String = when (this) {
            is IArray, is IImmutableList ->
                if (isPrimitivesArray) "contentHashCode($v)"
                else "contentDeepHashCode($v)"
            is INullable -> {
                "($v.has_value()) ? " + (itemType as IScalar).hc("$v.value()") + " : 0"
            }
            else -> "std::hash<${this.substitutedName(decl)}>()($v)"
        }

        block("namespace std {", "}") {
            block("template <> struct hash<${decl.name}> {", "};") {
                block("size_t operator()(const ${decl.name} & value) const {", "}") {
                    /*+"size_t __r = 0;"

                    decl.allMembers.println { m: Member ->
                        val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                        val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")
                        if (f.usedInEquals)
                            "__r = __r * 31 + (${t.hc("""value.get_${f.encapsulatedName}()""")});"
                        else
                            ""
                    }

                    +"return __r;"*/
                    +"return value.hashCode();"
                }
            }
        }
    }

    //endregion

    //region definition
    private fun PrettyPrinter.typedef(decl: Declaration) {
        comment("initializer")
        initializerTraitDef(decl)
        comment("primary ctor")
        primaryCtorTraitDef(decl)
        comment("reader")
        readerTraitDef(decl)
        comment("writer")
        writerTraitDef(decl)
        comment("getters")
        gettersTraitDef(decl)
        comment("equals trait")
        equalsTraitDef(decl)
        comment("hash code trait")
        hashCodeTraitDef(decl)
//        comment("pretty print")
//            prettyPrintTrait(decl)

    }

    /*protected open fun PrettyPrinter.libdef(decl: Toplevel, types: List<Declaration>) {
//        if (decl.getSetting(Cpp17Generator.Intrinsic) != null) return
        + "object ${decl.name} : ISerializersOwner {"
        indent {
            registerSerializersTrait(decl, types)
        }
        + "}"
    }*/

    fun PrettyPrinter.memberInitialize(decl: Declaration) {
        p(decl.bases(true).joinToString(separator = ", ", prefix = ": "))
        if (decl.ownMembers.isNotEmpty())
            p(", ")
        p(decl.ownMembers.asSequence().map {
            "${it.encapsulatedName}(std::move(${it.encapsulatedName}))"
        }.plus(unknownMemberNames(decl)).joinToString(", "))
    }

    fun PrettyPrinter.readerBodyTrait(decl: Declaration) {
        fun IType.reader(): String = when (this) {
            is Enum -> "buffer.readEnum<${substitutedName(decl)}>()"
            is InternedScalar -> "ctx.readInterned(buffer) { _, _ -> ${itemType.reader()} }"
            in PredefinedIntegrals -> "buffer.read_pod<${substitutedName(decl)}>()"
            is PredefinedType -> "buffer.read${name.capitalize()}()"
            is Declaration ->
                this.getSetting(Intrinsic)?.marshallerObjectFqn?.let { "$it.read(ctx, buffer)" }
                        ?: if (isAbstract)
                            "ctx.serializers->readPolymorphic<${substitutedName(decl)}>(ctx, buffer)"
                        else
                            "${substitutedName(decl)}::read(ctx, buffer)"
            is INullable -> {
                val lambda = lambda(null, "return ${itemType.reader()}")
                """buffer.readNullable<${itemType.substitutedName(decl)}>($lambda)"""
            }
            is IArray ->
                if (isPrimitivesArray) "buffer.readArray<${itemType.substitutedName(decl)}>()"//todo inner type as template
                else """buffer.readArray<${itemType.substitutedName(decl)}>(${lambda(null, "return ${itemType.reader()}")})"""
            is IImmutableList -> "buffer.readArray<${itemType.substitutedName(decl)}>(${lambda(null, "return ${itemType.reader()}")})"

            else -> fail("Unknown declaration: $decl")
        }

        fun Member.reader(): String = when (this) {
            is Member.Field -> type.reader()
            is Member.Reactive.Stateful.Extension -> "${ctorSubstitutedName(decl)}(${delegatedBy.reader()})"
            is Member.Reactive -> {
                val params = listOf("ctx", "buffer").joinToString(", ")
                "${implSubstitutedName(decl)}::read($params)"
            }
            else -> fail("Unknown member: $this")
        }

        fun Member.valName(): String = encapsulatedName.let { it + (it == "ctx" || it == "buffer").condstr { "_" } }

        val unknown = isUnknown(decl)
        if (unknown) {
            +"int32_t objectStartPosition = buffer.get_position();"
        }
        if (decl is Class && decl.isInternRoot) {
            +"auto ctx = ctx.withInternRootHere(false);"
        }
        if (decl is Class || decl is Aggregate) {
            +"auto _id = RdId::read(buffer);"
        }
        (decl.membersOfBaseClasses + decl.ownMembers).println { "auto ${it.valName()} = ${it.reader()};" }
        if (unknown) {
            +"auto unknownBytes = ByteArray(objectStartPosition + size - buffer.position);"
            +"buffer.readByteArrayRaw(unknownBytes);"
        }
        val ctorParams = decl.allMembers.asSequence().map { "std::move(${it.valName()})" }.plus(unknownMemberNames(decl)).joinToString(", ")
//        p("return ${decl.name}($ctorParams)${(decl is Class && decl.isInternRoot).condstr { ".apply { mySerializationContext = ctx }" }}")
        +"${decl.name} res = ${decl.name}(${ctorParams.isNotEmpty().condstr { ctorParams }});"
        if (decl is Class || decl is Aggregate) {
            +("withId(res, _id);")
        }
        +("return res;")
    }

    fun PrettyPrinter.lambda(args: String?, body: String): String {
        return "[&](${args ?: ""}) { $body; }"
    }
    //endregion

    //region TraitDef
    fun PrettyPrinter.primaryCtorTraitDef(decl: Declaration) {
//        if (decl.ownMembers.isEmpty()) return
        p(decl.scopeResolution())
        primaryCtorTraitDecl(decl)
        memberInitialize(decl)
        p(" { init(); }")
    }

    protected fun PrettyPrinter.readerTraitDef(decl: Declaration) {
        if (decl.isConcrete) {
            define(decl.name, "read(SerializationCtx const& ctx, Buffer const & buffer)", decl)
            block("{", "}") {
                indent {
                    if (isUnknown(decl)) {
                        +"throw std::exception(\"Unknown instances should not be read via serializer\")"
                    } else {
                        readerBodyTrait(decl)
                    }
                }
            }
        } else {
            //todo ???
        }
    }

    protected fun PrettyPrinter.writerTraitDef(decl: Declaration) {
        fun IType.writer(field: String): String {
            return when (this) {
                is Enum -> "buffer.writeEnum($field)"
                is InternedScalar -> "ctx.writeInterned(buffer, $field) { _, _, internedValue -> ${itemType.writer("internedValue")} }"
                is PredefinedType.string -> "buffer.writeString($field)"
                is PredefinedType -> "buffer.write_pod($field)"
                is Declaration ->
                    this.getSetting(Intrinsic)?.marshallerObjectFqn?.let { "$it.write(ctx,buffer, $field)" }
                            ?: if (isAbstract) "ctx.serializers->writePolymorphic(ctx, buffer, $field)"//todo template type
                            else "$field.write(ctx, buffer)"
                is INullable -> {
                    val lambda = lambda("${itemType.substitutedName(decl)} const & it", itemType.writer("it"))
                    "buffer.writeNullable<${itemType.substitutedName(decl)}>($field, $lambda)"
                }
                is IArray ->
                    if (isPrimitivesArray) "buffer.writeArray($field)"
                    else {
                        val lambda = lambda("${itemType.substitutedName(decl)} const & it", itemType.writer("it"))
                        "buffer.writeArray<${itemType.substitutedName(decl)}>($field, $lambda)"
                    }
                is IImmutableList -> {
                    val lambda = lambda("${itemType.substitutedName(decl)} const & it", itemType.writer("it"))
                    "buffer.writeArray<${itemType.substitutedName(decl)}>($field, $lambda)"
                }

                else -> fail("Unknown declaration: $decl")
            }
        }


        fun Member.writer(): String = when (this) {
            is Member.Field -> type.writer(encapsulatedName)
            is Member.Reactive.Stateful.Extension -> delegatedBy.writer(("$encapsulatedName.delegatedBy"))//todo
            is Member.Reactive -> "$encapsulatedName.write(ctx, buffer)"

            else -> fail("Unknown member: $this")
        }

        if (decl.isConcrete) {
            define("void", "write(SerializationCtx const& ctx, Buffer const& buffer) const", decl)
            block("{", "}") {
                indent {
                    if (decl is Class && decl.isInternRoot) {
                        +"val ctx = ctx.withInternRootHere(true);"
                        +"this->mySerializationContext = ctx;"
                    }
                    if (decl is Class || decl is Aggregate) {
                        +"this->rdid.write(buffer);"
                    }
                    (decl.membersOfBaseClasses + decl.ownMembers).println { member -> member.writer() + ";" }
                    if (isUnknown(decl)) {
                        +"buffer.writeByteArrayRaw(value.unknownBytes);"
                    }
                }
            }
        } else {
            //todo ???
        }
    }

    protected fun PrettyPrinter.gettersTraitDef(decl: Declaration) {
        for (member in decl.ownMembers/*.filter { it.isEncapsulated }*/) {
            p(docComment(member.documentation))
            define("${member.intfSubstitutedName(decl)} const &", "get_${member.publicName}() const", decl)
            +" { return ${member.encapsulatedName}; }"
        }

        /*if (decl is Class && decl.isInternRoot) {
            +"private var mySerializationContext: SerializationCtx? = null"
            +"override val serializationContext: SerializationCtx"
            indent {
                +"get() = mySerializationContext ?: throw IllegalStateException(\"Attempting to get serialization context too soon for \$location\")"
            }
        }*/
    }

    protected fun PrettyPrinter.initializerTraitDef(decl: Declaration) {
        define("void", "init()", decl)

        block("{", "}") {
            indent {
                decl.ownMembers
                        .filterIsInstance<Member.Reactive.Stateful>()
                        .filter { it !is Member.Reactive.Stateful.Extension && it.genericParams.none { it is IBindable } }
                        .println { "${it.encapsulatedName}.optimizeNested = true;" }

                if (flowTransform == FlowTransform.Reversed) {
                    decl.ownMembers
                            .filterIsInstance<Member.Reactive.Stateful.Map>()
                            .println { "${it.encapsulatedName}.master = false;" }
                }

                decl.ownMembers
                        .filterIsInstance<Member.Reactive>()
                        .filter { it.freeThreaded }
                        .println { "${it.encapsulatedName}.async = true;" }

                decl.ownMembers
                        .filter { it.isBindable }
                        .println { """//bindableChildren.emplace("${it.name}", ${it.encapsulatedName});""" }
            }
        }
    }

    protected fun PrettyPrinter.equalsTraitDef(decl: Declaration) {
        if (decl.isAbstract || decl !is IScalar) return

//        equalsTraitDecl(decl)
        p("bool operator==(const ${decl.name} &lhs, const ${decl.name} &rhs)")
        block("{", "}") {
            indent {
                +"if (&lhs == &rhs) return true;"

                decl.allMembers.println { m ->
                    val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                    val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")

                    if (f.usedInEquals)
//                    "if (${t.eq(f.encapsulatedName)}) return false"
                        "if (lhs.${f.encapsulatedName} != rhs.${f.encapsulatedName}) return false;"
                    else
                        ""
                }
                println()
                +"return true;"
            }
        }
    }

    protected fun PrettyPrinter.hashCodeTraitDef(decl: Declaration) {
        fun IScalar.hc(v: String): String = when (this) {
            is IArray, is IImmutableList ->
                if (isPrimitivesArray) "contentHashCode($v)"
                else "contentDeepHashCode($v)"
            is INullable -> {
                "($v.has_value()) ? " + (itemType as IScalar).hc("$v.value()") + " : 0"
            }
            else -> "std::hash<${this.substitutedName(decl)}>()($v)"
        }

        if (decl.isAbstract || decl !is IScalar) return

        define("size_t", "hashCode() const", decl)
        block("{", "}") {
            +"size_t __r = 0;"

            decl.allMembers.println { m: Member ->
                val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")
                if (f.usedInEquals)
                    "__r = __r * 31 + (${t.hc("""get_${f.encapsulatedName}()""")});"
                else
                    ""
            }

            +"return __r;"
        }
    }

    protected fun PrettyPrinter.extensionTraitDef(decl: Ext) {//todo
        val pointcut = decl.pointcut ?: return
        val lowerName = decl.name.decapitalize()
        val extName = decl.extName ?: lowerName
        +"""const & ${pointcut.sanitizedName(decl)}.$extName get_${pointcut.sanitizedName(decl)}.$extName get() = getOrCreateExtension("$lowerName", ::${decl.name})"""
        println()
    }
    //endregion

    //region unknowns
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
    //endregion

    private fun docComment(doc: String?) = (doc != null).condstr {
        "\n" +
                "/**\n" +
                " * $doc\n" +
                " */\n"
    }

    //only for toplevel Exts
    protected fun PrettyPrinter.createMethodTrait(decl: Toplevel) {
        if (decl.isExtension) return

        +"static ${decl.name} create(lifetime: Lifetime, protocol: IProtocol)  {"
        indent {
            +"${decl.root.sanitizedName(decl)}.register(protocol.serializers)"
            println()

            +"${decl.name} res;"
            val quotedName = """"${decl.name}""""
            +"res.identify(protocol.identity, RdId::Null().mix($quotedName));"
            +"res.bind(lifetime, protocol, $quotedName)"
            +"return res;"
        }
        +"}"
    }

    private fun getDefaultValue(containing: Declaration, member: Member): String? =
            if (member is Member.Reactive.Stateful.Property) {
                when {
                    member.defaultValue is String -> "\"" + member.defaultValue + "\""
                    member.defaultValue != null -> member.defaultValue.toString()
                    member.isNullable -> "null"
                    else -> null
                }
            } else if (member is Member.Reactive.Stateful.Extension)
                member.delegatedBy.sanitizedName(containing) + "()"
            else
                null


    override fun toString(): String {
        return "Cpp17Generator(flowTransform=$flowTransform, defaultNamespace='$defaultNamespace', folder=${folder.canonicalPath})"
    }

    val PredefinedIntegrals = listOf(
            PredefinedType.byte,
            PredefinedType.short,
            PredefinedType.int,
            PredefinedType.long,
            PredefinedType.float,
            PredefinedType.double,
            PredefinedType.char,
            PredefinedType.bool
    )
}


