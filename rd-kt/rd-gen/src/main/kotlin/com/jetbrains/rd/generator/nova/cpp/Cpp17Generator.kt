package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.Enum
import com.jetbrains.rd.generator.nova.FlowKind.*
import com.jetbrains.rd.generator.nova.cpp.CppSanitizer.sanitize
import com.jetbrains.rd.generator.nova.cpp.Signature.Constructor
import com.jetbrains.rd.generator.nova.cpp.Signature.MemberFunction
import com.jetbrains.rd.generator.nova.util.capitalizeInvariant
import com.jetbrains.rd.generator.nova.util.decapitalizeInvariant
import com.jetbrains.rd.generator.nova.util.joinToOptString
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.eol
import com.jetbrains.rd.util.hash.IncrementalHash64
import com.jetbrains.rd.util.string.Eol
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr
import com.jetbrains.rd.util.warn
import java.io.File

private fun StringBuilder.appendDefaultInitialize(member: Member, typeName: String) {
    if (member is Member.Field && (member.isOptional || member.defaultValue != null)) {
        append("{")
        when (val defaultValue = member.defaultValue) {
            is String -> append(if (member.type is Enum) "$typeName::$defaultValue" else "L\"$defaultValue\"")
            is Long, is Boolean -> append(defaultValue)
//            else -> if (member.isOptional) append("nullopt")
        }
        append("}")
    }
}

// please set VsWarningsDefault to null if you don't need disabling VS warnings
@Suppress("RedundantNullableReturnType")
// val VsWarningsDefault: IntArray? = null
val VsWarningsDefault: IntArray? = intArrayOf(4250, 4307, 4267, 4244, 4100)

/**
 * Generate C++ code.
 * @param defaultNamespace namespace separated by symbol "point", which will be translated to nested namespaces. "a.b.c" to "a::b::c", for instance.
 * Remember about following properties: "FsPath", "TargetName"!
 */
open class Cpp17Generator(
    flowTransform: FlowTransform,
    val defaultNamespace: String,
    override val folder: File,
    generatedFileSuffix: String = ".Generated"
) : GeneratorBase(flowTransform, generatedFileSuffix) {

    @Suppress("ConstPropertyName")
    companion object {
        private const val polymorphicHeader = "serialization/Polymorphic"

        @Suppress("unused")
        object LanguageVersion {
            const val `C++11` = "201103L"
            const val `C++14` = "201402L"
            const val `C++17` = "201703L"
            const val `C++20` = "202000L"
        }

        object Features {
            const val __cpp_structured_bindings = "__cpp_structured_bindings"
        }

        object Files {
            const val PrecompiledHeaderCmake = """PrecompiledHeader.cmake"""
        }

        private const val msvcCheckMacro = "_MSC_VER"
    }

    //region language specific properties
    object Namespace : ISetting<String, Declaration>

    private val Declaration.namespace: String
        get() {
            return when (this) {
                is CppIntrinsicType -> namespace.orEmpty()
                else -> getSetting(Namespace) ?: defaultNamespace
            }
        }

    private val Declaration.platformTypeName: String
        get() {
            return getSetting(Intrinsic)?.name ?: name
        }

    private val Member.Field.platformType: IType
        get() {
            return if (type is Declaration) {
                type.getSetting(Intrinsic) ?: type
            } else {
                type
            }
        }

    private val Declaration.isIntrinsic: Boolean
        get() = getSetting(Intrinsic) != null

    private val defaultListType = CppIntrinsicType("std", "vector", null)

    private val Declaration.listType: CppIntrinsicType
        get() {
            return getSetting(ListType) ?: defaultListType
        }

    private val defaultAllocatorType = { itemType: IType -> "rd::allocator<${itemType.name}>" }

    private fun Declaration.allocatorType(itemType: IType): String {
        return (getSetting(AllocatorType) ?: defaultAllocatorType).invoke(itemType)
    }

    private fun Declaration.exportMacroName(): String? = getSetting(ExportMacroName)

    object ListType : ISetting<CppIntrinsicType, Declaration>

    object AllocatorType : ISetting<(IType) -> String, Declaration>

    object Intrinsic : ISetting<CppIntrinsicType, Declaration>

    object AdditionalHeaders : SettingWithDefault<List<String>, Toplevel>(listOf())

    object ExportMacroName : ISetting<String, Declaration>

    object PublicCtors : ISetting<Unit, Declaration>

    object FsPath : ISetting<(Cpp17Generator) -> File, Toplevel>

    object TargetName : ISetting<String, Toplevel>

    object UsePrecompiledHeaders : SettingWithDefault<Boolean, Toplevel>(false)

    object GeneratePrecompiledHeaders : SettingWithDefault<Boolean, Toplevel>(false)

    private fun fsExtension(isDefinition: Boolean) = if (isDefinition) "cpp" else "h"

    private fun Declaration.sourceFileName() = this.fsName(true)
    private fun Declaration.headerFileName() = this.fsName(false)

    private fun Declaration.fsName(isDefinition: Boolean) =
        "$name$generatedFileSuffix.${fsExtension(isDefinition)}"

    protected open fun Toplevel.fsPath(): File = getSetting(FsPath)?.invoke(this@Cpp17Generator)
        ?: File(folder, this.name)


    protected open fun Declaration.fsPath(tl: Toplevel, isDefinition: Boolean): File = getSetting(FsPath)?.invoke(this@Cpp17Generator)
        ?: File(tl.fsPath(), fsName(isDefinition))

    private fun Root.targetName(): String {
        return getSetting(TargetName) ?: this.name
    }

    private fun Root.usePrecompiledHeaders(): Boolean = getSetting(UsePrecompiledHeaders) ?: false

    private fun Root.generatePrecompiledHeaders(): Boolean = getSetting(GeneratePrecompiledHeaders) ?: false

    private val Class.isInternRoot: Boolean
        get() = internRootForScopes.isNotEmpty()

    private fun InternScope.hash(): String {
        val s = this.keyName
        return """rd::util::getPlatformIndependentHash("$s")"""
    }

    private fun Class.withInternRootsHere(field: String): String {
        val roots = internRootForScopes.joinToString { "\"$it\"" }
        return "ctx.withInternRootsHere($field, {$roots})"
    }
    //endregion

    //region language specific types
    private open class RdCppLibraryType(override val name: String) : IType

    private object RdId : RdCppLibraryType("rd::RdId")

    private object ByteArray : RdCppLibraryType("rd::Buffer::ByteArray")

    private object IPolymorphicSerializable : RdCppLibraryType("rd::IPolymorphicSerializable")

    private object IUnknownInstance : RdCppLibraryType("rd::IUnknownInstance")

    private object RdBindableBase : RdCppLibraryType("rd::RdBindableBase")

    private object RdExtBase : RdCppLibraryType("rd::RdExtBase")

    //endregion",
    fun String.isWrapper(): Boolean {
        return startsWith("rd::Wrapper")
    }

    private fun String.wrapper(): String {
        return if (isWrapper()) {
            this
        } else {
            "rd::Wrapper<$this>"
        }
    }

    private fun String.optional(): String {
        return "rd::optional<$this>"
    }

    //endregion

    //region PrettyPrinter

    private fun String.includeQuotes(): String {
        return """
            #include "$this"
            """.trimIndent()
    }

    private fun String.includeAngleBrackets(): String {
        return "#include <$this>"
    }

    private fun String.includeWithExtension(extension: String = "h"): String {
        return "#include \"${this}.$extension\""
    }

    private fun Declaration.includeWithExtension(): String {
        return this.headerFileName().includeQuotes()
    }

    private fun IType.includeWithExtension(): String {
        return this.name.includeWithExtension("h")
    }

    private fun PrettyPrinter.carry(): String {
        return ";" + eolKind.value
    }

    protected fun PrettyPrinter.block(prefix: String, postfix: String, body: PrettyPrinter.() -> Unit) {
        +prefix
        indent(body)
        +postfix
    }

    protected fun PrettyPrinter.blockNoIndent(prefix: String, postfix: String, body: PrettyPrinter.() -> Unit) {
        +prefix
        body()
        +postfix
    }

    protected fun PrettyPrinter.braceBlock(body: PrettyPrinter.() -> Unit) {
        +"{"
        indent(body)
        +"}"
    }

    protected fun PrettyPrinter.braceBlockNoIndent(body: PrettyPrinter.() -> Unit) {
        +"{"
        body()
        +"}"
    }

    protected fun PrettyPrinter.titledBlock(title: String, body: PrettyPrinter.() -> Unit) {
        +"$title {"
        indent(body)
        +"}"
    }

    protected fun PrettyPrinter.titledBlockNoIndent(title: String, body: PrettyPrinter.() -> Unit) {
        +"$title {"
        body()
        +"}"
    }

    protected fun PrettyPrinter.comment(comment: String) {
        +"// $comment"
    }

    protected fun PrettyPrinter.declare(signature: Signature?) {
        signature?.let {
            this.println(it.decl())
        }
    }

    protected fun PrettyPrinter.declare(signatures: List<Signature>) {
        signatures.forEach {
            declare(it)
        }
    }

    protected fun PrettyPrinter.define(signature: Signature?, body: PrettyPrinter.() -> Unit) {
        signature?.let {
            this.println(it.def())
            braceBlock {
                this.body()
            }
        }
    }

    private fun PrettyPrinter.privateBlock(body: PrettyPrinter.() -> Unit) {
        println()
        +"private:"
        indent {
            body()
        }
    }

    private fun PrettyPrinter.protectedBlock(body: PrettyPrinter.() -> Unit) {
        println()
        +"protected:"
        indent {
            body()
        }
    }

    private fun PrettyPrinter.publicBlock(body: PrettyPrinter.() -> Unit) {
        println()
        +"public:"
        indent {
            body()
        }
    }

    private fun Member.getter() = "get_${this.publicName}"

    private fun PrettyPrinter.withNamespace(s: String, body: PrettyPrinter.() -> Unit) {
        titledBlockNoIndent("namespace $s") {
            body()
        }
    }

    private fun PrettyPrinter.surroundWithNamespaces(namespace: String, body: PrettyPrinter.() -> Unit) {
        namespace.split("::").foldRight(body) { s, acc ->
            {
                withNamespace(s, acc)
            }
        }()
        //don't touch. it works
    }

    //endregion
    private val IType.isPredefinedNumber: Boolean
        get() = this is PredefinedType.UnsignedIntegral ||
            this is PredefinedType.NativeIntegral ||
            this is PredefinedType.bool ||
            this is PredefinedType.char

    private val IType.isPrimitive: Boolean
        get() = this is PredefinedType.NativeFloatingPointType || this.isPredefinedNumber

    private fun IType.isAbstract0() = (this is Struct.Abstract || this is Class.Abstract)
    private fun IType.isAbstract() = (this.isAbstract0()) || (this is InternedScalar && (this.itemType.isAbstract0()))

    fun Member.Field.isAbstract() = this.type.isAbstract()

    fun IType.substitutedName(scope: Declaration, rawType: Boolean = false, omitNullability: Boolean = false): String = when (this) {
        is Enum -> sanitizedName(scope)
//        is Struct.Concrete -> sanitizedName(scope)
        is Declaration -> {
            val fullName = sanitizedName(scope)
            if (rawType || isIntrinsic) {
                fullName
            } else {
                fullName.wrapper()
            }
        }
        is INullable -> {
            if (omitNullability) {
                itemType.substitutedName(scope, rawType, omitNullability)
            } else {
                val substitutedName = itemType.substitutedName(scope, true, omitNullability)
                val item = itemType
                when {
                    item is PredefinedType.string -> substitutedName.wrapper()
                    item is PredefinedType -> substitutedName.optional()
                    item is Enum -> substitutedName.optional()
                    item is Declaration && item.isIntrinsic -> substitutedName.optional()
                    else -> substitutedName.wrapper()
                }
            }
        }
        is InternedScalar -> {
            val substitutedName = itemType.substitutedName(scope, rawType, omitNullability)
            if (rawType) {
                substitutedName
            } else {
                substitutedName.wrapper()
            }
        }
        is IArray -> "${scope.listType.withNamespace()}<${itemType.substitutedName(scope, false, omitNullability)}>"
        is IImmutableList -> "${scope.listType.withNamespace()}<${itemType.substitutedName(scope, false, omitNullability)}>"
        is IAttributedType -> itemType.substitutedName(scope, rawType, omitNullability)

        is PredefinedType.char -> "wchar_t"
        is PredefinedType.byte -> "uint8_t"
        is PredefinedType.short -> "int16_t"
        is PredefinedType.int -> "int32_t"
        is PredefinedType.long -> "int64_t"
        is PredefinedType.string -> {
            val type = "std::wstring"
            if (rawType) {
                type
            } else {
                type.wrapper()
            }

        }
        is PredefinedType.UnsignedIntegral -> {
            if (itemType is PredefinedType.byte) {
                "uint8_t"
            } else {
                "u" + itemType.substitutedName(scope)
            }
        }
        is PredefinedType.dateTime -> "rd::DateTime"
        is PredefinedType.timeSpan -> "rd::TimeSpan"
        is PredefinedType.guid -> "UUID"
        is PredefinedType.uri -> "URI"
        is PredefinedType.secureString -> "RdSecureString"
        is PredefinedType.void -> "rd::Void"
        is PredefinedType -> name.decapitalizeInvariant()
        is RdCppLibraryType -> name

        else -> fail("Unsupported type ${javaClass.simpleName}")
    }

    fun IType.templateName(scope: Declaration, omitNullability: Boolean = false) = substitutedName(scope, true, omitNullability)

    protected val IType.isPrimitivesArray
        get() = (this is IArray || this is IImmutableList) && this.isPrimitive

    protected fun IType.leafSerializerRef(scope: Declaration): String? {

        return when (this) {
            is Enum -> "Polymorphic<${sanitizedName(scope)}>"
            is PredefinedType -> "Polymorphic<${templateName(scope)}>"
            is Declaration -> {
                val name = sanitizedName(scope)
                if (isAbstract || isOpen) {
                    "AbstractPolymorphic<$name>"
                } else {
                    "Polymorphic<$name>"
                }
            }


            is IArray -> if (this.isPrimitivesArray) "Polymorphic<${substitutedName(scope)}>" else null
            else -> null
        }?.let { "rd::$it" }
    }

    protected fun IType.serializerRef(scope: Declaration, isUsage: Boolean, withNamespace: Boolean): String {
        val className = if (withNamespace) "${scope.namespace}::" + scope.name else scope.name
        return leafSerializerRef(scope)
            ?: (isUsage.condstr { "$className::" } + when (this) {
                is InternedScalar -> "__${name}At${internKey.keyName}Serializer"
                else -> "__${name}Serializer"
            })
    }

//endregion

    //region Member.
    val Member.Reactive.actualFlow: FlowKind get() = flowTransform.transform(flow)

    protected open fun Member.Reactive.intfSimpleName(scope: Declaration): String {
//        val async = this.freeThreaded.condstr { "Async" }
            return "rd::" + when (this) {
                is Member.Reactive.Task -> when (actualFlow) {
                    Sink -> "RdEndpoint"
                    Source -> "RdCall"
                    Both -> fail("Unsupported flow direction for tasks")
                }
                is Member.Reactive.Signal -> when (actualFlow) {
                    Sink -> "ISource"
                    Source, Both -> "ISignal"
                }
                is Member.Reactive.Stateful.Property -> when (actualFlow) {
                    Sink -> "IProperty"
                    Source, Both -> "IProperty"
                }

                is Member.Reactive.Stateful.AsyncProperty -> error("Not supported")

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

                is Member.Reactive.Stateful.Extension -> implSimpleName(scope)
            }
        }

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open fun Member.Reactive.implSimpleName(scope: Declaration): String = "rd::" + when (this) {
            is Member.Reactive.Task -> when (actualFlow) {
                Sink -> "RdEndpoint"
                Source -> "RdCall"
                Both -> "RdSymmetricCall"
            }
            is Member.Reactive.Signal -> "RdSignal"
            is Member.Reactive.Stateful.Property -> "RdProperty"
            is Member.Reactive.Stateful.List -> "RdList"
            is Member.Reactive.Stateful.Set -> "RdSet"
            is Member.Reactive.Stateful.Map -> "RdMap"
            is Member.Reactive.Stateful.Extension -> implSubstitutedName(scope)

            else -> fail("Unsupported member: $this")
        }

    protected open fun Member.Reactive.Stateful.Extension.implSubstitutedName(scope: Declaration): String = findDelegate(this@Cpp17Generator, flowTransform)?.delegateType?.let { delegateType ->
        when (delegateType) {
            is Member.DelegateType.Custom -> delegateType.fqn
            is Member.DelegateType.Delegated -> delegateType.type.substitutedName(scope)
        }
    } ?: this.javaClass.simpleName


    protected open fun Member.Reactive.ctorSimpleName(scope: Declaration): String = when (this) {
        is Member.Reactive.Stateful.Extension -> factoryFqn(scope)
        else -> implSimpleName(scope)
    }

    protected open fun Member.intfSubstitutedName(scope: Declaration) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.templateName(scope)
        is Member.Reactive -> {
            val customSerializers = if (this is Member.Reactive.Task) customSerializers(scope, false) else emptyList()
            intfSimpleName(scope) + (genericParams.toList().map { it.templateName(scope) } + customSerializers).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")
        }
        is Member.Const -> type.templateName(scope)
        is Member.Method -> publicName
    }

    protected open fun Member.implSubstitutedName(scope: Declaration) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
        is Member.Reactive -> {
            implSimpleName(scope) + (genericParams.toList().map { it.templateName(scope) } + customSerializers(scope, false)).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")
        }
        is Member.Const -> type.substitutedName(scope)
        is Member.Method -> publicName
    }

    protected open fun Member.implTemplateName(scope: Declaration) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.templateName(scope)
        is Member.Reactive -> {
            implSimpleName(scope) + (genericParams.toList().map { it.templateName(scope) } + customSerializers(scope, false)).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")
        }
        is Member.Const -> type.templateName(scope)
        is Member.Method -> publicName
    }


    protected open fun Member.ctorSubstitutedName(scope: Declaration) = when (this) {
        is Member.Reactive.Stateful.Extension -> {
            "rd::" + ctorSimpleName(scope) + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.templateName(scope) }
        }
        else -> implSubstitutedName(scope)
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


    open val Member.publicName: String get() = name
    open val Member.encapsulatedName: String get() = "${publicName}_"
    open val Member.isEncapsulated: Boolean get() = when (this) {
        is Member.Reactive.Stateful.Extension -> when {
            isSimplyDelegated(this@Cpp17Generator, flowTransform) -> false
            else -> true
        }
        is Member.Reactive -> true
        else -> false
    }

    internal fun ctorParam(member: Member, scope: Declaration, withSetter: Boolean): String {
        val typeName = member.implSubstitutedName(scope)
        return StringBuilder().also {
            it.append("$typeName ${member.encapsulatedName}")
            if (withSetter) {
                it.appendDefaultInitialize(member, typeName)
            }
        }.toString()
    }

    protected fun Member.Reactive.customSerializers(scope: Declaration, withNamespace: Boolean): List<String> {
        return genericParams.asList().map { it.serializerRef(scope, true, withNamespace) }
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
    //endregion

    //region Declaration.
    protected fun Declaration.sanitizedName(scope: Declaration): String {
        val needQualification = namespace != scope.namespace
        return needQualification.condstr { "$namespace::" } + platformTypeName
    }

    /**
     * Returns full name of receiver with namespace
     */
    private fun Declaration.withNamespace() =
        if (namespace.isEmpty()) {
            name
        } else {
            "$namespace::$platformTypeName"
        }


    protected fun Declaration.scopeResolution(): String {
        return "$name::"
    }

    internal fun bases(declaration: Declaration): MutableList<BaseClass> {
        val baseName = declaration.baseNames()
        return if (declaration.base == null) {
            mutableListOf<BaseClass>().apply {
                if (declaration !is Toplevel) {
                    add(BaseClass(IPolymorphicSerializable, emptyList()))
                }
                addAll(baseName)
            }
        } else {
            baseName.toMutableList().apply {
                if (isUnknown(declaration)) {
                    add(BaseClass(IUnknownInstance, listOf(Member.Field("unknownId", RdId))))
                }
            }
        }
    }

    protected fun Declaration.baseNames(): List<BaseClass> {
        return this.base?.let {
            mutableListOf(BaseClass(it as IType, it.allMembers))
        } ?: (when (this) {
            is Toplevel -> listOf(BaseClass(RdExtBase, emptyList()))
            is Class, is Aggregate -> listOf(BaseClass(RdBindableBase, emptyList()))
            else -> listOf()
        })
    }

    val Declaration.primaryCtorVisibility: String
        get() {
            val modifier =
                when {
                    hasSetting(PublicCtors) -> "public"
                    isAbstract || isOpen -> "protected"
                    hasSecondaryCtor -> "private"
                    isExtension -> "public"
                    this is Toplevel -> "private"
                    else -> "public"
                } + ":"
            return modifier
        }

    private val Declaration.hasSecondaryCtor: Boolean get() = (this is Toplevel || this.isConcrete) && this.allMembers.any { it.hasEmptyConstructor }
//endregion

    private fun pchFileName(targetName: String) = "pch_$targetName"

    private fun File.createPchHeader(pchHeaderFile: String) {
        val p = PrettyPrinter().apply {
            withIncludeGuard(pchHeaderFile.includeGuardName()) {
                +"#include \"pch.h\""
            }
        }
        File(this, pchHeaderFile).writeText(p.toString())
    }

    private fun File.createPchSource(pchCppFile: String, pchHeaderFile: String) {
        File(this, pchCppFile).writeText("#include \"${pchHeaderFile}\"")
    }

    private fun File.cmakeLists(root: Root, fileNames: List<String>) {
        val targetName = root.targetName()
        val usingPrecompiledHeaders = root.usePrecompiledHeaders()
        val generatePrecompiledHeaders = root.generatePrecompiledHeaders()

        mkdirs()
        if (usingPrecompiledHeaders && !generatePrecompiledHeaders) {
            fail("Option 'usingPrecompiledHeaders' conflicts with disabled option 'generatePrecompiledHeaders'")
        }

        val pchHeaderFile = "${pchFileName(targetName)}.h"
        val pchCppFile = "${pchFileName(targetName)}.cpp"

        if (generatePrecompiledHeaders) {
            createPchHeader(pchHeaderFile)
            createPchSource(pchCppFile, pchHeaderFile)
        }

        val cMakeLists = File(this, "CMakeLists.txt")
        PrettyPrinter().apply {
            eolKind = Eol.linux

            println("cmake_minimum_required(VERSION 3.7)")

            val pchOptionVariable = "ENABLE_PCH_HEADERS_FOR_$targetName"

            val targetFiles = fileNames.toMutableList()
            if (generatePrecompiledHeaders) {
                val onOrOff = if (usingPrecompiledHeaders) "ON" else "OFF"

                println("option($pchOptionVariable \"Enable precompiled headers\" $onOrOff)")
                println("""
                |if ($pchOptionVariable)
                |    set(PCH_CPP_OPT $pchCppFile)
                |else ()
                |    set(PCH_CPP_OPT "")
                |endif ()""".trimMargin()
                )
                targetFiles.add("\${PCH_CPP_OPT}")
            }

            println("add_library($targetName STATIC \n${targetFiles.joinToString(separator = "\n")})")
            println("target_include_directories($targetName PUBLIC \${CMAKE_CURRENT_SOURCE_DIR})")
            println("target_link_libraries($targetName PUBLIC rd_framework_cpp)")

            if (generatePrecompiledHeaders) {
                println("""
                        |if ($pchOptionVariable)
                        |    include(${Files.PrecompiledHeaderCmake})
                        |    add_precompiled_header($targetName $pchHeaderFile SOURCE_CXX $pchCppFile FORCEINCLUDE)
                        |endif ()""".trimMargin()
                )
            }

            cMakeLists.writeText(this.toString())
        }

        if (usingPrecompiledHeaders) {
            this.precompiledHeaderCmake()
        }
    }

    private fun File.precompiledHeaderCmake() {
        val file = File("./src/main/resources/cpp/${Files.PrecompiledHeaderCmake}")
        file.copyTo(this.resolve(Files.PrecompiledHeaderCmake), overwrite = true)
    }


    private fun PrettyPrinter.predeclare(decl: Declaration) {
        surroundWithNamespaces(decl.namespace) {
            when (decl) {
                is Enum -> {
                    val predecl = decl.getSetting(IsNonScoped)?.let {
                        "enum ${decl.platformTypeName} : $it"
                    } ?: "enum class ${decl.platformTypeName}"
                    +("$predecl;")
                }
                else -> {
                }
            }
        }
    }

    object EnumConstantValue : ISetting<Int, Member.EnumConst>
    object IsNonScoped : ISetting<String, Enum>

    private fun File.templateInstantiate(toplevels: List<Toplevel>, instatiationFileName: String) : List<String> {
        fun collectInitializedEnums(): List<Enum> = toplevels.flatMap { tl ->
            tl.declaredTypes.filterIsInstance<Enum>()
                .filter { enum ->
                    !enum.flags
                }
                .filter { enum ->
                    enum.constants.any { field ->
                        field.getSetting(EnumConstantValue) != null
                    }
                }
        }

        val initializedEnums = collectInitializedEnums()

        val header = File(this, "${instatiationFileName}.h").also { file ->
            FileSystemPrettyPrinter(file).use {
                withIncludeGuard("${instatiationFileName}.h".includeGuardName()) {
                    +polymorphicHeader.includeWithExtension("h")
                    println()
                    initializedEnums
                        .mapNotNull { enum ->
                            if (enum.isIntrinsic) {
                                enum.pointcut.getSetting(AdditionalHeaders)
                            } else {
                                listOf("${enum.pointcut.name}/${enum.headerFileName()}")
                            }
                        }
                        .flatten()
                        .map { it.includeQuotes() }
                        .forEach { +it }
                    println()
                    initializedEnums.forEach { enum ->
                        predeclare(enum)
                    }
                    println()
                    withNamespace("rd") {
                        initializedEnums.forEach { enum ->
                            val map = enum.constants
                                .mapIndexed { index, field ->
                                    field.getSetting(EnumConstantValue)?.let { _ ->
                                        Pair(index, field)
                                    }
                                }
                                .filterNotNull()
                                .toMap()

                            val enumTypeName = enum.withNamespace()
                            blockNoIndent("""
                                |template <>
                                |class Polymorphic<$enumTypeName> {
                            """.trimMargin(), "};") {
                                publicBlock {
                                    block("static $enumTypeName read(SerializationCtx& ctx, Buffer& buffer) {", "}") {
                                        +"int32_t x = buffer.read_integral<int32_t>();"
                                        blockNoIndent("switch (x) {", "}") {
                                            map.forEach { (key, value) ->
                                                +"""
                                                |case $key:
                                                |   return ${enumTypeName + "::" + value.name};
                                                """.trimMargin()
                                            }
                                            +"""
                                            |default:
                                            |   return static_cast<${enumTypeName}>(x);
                                            """.trimMargin()
                                        }
                                    }
                                    println()
                                    block("static void write(SerializationCtx& ctx, Buffer& buffer, $enumTypeName const& value) {", "}") {
                                        blockNoIndent("switch (value) {", "}") {
                                            map.forEach { (key, value) ->
                                                +"""
                                                |case ${enumTypeName + "::" + value.name}: {
                                                |   buffer.write_integral<int32_t>($key);
                                                |   return;
                                                |}
                                             """.trimMargin()
                                            }
                                            +"default:"
                                            indent {
                                                +"buffer.write_integral<int32_t>(static_cast<int32_t>(value));"
                                            }
                                        }
                                    }
                                }
                            }
                            println()
                            +"extern template class Polymorphic<$enumTypeName>;"
                            println()
                        }
                    }
                }
            }
        }

        val source = File(this, "${instatiationFileName}.cpp").also { file ->
            FileSystemPrettyPrinter(file).use {
                +instatiationFileName.includeWithExtension("h")
                println()
                withNamespace("rd") {
                    initializedEnums.forEach { enum ->
                        val enumType = enum.withNamespace()
                        +"template class Polymorphic<$enumType>;"
                    }
                }
            }
        }

        return listOf(header, source).map { f -> f.name }
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

    override fun realGenerate(toplevels: List<Toplevel>) {
        if (toplevels.isEmpty()) return

        val root = toplevels.first().root
        val instantiationsFileName = "instantiations_${root.targetName()}"
        val allFilePaths =
            folder.templateInstantiate(toplevels, instantiationsFileName).toMutableList()

        toplevels.forEach { tl ->
            val directory = tl.fsPath()
            directory.mkdirs()
            val types = (tl.declaredTypes + tl + unknowns(tl.declaredTypes)).filter { !it.isIntrinsic }
            val fileNames = types.flatMap { listOf(it.sourceFileName(), it.headerFileName()) }
            allFilePaths += fileNames.map { "${tl.name}/$it" }

            for (type in types) {
                FileSystemPrettyPrinter(type.fsPath(tl, false)).use {
                    //actual generation
                    autogenerated()
                    header(type, instantiationsFileName)
                }
                FileSystemPrettyPrinter(type.fsPath(tl, true)).use {
                    //actual generation
                    autogenerated()
                    source(type, types)
                }
            }
        }

        folder.cmakeLists(root, allFilePaths)
    }

    private fun PrettyPrinter.withIncludeGuard(includeGuardMacro: String, action: PrettyPrinter.() -> Unit) {
        +"#ifndef $includeGuardMacro"
        +"#define $includeGuardMacro"

        println()

        action()

        println()

        +"#endif // $includeGuardMacro"
    }

    private fun String.includeGuardName(): String {
        return this.replace('.', '_').uppercase()
    }

    private fun Declaration.includeGuardName(): String {
        return this.headerFileName().includeGuardName()
    }

    private fun PrettyPrinter.withDisabledWarnings(disabledWarnings: IntArray, action: PrettyPrinter.() -> Unit)
    {
        if (disabledWarnings.isNotEmpty()) {
            println()
            ifDefDirective(msvcCheckMacro) {
                +"#pragma warning( push )"
                disabledWarnings.forEach {
                    +"#pragma warning( disable:$it )"
                }
            }
            println()
        }

        action()

        if (disabledWarnings.isNotEmpty()) {
            println()
            ifDefDirective(msvcCheckMacro) {
                +"#pragma warning( pop )"
            }
            println()
        }
    }

    //region files
    fun PrettyPrinter.header(decl: Declaration, instantiationsFileName: String) {
        withIncludeGuard(decl.includeGuardName()) {
            println()

            includesDecl(instantiationsFileName)
            println()

            dependenciesDecl(decl)
            println()

            val disabledWarnings = VsWarningsDefault ?: intArrayOf()
            withDisabledWarnings(disabledWarnings) {
                if (decl is Toplevel && decl.isLibrary) {
                    comment("library")
                    surroundWithNamespaces(decl.namespace) {
                        println()
                        libdecl(decl)
                        println()
                    }
                } else {
                    typedecl(decl)
                }
            }

            println()
        }
    }

    fun PrettyPrinter.source(decl: Declaration, dependencies: List<Declaration>) {
        +decl.includeWithExtension()

        println()

        if (decl is Enum) {
            surroundWithNamespaces(decl.namespace) {
                enumToStringTraitDef(decl)
            }
            return
        }

        if (decl is Toplevel) {
            dependencies.filter { !(it.isAbstract || it.isOpen) }.filterIsInstance<IType>().println {
                if (it is Declaration) {
                    "${it.pointcut!!.name}/${it.headerFileName()}".includeQuotes()
                } else {
                    it.includeWithExtension()
                }
            }
        }
        println()
        if (decl is Toplevel) {
            +"${decl.root.name}/${decl.root.headerFileName()}".includeQuotes()
        }
        if (decl.isAbstract || decl.isOpen) {
            +(unknown(decl)!!.includeWithExtension())
        }
        if (decl is Root) {
            decl.toplevels.forEach {
                +"${it.name}/${it.headerFileName()}".includeQuotes()
            }
        }

        val disabledWarnings = VsWarningsDefault ?: intArrayOf()
        withDisabledWarnings(disabledWarnings) {
            if (decl is Toplevel && decl.isLibrary) {
                surroundWithNamespaces(decl.namespace) { libdef(decl, decl.declaredTypes + unknowns(decl.declaredTypes)) }
            } else {
                surroundWithNamespaces(decl.namespace) { typedef(decl) }
            }
        }
    }
//endregion

    //region declaration
    private fun Declaration.classNameDecl(): String {
        return exportMacroName()
            ?.let { "class $it $name" }
            ?: "class $name"
    }

    protected open fun PrettyPrinter.libdecl(decl: Declaration) {
        blockNoIndent("${decl.classNameDecl()} {", "};") {
            publicBlock() {
                registerSerializersTraitDecl(decl)
            }
        }
    }

    private fun PrettyPrinter.docDecl(decl: Declaration) {
        if (decl.documentation != null || decl.sourceFileAndLine != null) {
            +"/// <summary>"
            decl.documentation?.let {
                +"/// $it"
            }
            decl.sourceFileAndLine?.let {
                +"/// <p>Generated from: $it</p>"
            }
            +"/// </summary>"
        }
    }

    protected open fun PrettyPrinter.typedecl(decl: Declaration) {
        docDecl(decl)

        surroundWithNamespaces(decl.namespace) {
            println()
            if (decl is Enum) {
                enumDecl(decl)
                return@surroundWithNamespaces
            }

            if (decl is Interface) {
                Logger.root.warn { "CppGenerator doesn't support interfaces. Declaration will be ignored" }
                return@surroundWithNamespaces
            }

            if (decl.isOpen) {
                Logger.root.warn { "CppGenerator doesn't support open classes. All open classes wil be generated as abstract" }
            }

            if (decl.isAbstract) comment("abstract")
            if (decl.isOpen) comment("open")
            if (decl is Struct.Concrete && decl.base == null) comment("data")

            p(decl.classNameDecl())

            baseClassTraitDecl(decl)
            blockNoIndent(" {", "};") {
                companionTraitDecl(decl)

                if (decl.isExtension) {
                    indent() {
                        comment("extension")
                        declare(extensionTraitDecl(decl as Ext))
                    }
                }

                privateBlock() {
                    comment("custom serializers")
                    customSerializersTrait(decl)
                }

                publicBlock() {
                    comment("constants")
                    constantsDecl(decl)
                }

                protectedBlock() {
                    comment("fields")
                    fieldsDecl(decl)
                }

                privateBlock() {
                    comment("initializer")
                    declare(initializerTraitDecl(decl))
                }

                //            +(decl.primaryCtorVisibility)
                publicBlock() {
                    primaryCtorTraitDecl(decl)?.let {
                        comment("primary ctor")
                        declare(it)
                    }

                    secondaryConstructorTraitDecl(decl)?.let {
                        comment("secondary constructor")
                        declare(it)
                    }

                    if (shouldGenerateDeconstruct(decl)) {
                        println()
                        comment("deconstruct trait")
                        ifDefDirective(Features.__cpp_structured_bindings) {
                            deconstructTrait(decl)
                        }
                    }

                    println()
                    comment("default ctors and dtors")
                    defaultCtorsDtorsDecl(decl)

                    println()
                    comment("reader")
                    declare(readerTraitDecl(decl))

                    println()
                    comment("writer")
                    declare(writerTraitDecl(decl))

                    println()
                    comment("virtual init")
                    declare(virtualInitTraitDecl(decl))

                    println()
                    comment("identify")
                    declare(identifyTraitDecl(decl))

                    println()
                    comment("getters")
                    declare(gettersTraitDecl(decl))

                    println()
                    comment("intern")
                    declare(internTraitDecl(decl))
                }

                privateBlock() {
                    comment("equals trait")
                    declare(equalsTraitDecl(decl))
                }

                publicBlock() {
                    comment("equality operators")
                    equalityOperatorsDecl(decl)

                    comment("hash code trait")
                    declare(hashCodeTraitDecl(decl))

                    comment("type name trait")
                    declare(typenameTraitDecl(decl))

                    comment("static type name trait")
                    declare(staticTypenameTraitDecl(decl))
                }

                privateBlock() {
                    comment("polymorphic to string")
                    declare(polymorphicToStringTraitDecl(decl))
                }

                publicBlock() {
                    comment("external to string")
                    declare(externalToStringTraitDecl(decl))
                }

                /*if (decl.isExtension) {
                    extensionTraitDef(decl as Ext)
                }*/
            }
            println()
        }

//        externTemplates(decl)

        hashSpecialization(decl)

        if (shouldGenerateDeconstruct(decl)) {
            println()
            ifDefDirective(Features.__cpp_structured_bindings) {
                comment("tuple trait")
                tupleSpecialization(decl)
            }
        }
    }

    protected open fun PrettyPrinter.enumDecl(decl: Enum) {
        block("enum class ${decl.name} {", "};") {
            +decl.constants.withIndex().joinToString(separator = ",${eolKind.value}") { (idx, field) ->
                val doc = docComment(field.documentation)
                val name = sanitize(field.name)
                val value = field.getSetting(EnumConstantValue)?.let { " = $it" }
                    ?: decl.flags.condstr { " = 1 << $idx" }
                doc + name + value
            }
        }

        if (decl.flags) {
            +"RD_DEFINE_ENUM_FLAG_OPERATORS(${decl.name})"
        }

        declare(enumToStringTraitDecl(decl))
    }

    private fun enumToStringTraitDecl(decl: Enum): MemberFunction {
        return MemberFunction("std::string", "to_string(const ${decl.name} & value)", null)
    }

    private fun PrettyPrinter.enumToStringTraitDef(decl: Enum) {
        define(enumToStringTraitDecl(decl)) {
            +"""
            |switch(value) {
            |${decl.constants.joinToString(separator = eol) { """case ${decl.name}::${sanitize(it.name)}: return "${it.name}";""" }}
            |default: return std::to_string(static_cast<int32_t>(value));
            |}
            """.trimMargin()

        }
    }

    protected fun primaryCtorParams(decl: Declaration): Constructor.Primary.AllArguments {
        val own = decl.ownMembers
        val base = decl.membersOfBaseClasses
        return Constructor.Primary.AllArguments(own, base.plus(unknownMembers(decl)))
    }

    protected fun secondaryCtorParams(decl: Declaration): Constructor.Secondary.AllArguments {
        val ownMembers = decl.allMembers
            .asSequence()
            .filter { !it.hasEmptyConstructor }.plus(unknownMembersSecondary(decl))
            .toList()
        val membersOfBaseClasses = decl.allMembers
            .asSequence()
            .map {
                if (ownMembers.contains(it)) {
                    it
                } else {
                    null
                }
            }
            .toList()
        /*if (ownMembers.size + membersOfBaseClasses.size == 0) {
            return Constructor.Secondary.AllArguments()
        }*/
        val unknowns = unknownMembersSecondary(decl)
        return Constructor.Secondary.AllArguments(ownMembers, membersOfBaseClasses.plus(unknowns))
    }
//endregion

    //region TraitDecl
    protected fun PrettyPrinter.includesDecl(instantiationsFileName: String) {
//        +"class ${decl.name};"

        val standardHeaders = listOf(
            "cstring",
            "cstdint",
            "vector",
            "ctime"
        )

        val frameworkHeaders = listOf(
            //root
            "protocol/Protocol",
            //types
            "types/DateTime",
            //impl
            "impl/RdSignal",
            "impl/RdProperty",
            "impl/RdList",
            "impl/RdSet",
            "impl/RdMap",
            //base
            "base/ISerializersOwner",
            "base/IUnknownInstance",
            //serialization
            "serialization/ISerializable",
            polymorphicHeader,
            "serialization/NullableSerializer",
            "serialization/ArraySerializer",
            "serialization/InternedSerializer",
            "serialization/SerializationCtx",
            "serialization/Serializers",
            //ext
            "ext/RdExtBase",
            //task
            "task/RdCall",
            "task/RdEndpoint",
            "task/RdSymmetricCall",
            //std stubs
            "std/to_string",
            "std/hash",
            "std/allocator",
            //enum
            "util/enum",
            //gen
            "util/gen_util"
        )

        +frameworkHeaders.joinToString(separator = eolKind.value) { it.includeWithExtension("h") }
        println()
        +standardHeaders.joinToString(separator = eolKind.value, transform = { it.includeAngleBrackets() })
        println()
        //third-party
        +"thirdparty".includeWithExtension("hpp")

        +instantiationsFileName.includeWithExtension("h")
    }

    private fun Declaration.parseType(type: IType, allowPredefined: Boolean): IType? {
        return when (type) {
            is IArray -> {
                parseType(type.itemType, allowPredefined)
            }
            is IImmutableList -> {
                parseType(type.itemType, allowPredefined)
            }
            is INullable -> {
                parseType(type.itemType, allowPredefined)
            }
            is IAttributedType -> {
                parseType(type.itemType, allowPredefined)
            }
            is InternedScalar -> {
                parseType(type.itemType, allowPredefined)
            }
            is Struct -> type
            is Class -> type
            is Enum -> type
            else -> {
                if (allowPredefined && type is PredefinedType) {
                    type
                } else {
                    null
                }
            }
        }
    }

    private fun PrettyPrinter.dependenciesDecl(decl: Declaration) {
        fun parseMember(member: Member): List<String> {
            val types = when (member) {
                is Member.EnumConst -> {
                    arrayListOf()
                }
                is Member.Field -> {
                    listOfNotNull(decl.parseType(member.type, false))
                }
                is Member.Reactive -> {
                    if (member is Member.Reactive.Stateful.Extension) {
                        listOfNotNull(/*member.fqn(this@Cpp17Generator, flowTransform), */decl.parseType(member.delegatedBy, false))
                    } else {
                        member.genericParams.fold(arrayListOf<IType>()) { acc, iType ->
                            decl.parseType(iType, false)?.let {
                                acc += it
                            }
                            acc
                        }
                    }
                }
                is Member.Const -> listOfNotNull(decl.parseType(member.type, false))
                is Member.Method ->  member.args.map { it.second } + member.resultType
            }
            return types.mapNotNull {
                when (it) {
                    is Root -> "${it.name}/${it.headerFileName()}"
                    is Declaration ->
                        if (!it.isIntrinsic) {
                            "${it.pointcut!!.name}/${it.headerFileName()}"
                        } else {
                            it.getSetting(Intrinsic)!!.header
                        }
                    else -> {
                        "${it.name}.h"
                    }
                }
            }
        }

        fun dependentTypes(decl: Declaration): List<String> {
            val bases = listOfNotNull(decl.base).map { it.headerFileName() }
            return (decl.ownMembers + decl.constantMembers)
                .asSequence()
                .map { parseMember(it) }
                .fold(arrayListOf<String>()) { acc, arrayList ->
                    acc += arrayList
                    acc
                }.plus(bases)
                //                .filter { dependencies.map { it.name }.contains(it) }
                .distinct().toList()
        }

        val extDecl = if (decl.isExtension) decl.pointcut else null
        if (extDecl != null) {
            val extToplevel = if (extDecl is Toplevel) extDecl else extDecl.pointcut!!
            +"${extToplevel.name}/${extDecl.headerFileName()}".includeQuotes()
            println()
        }
        dependentTypes(decl).printlnWithBlankLine { it.includeQuotes() }

        decl.getSetting(AdditionalHeaders)?.distinct()?.println { it.includeQuotes() }
    }

/*
    private fun PrettyPrinter.externTemplates(decl: Declaration) {
        +decl.ownMembers.filterIsInstance<Member.Reactive>().map {
            it.implSimpleName + (it.genericParams.toList().map { generic ->
                generic.templateName(FakeDeclaration(decl))
            } + it.customSerializers(FakeDeclaration(decl), true)).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")
        }
                .distinct()
                .joinToString(separator = eol, prefix = eol, postfix = eol) { "extern template class $it;" }
    }
*/

    private fun PrettyPrinter.baseClassTraitDecl(decl: Declaration) {
        p(bases(decl).joinToString(separator = ", ", prefix = " : ") { "public ${it.type.name}" })
    }


    private fun createMethodTraitDecl(decl: Toplevel): Signature? {
        if (decl.isExtension) return null
        return MemberFunction("void", "connect(rd::Lifetime lifetime, rd::IProtocol const * protocol)", decl.name)
    }

    fun PrettyPrinter.customSerializersTrait(decl: Declaration) {
        fun IType.serializerBuilder(): String = leafSerializerRef(decl) ?: ("rd::" + when (this) {
            is IArray -> "ArraySerializer<${itemType.serializerBuilder()}, ${decl.listType.withNamespace()}>"
            is IImmutableList -> "ArraySerializer<${itemType.serializerBuilder()}, ${decl.listType.withNamespace()}>"
            is INullable -> "NullableSerializer<${itemType.serializerBuilder()}>"
            is IAttributedType -> itemType.serializerBuilder()
            is InternedScalar -> """InternedSerializer<${itemType.serializerBuilder()}, ${internKey.hash()}>"""
            else -> fail("Unknown type: $this")
        })

        val allTypesForDelegation = decl.allMembers
            .filterIsInstance<Member.Reactive>()
            .flatMap { it.genericParams.toList() }
            .distinct()
            .filter { it.leafSerializerRef(decl) == null }

        allTypesForDelegation.println { "using ${it.serializerRef(decl, false, false)} = ${it.serializerBuilder()};" }
    }

    protected fun PrettyPrinter.registerSerializersTraitDecl(decl: Declaration) {
        val serializersOwnerImplName = "${decl.name}SerializersOwner"
        block("struct $serializersOwnerImplName final : public rd::ISerializersOwner {", "};") {
            declare(
                MemberFunction(
                    "void",
                    "registerSerializersCore(rd::Serializers const& serializers)",
                    decl.name
                ).const().override()
            )
        }
        println()
        +"static const $serializersOwnerImplName serializersOwner;"
        println()
    }

    protected fun PrettyPrinter.companionTraitDecl(decl: Declaration) {
        if (isUnknown(decl)) {
            +"friend class ${decl.name.dropLast(8)};"
            //todo drop "_Unknown" smarter
        }
        if (decl is Toplevel) {
            publicBlock() {
                registerSerializersTraitDecl(decl)
                println()
            }
            publicBlock() {
                declare(createMethodTraitDecl(decl))
                println()
            }
        }
    }


    private fun extensionTraitDecl(decl: Ext): MemberFunction? {
        val pointcut = decl.pointcut ?: return null
        return MemberFunction("""${decl.name} const &""", "getOrCreateExtensionOf(${pointcut.sanitizedName(decl)} & pointcut)", decl.name).static()
    }

    private fun PrettyPrinter.constantsDecl(decl: Declaration) {
        decl.constantMembers.forEach {
            val value = getDefaultValue(it)
            val type = when (it.type) {
                is PredefinedType.string -> "rd::wstring_view"
                else -> it.type.templateName(decl)
            }
            +"static constexpr $type ${it.name}{$value};"
        }
    }

    private fun PrettyPrinter.fieldsDecl(decl: Declaration) {
        val own = decl.ownMembers.map {
            val initial = getDefaultValue(it)?.let {
                "{$it}"
            } ?: ""
            "${ctorParam(it, decl, true)}$initial"
        }

        val unknowns = unknownMembers(decl).map { "${it.platformType.name} ${it.encapsulatedName}" }
        +own.asSequence().plus(unknowns).joinToString(separator = "") { "$it${carry()}" }

        if (decl is Class && decl.isInternRoot) {
            +"mutable rd::optional<rd::SerializationCtx> mySerializationContext;"
        }
    }

    fun initializerTraitDecl(decl: Declaration): MemberFunction {
        return MemberFunction("void", "initialize()", decl.name)
    }

    private fun primaryCtorTraitDecl(decl: Declaration): Constructor? {
        val arguments = primaryCtorParams(decl)
        return if (arguments.isEmpty()) {
            null
        } else {
            Constructor.Primary(this, decl, arguments)
        }
    }

    private fun secondaryConstructorTraitDecl(decl: Declaration): Constructor? {
        if (!decl.hasSecondaryCtor) return null

        val members = decl.allMembers
            .asSequence()
            .filter { !it.hasEmptyConstructor }
        if (members.count() == 0) {
            return null
        }
        val arguments = secondaryCtorParams(decl)
        return Constructor.Secondary(this, decl, arguments)
    }


    private fun PrettyPrinter.deconstructTrait(decl: Declaration) {
        +"template <size_t I>"
        define(MemberFunction("decltype(auto)", "get()", null).const()) {
            val n = decl.ownMembers.size
            val condition = "I < 0 || I >= $n"
            +"if constexpr ($condition) static_assert ($condition, \"$condition\");"
            decl.ownMembers.forEachIndexed { index, member ->
                +"else if constexpr (I==$index)  return static_cast<const ${member.implTemplateName(decl)}&>(${member.getter()}());"
            }
        }
    }

    private fun shouldGenerateDeconstruct(decl: Declaration) =
        (decl.isDataClass || (decl.isConcrete && decl.base == null && decl.hasSetting(AllowDeconstruct)))

    fun Declaration.defaultCtor(): Constructor.Default? {
        return if (allMembers.asSequence().filter { !it.hasEmptyConstructor }.toList().isEmpty()) {
            Constructor.Default(this@Cpp17Generator, this)
        } else {
            null
        }
    }

    fun PrettyPrinter.defaultCtorsDtorsDecl(decl: Declaration) {
        val name = decl.name
        println()

        decl.defaultCtor()?.let {
            declare(it)
        } ?: +"$name() = delete;"

        if (decl is IScalar) {
            println()
            +"$name($name const &) = default;"
            println()
            +"$name& operator=($name const &) = default;"
        }
        println()
        if (decl is Toplevel) {
            +"$name($name &&) = delete;"
            println()
            +"$name& operator=($name &&) = delete;"
        } else {
            +"$name($name &&) = default;"
            println()
            +"$name& operator=($name &&) = default;"
        }
        println()
        +"virtual ~$name() = default;"
    }

    private fun readerTraitDecl(decl: Declaration): Signature? {
        return when {
            decl.isConcrete -> MemberFunction(decl.name, "read(rd::SerializationCtx& ctx, rd::Buffer & buffer)", decl.name).static()
            decl.isAbstract || decl.isOpen -> MemberFunction(decl.name.wrapper(), "readUnknownInstance(rd::SerializationCtx& ctx, rd::Buffer & buffer, rd::RdId const& unknownId, int32_t size)", decl.name).static()
            else -> null
        }
    }

    protected fun writerTraitDecl(decl: Declaration): MemberFunction? {
        val signature = MemberFunction("void", "write(rd::SerializationCtx& ctx, rd::Buffer& buffer)", decl.name).const()

        return when {
            decl is Toplevel -> return null
            decl.isConcrete -> signature.override()
            else -> signature.abstract().override()
        }
    }

    protected fun virtualInitTraitDecl(decl: Declaration): MemberFunction? {
        if (decl !is BindableDeclaration) {
            return null
        }
        return MemberFunction("void", "init(rd::Lifetime lifetime)", decl.name).const().override()
    }

    protected fun identifyTraitDecl(decl: Declaration): MemberFunction? {
        if (decl !is BindableDeclaration) {
            return null
        }
        return MemberFunction("void", "identify(const rd::Identities &identities, rd::RdId const &id)", decl.name).const().override()
    }

    protected fun gettersTraitDecl(decl: Declaration): List<MemberFunction> {
        return decl.ownMembers.map { member -> MemberFunction("${member.intfSubstitutedName(decl)} const &", "${member.getter()}()", decl.name).const() }
    }

    protected fun internTraitDecl(decl: Declaration): MemberFunction? {
        return if (decl is Class && decl.isInternRoot) {
            return MemberFunction("rd::SerializationCtx &", "get_serialization_context()", decl.name).const().override()
        } else {
            null
        }
    }

    protected fun equalsTraitDecl(decl: Declaration): MemberFunction? {
        val signature = MemberFunction("bool", "equals(rd::ISerializable const& object)", decl.name).const()
        return if (decl is Toplevel || decl.isAbstract || decl.isOpen) {
            null
        } else {
            signature.override()
        }
    }

    protected fun PrettyPrinter.equalityOperatorsDecl(decl: Declaration) {
//        if (decl.isAbstract || decl !is IScalar) return

        +("friend bool operator==(const ${decl.name} &lhs, const ${decl.name} &rhs);")
        +("friend bool operator!=(const ${decl.name} &lhs, const ${decl.name} &rhs);")
    }

    protected fun hashCodeTraitDecl(decl: Declaration): MemberFunction? {
        if (decl !is IScalar) return null

        val signature = MemberFunction("size_t", "hashCode()", decl.name).const().noexcept()
        return when {
            decl is Toplevel -> return null
            decl.isConcrete -> signature.override()
            else -> signature.abstract().override()
        }
    }

    protected fun typenameTraitDecl(decl: Declaration): MemberFunction? {
        return if (decl !is Toplevel) {
            MemberFunction("std::string", "type_name()", decl.name).const().override()
        } else {
            null
        }
    }

    protected fun staticTypenameTraitDecl(decl: Declaration): MemberFunction? {
        return if (decl !is Toplevel) {
            MemberFunction("std::string", "static_type_name()", decl.name).static()
        } else {
            null
        }
    }

    private fun polymorphicToStringTraitDecl(decl: Declaration): MemberFunction {
        val signature = MemberFunction("std::string", "toString()", decl.name).const()
        return when {
            decl is Toplevel -> signature.override()
            decl.isAbstract || decl.isOpen -> signature.override()
            decl.isConcrete -> signature.override()
            else -> signature
        }
    }

    private fun externalToStringTraitDecl(decl: Declaration): MemberFunction {
        return MemberFunction("std::string", "to_string(const ${decl.name} & value)", null).friend()
    }

    protected fun PrettyPrinter.hashSpecialization(decl: Declaration) {
        if (decl !is IScalar) return
        if (decl is Enum) return

        println()
        comment("hash code trait")
        withNamespace("rd") {
            println()
            +"template <>"
            block( "struct hash<${decl.withNamespace()}> {", "};") {
                block("size_t operator()(const ${decl.withNamespace()} & value) const noexcept {", "}") {
                    +"return value.hashCode();"
                }
            }
            println()
        }
    }

    private fun PrettyPrinter.tupleSpecialization(decl: Declaration) {
        val n = decl.ownMembers.size
        withNamespace("std") {
            println()
            +"template <>"
            +"class tuple_size<${decl.withNamespace()}> : public integral_constant<size_t, $n> {};"
            println()
            +"""
                |template <size_t I>
                |class tuple_element<I, ${decl.withNamespace()}> {
                |public:
                |    using type = decltype (declval<${decl.withNamespace()}>().get<I>());
                |};""".trimMargin()
            println()
        }
    }

    //endregion

    //region definition
    protected open fun PrettyPrinter.libdef(decl: Toplevel, types: List<Declaration>) {
        registerSerializersTraitDef(decl, types)
    }

    protected fun PrettyPrinter.typedef(decl: Declaration) {
        comment("companion")
        companionTraitDef(decl)

        if (decl.isExtension) {
            comment("extension")
            extensionTraitDef(decl as Ext)
        }

        comment("constants")
        constantsDef(decl)

        comment("initializer")
        initializerTraitDef(decl)

        comment("primary ctor")
        primaryCtorTraitDef(decl)

        comment("secondary constructor")
        secondaryConstructorTraitDef(decl)

        comment("default ctors and dtors")
        defaultCtorsDtorsDef(decl)

        comment("reader")
        readerTraitDef(decl)

        comment("writer")
        writerTraitDef(decl)

        comment("virtual init")
        virtualInitTraitDef(decl)

        comment("identify")
        identifyTraitDef(decl)

        comment("getters")
        gettersTraitDef(decl)

        comment("intern")
        internTraitDef(decl)

        comment("equals trait")
        equalsTraitDef(decl)

        comment("equality operators")
        equalityOperatorsDef(decl)

        comment("hash code trait")
        hashCodeTraitDef(decl)

        comment("type name trait")
        typenameTraitDef(decl)

        comment("static type name trait")
        staticTypenameTraitDef(decl)

        comment("polymorphic to string")
        polymorphicToStringTraitDef(decl)

        comment("external to string")
        externalToStringTraitDef(decl)
    }

    private fun PrettyPrinter.readerBodyTrait(decl: Declaration) {
        fun IType.polymorphicReader() = "rd::Polymorphic<${templateName(decl)}>::read(ctx, buffer)"

        fun IType.reader(): String = when (this) {
            is Enum -> polymorphicReader()
            is InternedScalar -> {
                val lambda = lambda("rd::SerializationCtx &, rd::Buffer &", "return ${itemType.reader()}")
                """ctx.readInterned<${itemType.templateName(decl)}, ${internKey.hash()}>(buffer, $lambda)"""
            }
            is PredefinedType.void -> "rd::Void()" //really?
            is PredefinedType.rdId -> "rd::RdId()"
            is PredefinedType.bool -> "buffer.read_bool()"
            is PredefinedType.char -> "buffer.read_char()"
            is PredefinedType.string -> "buffer.read_wstring()"
            is PredefinedType.dateTime -> "buffer.read_date_time()"
            is PredefinedType.NativeIntegral, is PredefinedType.UnsignedIntegral -> "buffer.read_integral<${templateName(decl)}>()"
            is PredefinedType.NativeFloatingPointType -> "buffer.read_floating_point<${templateName(decl)}>()"
            is PredefinedType -> "buffer.read${name.capitalizeInvariant()}()"
            is Declaration -> {
                if (isIntrinsic) {
                    polymorphicReader()
                } else {
                    if (isAbstract || isOpen)
                        "ctx.get_serializers().readPolymorphic<${templateName(decl)}>(ctx, buffer)"
                    else
                        "${templateName(decl)}::read(ctx, buffer)"
                }
            }
            is INullable -> {
                val lambda = lambda(null, "return ${itemType.reader()}")
                """buffer.read_nullable<${itemType.templateName(decl)}>($lambda)"""
            }
            is IAttributedType -> itemType.reader()
            is IArray, is IImmutableList -> { //awaiting superinterfaces' support in Kotlin
                this as IHasItemType
                val templateTypes = "${decl.listType.withNamespace()}, ${itemType.templateName(decl)}, ${decl.allocatorType(itemType)}"
                if (isPrimitivesArray) {
                    "buffer.read_array<$templateTypes>()"
                } else {
                    """buffer.read_array<$templateTypes>(${lambda(null, "return ${itemType.reader()}")})"""
                }
            }
            else -> fail("Unknown declaration: $decl")
        }

        fun Member.reader(): String = when (this) {
            is Member.Field -> type.reader()
//            is Member.Reactive.Stateful.Extension -> "${ctorSubstitutedName(decl)}(${delegatedBy.reader()})"
            is Member.Reactive.Stateful.Extension -> {
                "${ctorSubstitutedName(decl)}{}"
            }
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
        if (decl is Class || decl is Aggregate) {
            +"auto _id = rd::RdId::read(buffer);"
        }
        (decl.membersOfBaseClasses + decl.ownMembers).println { "auto ${it.valName()} = ${it.reader()};" }
        if (unknown) {
            +"auto unknownBytes = rd::Buffer::ByteArray(objectStartPosition + size - buffer.get_position());"
            +"buffer.read_byte_array_raw(unknownBytes);"
        }
        val ctorParams = decl.allMembers.asSequence().map { "std::move(${it.valName()})" }.plus(unknownMemberNames(decl)).joinToString(", ")
//        p("return ${decl.name}($ctorParams)${(decl is Class && decl.isInternRoot).condstr { ".apply { mySerializationContext = ctx }" }}")
        +"${decl.name} res{${ctorParams.isNotEmpty().condstr { ctorParams }}};"
        if (decl is Class || decl is Aggregate) {
            +"withId(res, _id);"
        }
        if (decl is Class && decl.isInternRoot) {
            +"res.mySerializationContext = ${decl.withInternRootsHere("res")};"
        }
        if (unknown) {
            +"return rd::Wrapper<${decl.name}>(std::move(res));"
        } else {
            +"return res;"
        }
    }

    private fun lambda(args: String?, body: String, resultType: String? = null): String {
        val typeHint = resultType?.let { " -> $resultType" } ?: ""
        return "$eol[&ctx, &buffer](${args ?: ""}) mutable $typeHint $eol{ $body; }$eol"
    }
//endregion

//region TraitDef

    protected fun PrettyPrinter.registerSerializersTraitDef(decl: Toplevel, types: List<Declaration>) {//todo name access
        val serializersOwnerImplName = "${decl.name}SerializersOwner"
        +"${decl.name}::$serializersOwnerImplName const ${decl.name}::serializersOwner;"
        println()
        define(MemberFunction("void", "registerSerializersCore(rd::Serializers const& serializers)", "${decl.name}::${decl.name}SerializersOwner").const().override()) {
            types.filter { !(it.isAbstract || it.isOpen) }
                .filterIsInstance<IType>()
                .filterNot { iType -> iType is Enum }
                .filterNot { iType -> iType is Declaration && iType.isIntrinsic }
                .println {
                    "serializers.registry<${it.name}>();"
                }

            if (decl is Root) {
                decl.toplevels.minus(decl).println {
                    val name = it.sanitizedName(decl)
                    "$name::serializersOwner.registry(serializers);"
                }
                //todo mark graph vertex
            }
        }
    }

    //only for toplevel Exts
    protected fun PrettyPrinter.createMethodTraitDef(decl: Toplevel) {
        define(createMethodTraitDecl(decl)) {
            +"${decl.root.sanitizedName(decl)}::serializersOwner.registry(protocol->get_serializers());"
            println()

//            +"${decl.name} res;"
            val quotedName = "\"${decl.name}\""
            +"identify(*(protocol->get_identity()), rd::RdId::Null().mix($quotedName));"
            +"bind(lifetime, protocol, $quotedName);"
//            +"return res;"
        }
    }

    protected fun PrettyPrinter.companionTraitDef(decl: Declaration) {
        /*if (decl.isAbstract) {
            println()
            abstractDeclarationTraitDef(decl)
        }*/
        if (decl is Toplevel) {
            println()
            registerSerializersTraitDef(decl, decl.declaredTypes + unknowns(decl.declaredTypes))
            println()
            createMethodTraitDef(decl)

            println()
        }
    }


    fun PrettyPrinter.primaryCtorTraitDef(decl: Declaration) {
        define(primaryCtorTraitDecl(decl)) {
            +"initialize();"
        }
    }

    private fun PrettyPrinter.secondaryConstructorTraitDef(decl: Declaration) {
        define(secondaryConstructorTraitDecl(decl)) {
            +"initialize();"
        }
    }

    fun PrettyPrinter.defaultCtorsDtorsDef(decl: Declaration) {
        define(decl.defaultCtor()) {
            +"initialize();"
        }
    }


    protected fun PrettyPrinter.readerTraitDef(decl: Declaration) {
        define(readerTraitDecl(decl)) {
            if (decl.isConcrete) {
                if (isUnknown(decl)) {
                    +"""throw std::logic_error("Unknown instances should not be read via serializer");"""
                } else {
                    readerBodyTrait(decl)
                }
            } else if (decl.isAbstract || decl.isOpen) {
                readerBodyTrait(unknown(decl)!!)
            }
        }
    }

/*
    protected fun IType.nestedOf(decl: Declaration): Boolean {
        val iType = decl.parseType(this, true)
        return iType?.name == decl.name
    }
*/

    protected fun PrettyPrinter.writerTraitDef(decl: Declaration) {
        fun IType.polymorphicWriter(field: String) = "rd::Polymorphic<${templateName(decl)}>::write(ctx, buffer, $field)"

        fun IType.writer(field: String): String {
            return when (this) {
                is CppIntrinsicType -> polymorphicWriter(field)
                is Enum -> polymorphicWriter(field)
                is InternedScalar -> {
                    val lambda = lambda("rd::SerializationCtx &, rd::Buffer &, ${itemType.substitutedName(decl)} const & internedValue", itemType.writer("internedValue"), "void")
                    """ctx.writeInterned<${itemType.templateName(decl)}, ${internKey.hash()}>(buffer, $field, $lambda)"""
                }
                is PredefinedType.void -> "" //really?
                is PredefinedType.bool -> "buffer.write_bool($field)"
                is PredefinedType.char -> "buffer.write_char($field)"
                is PredefinedType.string -> "buffer.write_wstring($field)"
                is PredefinedType.dateTime -> "buffer.write_date_time($field)"
                is PredefinedType.NativeIntegral, is PredefinedType.UnsignedIntegral -> "buffer.write_integral($field)"
                is PredefinedType.NativeFloatingPointType -> "buffer.write_floating_point($field)"
                is Declaration ->
                    if (isAbstract || isOpen)
                        "ctx.get_serializers().writePolymorphic<${templateName(decl)}>(ctx, buffer, $field)"
                    else {
                        "rd::Polymorphic<std::decay_t<decltype($field)>>::write(ctx, buffer, $field)"
                    }
                is INullable -> {
                    val lambda = lambda("${itemType.substitutedName(decl)} const & it", itemType.writer("it"), "void")
                    "buffer.write_nullable<${itemType.templateName(decl)}>($field, $lambda)"
                }
                is IAttributedType -> itemType.writer(field)
                is IArray, is IImmutableList -> { //awaiting superinterfaces' support in Kotlin
                    this as IHasItemType
                    if (isPrimitivesArray) {
                        "buffer.write_array($field)"
                    } else {
                        val templateTypes = "${decl.listType.withNamespace()}, ${itemType.templateName(decl)}, ${decl.allocatorType(itemType)}"
                        val lambda = lambda("${itemType.templateName(decl)} const & it", itemType.writer("it"), "void")
                        "buffer.write_array<$templateTypes>($field, $lambda)"
                    }
                }
                else -> fail("Unknown declaration: $decl")
            }
        }


        fun Member.writer(): String = when (this) {
            is Member.Field -> type.writer(encapsulatedName)
//            is Member.Reactive.Stateful.Extension -> delegatedBy.writer((encapsulatedName))//todo
            is Member.Reactive.Stateful.Extension -> ""
            is Member.Reactive -> "$encapsulatedName.write(ctx, buffer)"

            else -> fail("Unknown member: $this")
        }

        if (decl.isConcrete) {
            define(writerTraitDecl(decl)) {
                if (decl is Class || decl is Aggregate) {
                    +"this->rdid.write(buffer);"
                }
                (decl.membersOfBaseClasses + decl.ownMembers).println { member -> member.writer() + ";" }
                if (isUnknown(decl)) {
                    +"buffer.write_byte_array_raw(unknownBytes_);"
                }
                if (decl is Class && decl.isInternRoot) {
                    +"this->mySerializationContext = ${decl.withInternRootsHere("*this")};"
                }
            }
        } else {
            //todo ???
        }
    }

    fun PrettyPrinter.virtualInitTraitDef(decl: Declaration) {
        virtualInitTraitDecl(decl)?.let {
            define(it) {
                val base = "rd::" + (if (decl is Toplevel) "RdExtBase" else "RdBindableBase")
                +"$base::init(lifetime);"
                decl.ownMembers
                    .filter { it.isBindable }
                    .println { """bindPolymorphic(${it.encapsulatedName}, lifetime, this, "${it.name}");""" }
            }
        }
    }

    fun PrettyPrinter.identifyTraitDef(decl: Declaration) {
        identifyTraitDecl(decl)?.let {
            define(it) {
                +"rd::RdBindableBase::identify(identities, id);"
                decl.ownMembers
                    .filter { it.isBindable }
                    .println { """identifyPolymorphic(${it.encapsulatedName}, identities, id.mix(".${it.name}"));""" }
            }
        }
    }

    protected fun PrettyPrinter.gettersTraitDef(decl: Declaration) {
        gettersTraitDecl(decl).zip(decl.ownMembers) { s: MemberFunction, member: Member ->
            define(s) {
                p(docComment(member.documentation))
                val unwrap = when {
                    member is Member.Reactive -> false
                    member is IBindable -> true
                    member is Member.Field && member.type is Struct && !member.type.isIntrinsic -> true
                    member is Member.Field && member.type is Class && !member.type.isIntrinsic -> true
                    member is Member.Field && member.type is PredefinedType.string -> true
                    member is Member.Field && member.type is InternedScalar && member.type.itemType is PredefinedType.string -> true
                    else -> false
                }
                val star = unwrap.condstr { "*" }
                +"return $star${member.encapsulatedName};"
            }
        }
    }

    protected fun PrettyPrinter.internTraitDef(decl: Declaration) {
        define(internTraitDecl(decl)) {
            +"""if (mySerializationContext) {
                    |   return *mySerializationContext;
                    |} else {
                    |   throw std::invalid_argument("Attempting to get serialization context too soon for");
                    |}""".trimMargin()
        }
    }

    private fun PrettyPrinter.constantsDef(decl: Declaration) {
        decl.constantMembers.forEach {
            val type = when (it.type) {
                is PredefinedType.string -> "rd::wstring_view"
                else -> it.type.templateName(decl)
            }
            +"constexpr $type ${decl.name}::${it.name};"
        }
    }

    protected fun PrettyPrinter.initializerTraitDef(decl: Declaration) {
        define(initializerTraitDecl(decl)) {
            decl.ownMembers
                .filterIsInstance<Member.Reactive.Stateful>()
                .filter { it !is Member.Reactive.Stateful.Extension && it.genericParams.none { it is IBindable } }
                .println { "${it.encapsulatedName}.optimize_nested = true;" }

            decl.ownMembers
                .filterIsInstance<Member.Reactive>()
                .filter { it.freeThreaded }
                .println { "${it.encapsulatedName}.async = true;" }

            if (decl is Toplevel) {
                +"serializationHash = ${decl.serializationHash(IncrementalHash64()).result}L;"
            }
        }
    }


    private fun PrettyPrinter.equalsTraitDef(decl: Declaration) {
        define(equalsTraitDecl(decl)) {
            +"auto const &other = dynamic_cast<${decl.name} const&>(object);"
            if (decl.isAbstract || decl.isOpen || decl !is IScalar) {
                +"return this == &other;"
            } else {
                +"if (this == &other) return true;"

                decl.allMembers.println { m ->
                    val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                    f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")

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

    private fun PrettyPrinter.equalityOperatorsDef(decl: Declaration) {
        titledBlock("bool operator==(const ${decl.name} &lhs, const ${decl.name} &rhs)") {
            when (decl) {
                !is IScalar -> +"return &lhs == &rhs;"
                else -> {
                    +"if (lhs.type_name() != rhs.type_name()) return false;"
                    +"return lhs.equals(rhs);"
                }
            }
        }

        p("bool operator!=(const ${decl.name} &lhs, const ${decl.name} &rhs)")
        braceBlock {
            +"return !(lhs == rhs);"
        }
    }

    protected fun PrettyPrinter.hashCodeTraitDef(decl: Declaration) {
        fun IScalar.hc(v: String): String = when (this) {
            is IArray, is IImmutableList ->
                if (isPrimitivesArray) "rd::contentHashCode($v)"
                else "rd::contentDeepHashCode($v)"
            is INullable -> {
                "(static_cast<bool>($v)) ? " + (itemType as IScalar).hc("*$v") + " : 0"
            }
            is ScalarAttributedType<IScalar> -> itemType.hc(v)
            else -> {
                if (this.isAbstract()) {
                    "rd::hash<${this.templateName(decl)}>()($v)"
                } else {
                    "rd::hash<${this.templateName(decl)}>()($v)"
                }
            }
        }

        hashCodeTraitDecl(decl)?.let {
            define(it) {
                +"size_t __r = 0;"

                decl.allMembers.println { m: Member ->
                    val f = m as? Member.Field ?: fail("Must be field but was `$m`")
                    val t = f.type as? IScalar ?: fail("Field $decl.`$m` must have scalar type but was ${f.type}")
                    if (f.usedInEquals)
                        "__r = __r * 31 + (${t.hc("""${f.getter()}()""")});"
                    else
                        ""
                }

                +"return __r;"
            }
        }
    }

    protected fun PrettyPrinter.typenameTraitDef(decl: Declaration) {
        typenameTraitDecl(decl)?.let {
            define(it) {
                +"""return "${decl.name}";"""
            }
        }
    }

    protected fun PrettyPrinter.staticTypenameTraitDef(decl: Declaration) {
        staticTypenameTraitDecl(decl)?.let {
            define(it) {
                +"""return "${decl.name}";"""
            }
        }
    }

    private fun PrettyPrinter.polymorphicToStringTraitDef(decl: Declaration) {
        polymorphicToStringTraitDecl(decl).let { function ->
            define(function) {
                +"""std::string res = "${decl.name}\n";"""
                decl.allMembers.forEach { member ->
                    println("""res += "\t${member.name} = ";""")
                    println("""res += rd::to_string(${member.encapsulatedName});""")
                    println("""res += '\n';""")
                }
                +"return res;"
            }
        }
    }

    private fun PrettyPrinter.externalToStringTraitDef(decl: Declaration) {
        externalToStringTraitDecl(decl).let {
            define(it) {
                +"return value.toString();"
            }
        }
    }


    protected fun PrettyPrinter.extensionTraitDef(decl: Ext) {//todo
        define(extensionTraitDecl(decl)) {
            val lowerName = decl.name.decapitalizeInvariant()
            +"""return pointcut.getOrCreateExtension<${decl.name}>("$lowerName");"""
            println()
        }

    }
//endregion

    //region unknowns
    protected fun isUnknown(decl: Declaration) =
        decl is Class.Concrete && decl.isUnknown ||
            decl is Struct.Concrete && decl.isUnknown

    protected fun unknownMembers(decl: Declaration): List<Member.Field> =
        if (isUnknown(decl)) listOf(
            Member.Field("unknownId", RdId),
            Member.Field("unknownBytes", ByteArray))//todo bytearray
        else emptyList()

    private fun unknownMembersSecondary(decl: Declaration) = unknownMembers(decl)

    protected fun unknownMemberNames(decl: Declaration) = unknownMembers(decl).map { it.name }


    override fun unknown(it: Declaration): Declaration? = super.unknown(it)?.setting(PublicCtors)
//endregion

    protected fun docComment(doc: String?) = (doc != null).condstr {
        "\n" +
            "/**" + eol +
            " * $doc" + eol +
            " */" + eol
    }

    protected fun getDefaultValue(member: Member): String? {
        fun unwrapConstant(c: Member.Const): String {
            val name = c.name
            return when (c.type) {
                is PredefinedType.string -> "$name.data()"
                else -> name
            }
        }

        return when (member) {
            is Member.Reactive.Stateful.PropertyBase -> when {
                member.defaultValue is String -> "L\"${member.defaultValue}\""
                member.defaultValue is Member.Const -> unwrapConstant(member.defaultValue)
                member.defaultValue != null -> {
                    val default = member.defaultValue.toString()
                    if (member.genericParams[0] is PredefinedType.string) {
                        "L$default"
                    } else {
                        default
                    }
                }
                else -> null
            }
            is Member.Const -> {
                val value = member.value
                when (member.type) {
                    is PredefinedType.char -> "L'$value'"
                    is PredefinedType.string -> "L\"$value\", ${value.length}"
                    is PredefinedType.long -> "${value}ll"
                    is PredefinedType.uint -> "${value}u"
                    is PredefinedType.ulong -> "${value}ull"
                    is PredefinedType.float -> "${value}f"
                    is Enum -> "${member.type.name}::${sanitize(value)}"
                    else -> value
                }
            }
            //                is Member.Reactive.Stateful.Extension -> member.delegatedBy.sanitizedName(containing) + "()"
            else -> null
        }
    }

    private fun PrettyPrinter.ifDefDirective(feature: String, printer: PrettyPrinter.() -> Unit) {
        +"#ifdef $feature"
        printer()
        +"#endif"
    }

    private fun Member.Reactive.Stateful.Extension.factoryFqn(scope: Declaration) : String {
        val delegate = findDelegate() ?: return javaClass.simpleName
        return delegate.factoryFqn ?: this.delegateFqnSubstitutedName(scope)
    }

    private fun Member.Reactive.Stateful.Extension.findDelegate() = findDelegate(this@Cpp17Generator, flowTransform)

    protected open fun Member.Reactive.Stateful.Extension.delegateFqnSubstitutedName(scope: Declaration): String = findDelegate()?.fqnSubstitutedName(scope)
        ?: this.javaClass.simpleName

    protected open fun Member.ExtensionDelegate.fqnSubstitutedName(scope: Declaration): String = this.delegateType.let { delegateType ->
        when (delegateType) {
            is Member.DelegateType.Custom -> delegateType.fqn
            is Member.DelegateType.Delegated -> delegateType.type.substitutedName(scope)
        }
    }

    override fun toString(): String {
        return "Cpp17Generator($flowTransform, \"$defaultNamespace\", '${folder.canonicalPath}')"
    }
}
