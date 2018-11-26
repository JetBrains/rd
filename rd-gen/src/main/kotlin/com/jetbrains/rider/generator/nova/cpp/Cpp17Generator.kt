package com.jetbrains.rider.generator.nova.cpp

import com.jetbrains.rider.generator.nova.*
import com.jetbrains.rider.generator.nova.Enum
import com.jetbrains.rider.generator.nova.FlowKind.*
import com.jetbrains.rider.generator.nova.util.joinToOptString
import com.jetbrains.rider.util.eol
import com.jetbrains.rider.util.hash.IncrementalHash64
import com.jetbrains.rider.util.string.Eol
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.string.condstr
import java.io.File

class Signature(val returnType: String, val arguments: String, val scope: String) {
    private var declPrefix = arrayListOf<String>()
    private var declPostfix = arrayListOf<String>()
    private var commonPostfix = arrayListOf<String>()

    private fun <T> ArrayList<T>.front(): String {
        return toArray().joinToOptString(separator = " ", postfix = " ")
    }

    private fun <T> ArrayList<T>.back(): String {
        return toArray().joinToOptString(separator = " ", prefix = " ")
    }

    fun decl(): String {
        return "${declPrefix.front()}$returnType $arguments${commonPostfix.back()}${declPostfix.back()};"
    }

    fun def(): String {
        return "$returnType $scope::$arguments${commonPostfix.back()}"
    }

    fun const(): Signature {
        return this.also {
            commonPostfix.add("const")
        }
    }

    fun override(): Signature {
        return this.also {
            declPostfix.add("override")
        }
    }

    fun static(): Signature {
        return this.also {
            declPrefix.add("static")
        }
    }
}


private fun PrettyPrinter.declare(signature: Signature?) {
    signature?.let {
        this.println(it.decl())
    }
}

private fun PrettyPrinter.def(signature: Signature?) {
    signature?.let {
        this.println(it.def())
    }
}

fun StringBuilder.appendDefaultInitialize(member: Member, typeName: String) {
    if (member is Member.Field && (member.isOptional || member.defaultValue != null)) {
        append("{")
        val defaultValue = member.defaultValue
        when (defaultValue) {
            is String -> append(if (member.type is Enum) "$typeName::$defaultValue" else "\"$defaultValue\"")
            is Long, is Boolean -> append(defaultValue)
            else -> if (member.isOptional) append("tl::nullopt")
        }
        append("}")
    }
}

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

    fun PrettyPrinter.titledBlock(title: String, body: PrettyPrinter.() -> Unit) {
        +"$title {"
        indent(body)
        +"}"
    }

    fun PrettyPrinter.comment(comment: String) {
        +"${eolKind.value}//$comment"
    }

    fun PrettyPrinter.define(returnType: String, signature: String, decl: Declaration) {
        p("$returnType ${decl.scopeResolution()}$signature ")
    }
    //endregion

    //region IType.
    protected open fun IType.substitutedName(scope: Declaration): String = when (this) {
        is Declaration -> (namespace != scope.namespace).condstr { "$namespace." } + name
        is INullable -> "tl::optional<${itemType.substitutedName(scope)}>"
        is InternedScalar -> itemType.substitutedName(scope)
        is IArray -> "std::vector<${itemType.substitutedName(scope)}>"
        is IImmutableList -> "std::vector<${itemType.substitutedName(scope)}>"

        is PredefinedType.byte -> "signed char"
        is PredefinedType.char -> "wchar_t"
        is PredefinedType.int -> "int32_t"
        is PredefinedType.long -> "int64_t"
        is PredefinedType.string -> "std::wstring"
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

    protected fun IType.serializerRef(scope: Declaration): String = leafSerializerRef(scope)
            ?: "${scope.name}::__${name}Serializer"

    protected fun IType.serializerDef(scope: Declaration): String = "__${name}Serializer"

    //endregion

    //region Member.
    val Member.Reactive.actualFlow: FlowKind get() = flowTransform.transform(flow)

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open val Member.Reactive.implSimpleName: String
        get () = when (this) {
            is Member.Reactive.Task -> when (actualFlow) {
                Sink -> "RdEndpoint"
                Source -> "RdCall"
                Both -> "RdCall"
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

    protected fun Member.ctorParam(containing: Declaration, withSetter: Boolean): String {
        val typeName = implSubstitutedName(containing)
        return StringBuilder().also {
            it.append("$typeName $encapsulatedName")
            if (withSetter) {
                it.appendDefaultInitialize(this, typeName)
            }
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
            val result = arrayListOf<String>()
            if (this !is Toplevel) {
                result.add("ISerializable" + withMembers.condstr { "()" })
            }
            baseName?.let { result.add(it) }
            result
        } else {
            val result = listOfNotNull(baseName).toMutableList()
            if (isUnknown(this)) {
                result.add("IUnknownInstance" + withMembers.condstr { "(std::move(unknownId))" })
            }
            result
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
            val types = tl.declaredTypes + tl + unknowns(tl.declaredTypes)
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
                                        source(type, types)
                                    }
                                } else {
                                    header(type)
                                }

                                writer.write(toString())
                            }
                        }
                    }
                }

            }


        }

        File(folder, "CMakeLists.txt").bufferedWriter().use {
            it.write("cmake_minimum_required(VERSION 3.10)$eol")
            it.write("add_library(rd_model STATIC ${fileNames.joinToString(eol)})" + eol)
            it.write("target_link_libraries(rd_model rd_framework_cpp)\n")
        }
    }


    //region files
    fun PrettyPrinter.header(decl: Declaration) {
        +"#ifndef ${decl.name}_H"
        +"#define ${decl.name}_H"
        println()

        includesDecl(decl)
        println()

        dependenciesDecl(decl)
        println()

        if (decl is Toplevel && decl.isLibrary) {
            comment("library")
            libdecl(decl)
        } else {
            typedecl(decl)
        }
        println()

        +"#endif // ${decl.name}_H"
    }

    fun PrettyPrinter.source(decl: Declaration, dependencies: List<Declaration>) {
        +"""#include "${decl.name}.h""""

        println()
        if (decl is Toplevel) {
            dependencies.filter { !it.isAbstract }.filterIsInstance<IType>().println {
                """#include "${it.name}.h""""
            }
        }
        println()
        if (decl is Ext) {
            println("""#include "${decl.root.sanitizedName(decl)}.h"""")
        }
        if (decl is Toplevel && decl.isLibrary) {
            libdef(decl, dependencies)
        } else {
            typedef(decl)
        }
    }
    //endregion

    //region declaration
    protected open fun PrettyPrinter.libdecl(decl: Declaration) {
        if (decl.getSetting(Cpp17Generator.Intrinsic) != null) return
        block("class ${decl.name} {", "};") {
            registerSerializersTraitDecl(decl)
        }
    }

    protected open fun PrettyPrinter.typedecl(decl: Declaration) {
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
            comment("companion")
            companionTraitDecl(decl)

            if (decl.isExtension) {
                comment("extension")
                declare(extensionTraitDecl(decl as Ext))
            }

            comment("custom serializers")
            customSerializersTrait(decl)

            comment("fields")
            fieldsDecl(decl)

            comment("initializer")
            +"private:"
            declare(initializerTraitDecl(decl))

            comment("primary ctor")
//            +(decl.primaryCtorVisibility)
            +"public:"
            p("explicit ")
            primaryCtorTraitDecl(decl)
            +";"

            comment("default ctors and dtors")
            defaultCtorsDtors(decl)

            comment("reader")
            +"public:"
            declare(readerTraitDecl(decl))

            comment("writer")
            +"public:"
            declare(writerTraitDecl(decl))

            comment("getters")
            gettersTraitDecl(decl)

            comment("equals trait")
            +"public:"
            declare(equalsTraitDecl(decl))

            comment("equality operators")
            equalityOperatorsDecl(decl)

            comment("hash code trait")
            hashCodeTraitDecl(decl)
//            comment("pretty print")
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
            +decl.constants.joinToString(separator = ",${eolKind.value}") {
                docComment(it.documentation) + it.name
            }
        }
    }

    protected fun PrettyPrinter.primaryCtorParams(decl: Declaration): String {
        val own = decl.ownMembers.map {
            val attrs = it.getSetting(Cpp17Generator.Attributes)?.fold("") { acc, attr -> "$acc@$attr${eolKind.value}" }
            (attrs ?: "") + it.ctorParam(decl, false)
        }
        val base = decl.membersOfBaseClasses.map { it.ctorParam(decl, false) }

        return own.asSequence().plus(base).plus(unknownMembers(decl)).joinToString(", ")
    }
    //endregion

    //region TraitDecl
    fun PrettyPrinter.includesDecl(decl: Declaration) {
//        +"class ${decl.name};"

        val standardHeaders = listOf(
                "iostream",
                "cstring",
                "cstdint",
                "vector",
                "type_traits",
                "optional",
                "utility"
        )


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
                "NullableSerializer",
                "ArraySerializer",
                "SerializationCtx",
                "Serializers",
                "ISerializersOwner",
                "IUnknownInstance",
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
        println()
        +standardHeaders.joinToString(separator = eolKind.value, transform = { "#include <$it>" })
    }

    fun PrettyPrinter.dependenciesDecl(decl: Declaration) {
        fun parseType(type: IType): ArrayList<String> {
            return when (type) {
                is IArray -> {
                    parseType(type.itemType)
                }
                is IImmutableList -> {
                    parseType(type.itemType)
                }
                is INullable -> {
                    parseType(type.itemType)
                }
                is InternedScalar -> {
                    parseType(type.itemType)
                }
                !is PredefinedType -> {
                    arrayListOf(type.name)
                }
                else -> {
                    arrayListOf()
                }
            }
        }

        fun parseMember(member: Member): ArrayList<String> {
            return when (member) {
                is Member.EnumConst -> {
                    arrayListOf()
                }
                is Member.Field -> {
                    parseType(member.type)
                }
                is Member.Reactive -> {
                    if (member is Member.Reactive.Stateful.Extension) {
                        arrayListOf(member.implSimpleName)
                    } else {
                        member.genericParams.fold(arrayListOf()) { acc, iType ->
                            acc += parseType(iType)
                            acc
                        }
                    }
                }
            }
        }

        val extHeader = listOfNotNull(if (decl.isExtension) decl.pointcut?.name else null)
        decl.ownMembers.map { parseMember(it) }.fold(arrayListOf<String>()) { acc, arrayList ->
            acc += arrayList
            acc
        }.plus(listOfNotNull(decl.base?.name)).plus(extHeader)
//                .filter { dependencies.map { it.name }.contains(it) }
                .distinct()
                .printlnWithBlankLine { """#include "$it.h"""" }
    }

    fun PrettyPrinter.baseClassTraitDecl(decl: Declaration) {
        +decl.bases(false).joinToString(separator = ", ", prefix = ": ") { "public $it" }
    }


    private fun PrettyPrinter.createMethodTraitDecl(decl: Toplevel) {
        if (decl.isExtension) return
        +"static ${decl.name} create(Lifetime lifetime, IProtocol * protocol);"
    }

    fun PrettyPrinter.customSerializersTrait(decl: Declaration) {
        fun IType.serializerBuilder(): String = leafSerializerRef(decl) ?: when (this) {
            is IArray -> "ArraySerializer<${itemType.serializerBuilder()}>"
            is IImmutableList -> "ArraySerializer<${itemType.serializerBuilder()}>"
            is INullable -> "NullableSerializer<${itemType.serializerBuilder()}>"
            is InternedScalar -> "InternedSerializer<${itemType.serializerBuilder()}>"
            else -> fail("Unknown type: $this")
        }

        +"private:"
        val allTypesForDelegation = decl.allMembers
                .filterIsInstance<Member.Reactive>()
                .flatMap { it.genericParams.toList() }
                .distinct()
                .filter { it.leafSerializerRef(decl) == null }

        allTypesForDelegation.println { "using ${it.serializerDef(decl)} = ${it.serializerBuilder()};" }
    }


    private fun PrettyPrinter.registerSerializersTraitDecl(decl: Declaration) {
        val serializersOwnerImplName = "${decl.name}SerializersOwner"
        +"public:"
        block("struct $serializersOwnerImplName : public ISerializersOwner {", "};") {
            +"void registerSerializersCore(Serializers const& serializers) override;"
        }
        println()
        +"static $serializersOwnerImplName serializersOwner;"
        println()
    }

    private fun PrettyPrinter.companionTraitDecl(decl: Declaration) {
        if (decl.isAbstract) {
            println()
//            abstractDeclarationTrait(decl)
            customSerializersTrait(decl)
        }
        if (decl is Toplevel) {
            println()
            registerSerializersTraitDecl(decl)
            println()
            +"public:"
            createMethodTraitDecl(decl)
            println()
        }
    }


    private fun PrettyPrinter.extensionTraitDecl(decl: Ext): Signature? {
        val pointcut = decl.pointcut ?: return null
        val lowerName = decl.name.decapitalize()
        val extName = decl.extName ?: lowerName
        return Signature("void", "getOrCreateExtensionOf(${pointcut.sanitizedName(decl)} & pointcut)", decl.name).static()
    }

    fun PrettyPrinter.fieldsDecl(decl: Declaration) {
        +"protected:"
        val own = decl.ownMembers.map {
            val initial = getDefaultValue(decl, it)?.let {
                "{$it}"
            } ?: ""
            "${it.ctorParam(decl, true)}$initial"
        }

        +own.asSequence().plus(unknownMembers(decl)).joinToString(separator = "") { "$it${carry()}" }
    }

    fun PrettyPrinter.initializerTraitDecl(decl: Declaration): Signature {
        return Signature("void", "init()", decl.name)
    }

    fun PrettyPrinter.primaryCtorTraitDecl(decl: Declaration) {
//        if (decl.ownMembers.isEmpty()) return false
        p("${decl.name}(${primaryCtorParams(decl)})")
    }

    fun PrettyPrinter.defaultCtorsDtors(decl: Declaration) {
        +"public:"
        val name = decl.name
        println()
        if (primaryCtorParams(decl).isNotEmpty()) {
            titledBlock("$name()") {
                +"init();"
            }
        }
        if (decl is IScalar) {
            println()
            +"$name($name const &) = default;"
            println()
            +"$name& operator=($name const &) = default;"
        }
        println()
        +"$name($name &&) = default;"
        println()
        +"$name& operator=($name &&) = default;"
        println()
        +"virtual ~$name() = default;"
    }

    fun readerTraitDecl(decl: Declaration): Signature? {
        if (decl.isConcrete) {
            return Signature(decl.name, "read(SerializationCtx const& ctx, Buffer const & buffer)", decl.name).static()
        } else if (decl.isAbstract) {
            //todo read abstract
            return null
        }
        return null
    }

    fun writerTraitDecl(decl: Declaration): Signature? {
        if (decl.isConcrete) {
            return Signature("void", "write(SerializationCtx const& ctx, Buffer const& buffer)", decl.name).const().override()
        } else {
            //todo ???
            return null
        }
    }

    fun PrettyPrinter.gettersTraitDecl(decl: Declaration) {
        +"public:"
        for (member in decl.ownMembers) {
            p(docComment(member.documentation))
            +"${member.intfSubstitutedName(decl)} const & get_${member.publicName}() const;"
        }
    }

    private fun equalsTraitDecl(decl: Declaration): Signature {
        return Signature("bool", "equals(${decl.name} const& other)", decl.name).const()
    }

    fun PrettyPrinter.equalityOperatorsDecl(decl: Declaration) {
//        if (decl.isAbstract || decl !is IScalar) return

        +"public:"
        +("friend bool operator==(const ${decl.name} &lhs, const ${decl.name} &rhs);")
        +("friend bool operator!=(const ${decl.name} &lhs, const ${decl.name} &rhs);")
    }

    fun PrettyPrinter.hashCodeTraitDecl(decl: Declaration) {
        if (decl !is IScalar) return

        +"public:"
        if (decl.isAbstract) {
//            +("virtual size_t hashCode() const = 0;")
            +("virtual size_t hashCode() const;")
        } else {
            if (decl.base == null) {
                +("size_t hashCode() const;")
            } else {
                +("size_t hashCode() const;")
            }
        }
    }

    fun PrettyPrinter.hashSpecialization(decl: Declaration) {
        if (decl !is IScalar) return

        block("namespace std {", "}") {
            block("template <> struct hash<${decl.name}> {", "};") {
                block("size_t operator()(const ${decl.name} & value) const {", "}") {
                    +"return value.hashCode();"
                }
            }
        }
    }

    //endregion

    //region definition
    protected open fun PrettyPrinter.libdef(decl: Toplevel, types: List<Declaration>) {
        if (decl.getSetting(Cpp17Generator.Intrinsic) != null) return
        registerSerializersTraitDef(decl, types)
    }

    private fun PrettyPrinter.typedef(decl: Declaration) {
        comment("companion")
        companionTraitDef(decl)

        if (decl.isExtension) {
            comment("extension")
            extensionTraitDef(decl as Ext)
        }

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

        comment("equality operators")
        equalityOperatorsDef(decl)

        comment("hash code trait")
        hashCodeTraitDef(decl)
//        comment("pretty print")
//            prettyPrintTrait(decl)

    }

    fun PrettyPrinter.memberInitialize(decl: Declaration) {
        var result = decl.bases(true)
        result += decl.ownMembers.asSequence().map {
            "${it.encapsulatedName}(std::move(${it.encapsulatedName}))"
        }
        if (isUnknown(decl)) {
            result += ("unknownBytes(std::move(unknownBytes))")
        }
        p(result.joinToString(separator = ", ", prefix = ": "))
    }

    fun PrettyPrinter.readerBodyTrait(decl: Declaration) {
        fun IType.reader(): String = when (this) {
            is Enum -> "buffer.readEnum<${substitutedName(decl)}>()"
            is InternedScalar -> {
                val lambda = lambda("SerializationCtx const &, Buffer const &", "return ${itemType.reader()}")
                "ctx.readInterned<${itemType.substitutedName(decl)}>(buffer, $lambda)"
            }
            in PredefinedIntegrals -> "buffer.read_pod<${substitutedName(decl)}>()"
            is PredefinedType.string -> "buffer.readWString()"
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

    fun lambda(args: String?, body: String): String {
        return "[&](${args ?: ""}) { $body; }"
    }
    //endregion

    //region TraitDef

    private fun PrettyPrinter.registerSerializersTraitDef(decl: Toplevel, types: List<Declaration>) {//todo name access
        val serializersOwnerImplName = "${decl.name}SerializersOwner"
        +"${decl.name}::$serializersOwnerImplName ${decl.name}::serializersOwner;"
        println()
        +"void ${decl.name}::${decl.name}SerializersOwner::registerSerializersCore(Serializers const& serializers)"
        block("{", "}") {
            indent {
                types.filter { !it.isAbstract }.filterIsInstance<IType>().filterNot { iType -> iType is Enum }.println {
                    "serializers.registry<${it.name}>();"
                }

                if (decl is Root) {
                    //decl.toplevels.println { it.sanitizedName(decl) + "::serializersOwner.registry(serializers);" }
                    //todo mark graph vertex
                }
            }
        }
    }

    //only for toplevel Exts
    protected fun PrettyPrinter.createMethodTraitDef(decl: Toplevel) {
        if (decl.isExtension) return

        define(decl.name, "create(Lifetime lifetime, IProtocol * protocol)", decl)
        block("{", "}") {
            +"${decl.root.sanitizedName(decl)}::serializersOwner.registry(protocol->serializers);"
            println()

            +"${decl.name} res;"
            val quotedName = """"${decl.name}""""
            +"res.identify(*(protocol->identity), RdId::Null().mix($quotedName));"
            +"res.bind(lifetime, protocol, $quotedName);"
            +"return res;"
        }
    }

    private fun PrettyPrinter.companionTraitDef(decl: Declaration) {
        if (decl is Toplevel) {
            println()
            registerSerializersTraitDef(decl, decl.declaredTypes + unknowns(decl.declaredTypes))
            println()
            createMethodTraitDef(decl)

            println()
        }
    }


    fun PrettyPrinter.primaryCtorTraitDef(decl: Declaration) {
//        if (decl.ownMembers.isEmpty()) return
        p(decl.scopeResolution())
        primaryCtorTraitDecl(decl)
        memberInitialize(decl)
        p(" { init(); }")
    }

    protected fun PrettyPrinter.readerTraitDef(decl: Declaration) {
        def(readerTraitDecl(decl))
        if (decl.isConcrete) {
            block("{", "}") {
                if (isUnknown(decl)) {
                    +"throw std::logic_error(\"Unknown instances should not be read via serializer\");"
                } else {
                    readerBodyTrait(decl)
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
                is InternedScalar -> {
                    val lambda = lambda("SerializationCtx const &, Buffer const &, ${itemType.substitutedName(decl)} const & internedValue", itemType.writer("internedValue"))
                    "ctx.writeInterned<${itemType.substitutedName(decl)}>(buffer, $field, $lambda)"
                }
                is PredefinedType.string -> "buffer.writeWString($field)"
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
            def(writerTraitDecl(decl))
            block("{", "}") {
                if (decl is Class && decl.isInternRoot) {
                    +"val ctx = ctx.withInternRootHere(true);"
                    +"this->mySerializationContext = ctx;"
                }
                if (decl is Class || decl is Aggregate) {
                    +"this->rdid.write(buffer);"
                }
                (decl.membersOfBaseClasses + decl.ownMembers).println { member -> member.writer() + ";" }
                if (isUnknown(decl)) {
                    +"buffer.writeByteArrayRaw(unknownBytes);"
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
        def(initializerTraitDecl(decl))

        block("{", "}") {
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
                    .println { """bindableChildren.emplace_back("${it.name}", &${it.encapsulatedName});""" }

            if (decl is Toplevel) {
                +"serializationHash = ${decl.serializationHash(IncrementalHash64()).result}L;"
            }
        }
    }


    private fun PrettyPrinter.equalsTraitDef(decl: Declaration) {
        def(equalsTraitDecl(decl))
        if (decl.isAbstract || decl !is IScalar) {
            block("{", "}") {
                +"return this == &other;"
            }
        } else {
            block("{", "}") {
                +"if (this == &other) return true;"

                decl.allMembers.println { m ->
                    val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                    val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")

                    if (f.usedInEquals)
                    //                    "if (${t.eq(f.encapsulatedName)}) return false"
                        "if (this->${f.encapsulatedName} != other.${f.encapsulatedName}) return false;"
                    else
                        ""
                }
                println()
                +"return true;"
            }
        }
    }

    protected fun PrettyPrinter.equalityOperatorsDef(decl: Declaration) {
        p("bool operator==(const ${decl.name} &lhs, const ${decl.name} &rhs)")
        if (decl.isAbstract || decl !is IScalar) {
            block("{", "}") {
                +"return &lhs == &rhs;"
            }
        } else {
            block("{", "}") {
                +"return lhs.equals(rhs);"
            }

        }

        p("bool operator!=(const ${decl.name} &lhs, const ${decl.name} &rhs)")
        block("{", "}") {
            +"return !(lhs == rhs);"
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
        def(extensionTraitDecl(decl))
        val lowerName = decl.name.decapitalize()
        block("{", "}") {
            +"""pointcut.getOrCreateExtension<${decl.name}>("$lowerName", []() { return ${decl.name}(); });"""
        }
        println()
    }
    //endregion

    //region unknowns
    private fun isUnknown(decl: Declaration) =
            decl is Class.Concrete && decl.isUnknown ||
                    decl is Struct.Concrete && decl.isUnknown

    private fun unknownMembers(decl: Declaration) =
            if (isUnknown(decl)) arrayOf("RdId unknownId",
                    "Buffer::ByteArray unknownBytes")
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

    private fun getDefaultValue(containing: Declaration, member: Member): String? =
            if (member is Member.Reactive.Stateful.Property) {
                when {
                    member.defaultValue is String -> """L"${member.defaultValue}""""
                    member.defaultValue != null -> member.defaultValue.toString()
                    member.isNullable -> "tl::nullopt"
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

