package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.Enum
import com.jetbrains.rd.generator.nova.FlowKind.*
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator.Companion.Features.__cpp_structured_bindings
import com.jetbrains.rd.generator.nova.cpp.Signature.Constructor
import com.jetbrains.rd.generator.nova.cpp.Signature.MemberFunction
import com.jetbrains.rd.generator.nova.util.joinToOptString
import com.jetbrains.rd.util.eol
import com.jetbrains.rd.util.hash.IncrementalHash64
import com.jetbrains.rd.util.string.Eol
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.condstr

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

/*please set VsWarningsDefault to null if you don't need disabling VS warnings
val VsWarningsDefault : IntArray? = null*/
val VsWarningsDefault: IntArray? = intArrayOf(4250, 4307, 4267, 4244)

/**
 * Generate C++ code.
 * @param defaultNamespace namespace separated by symbol "point", which will be translated to nested namespaces. "a.b.c" to "a::b::c", for instance.
 * Remember about following properties: "FsPath", "TargetName"!
 */
open class Cpp17Generator(override val flowTransform: FlowTransform,
                          val defaultNamespace: String,
                          override val folder: File,
                          val usingPrecompiledHeaders: Boolean = false
) : GeneratorBase() {
    @Suppress("ObjectPropertyName")
    companion object {
        //        private const val INSTANTIATION_FILE_NAME = "instantiations"
        object LanguageVersion {
            const val `C++11` = "201103L"
            const val `C++14` = "201402L"
            const val `C++17` = "201703L"
            const val `C++20` = "202000L"
        }

        object Features {
            const val __cpp_structured_bindings = "__cpp_structured_bindings"
        }
    }

    //region language specific properties
    object Namespace : ISetting<String, Declaration>

    private val Declaration.namespace: String
        get() {
            return if (this is FakeDeclaration) {
                decl.namespace
            } else {
                val ns = getSetting(Namespace) ?: defaultNamespace
                ns.split('.').joinToString(separator = "::")
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
        get() {
            return getSetting(Intrinsic) != null
        }

    object Intrinsic : ISetting<CppIntrinsicType, Declaration>

    object MarshallerHeaders : SettingWithDefault<List<String>, Toplevel>(listOf())

    val nestedNamespaces = defaultNamespace.split('.')

    object PublicCtors : ISetting<Unit, Declaration>

    object MasterStateful : ISetting<Boolean, Declaration>

    private val Member.Reactive.Stateful.Property.master: Boolean
        get() = owner.getSetting(MasterStateful) ?: this@Cpp17Generator.master

    private val Member.Reactive.Stateful.Map.master: Boolean
        get() = owner.getSetting(MasterStateful) ?: this@Cpp17Generator.master

    object FsPath : ISetting<(Cpp17Generator) -> File, Toplevel>

    object TargetName : ISetting<String, Toplevel>

    private fun Declaration.fsName(isDefinition: Boolean) =
            "$name.${if (isDefinition) "cpp" else "h"}"

    protected open fun Toplevel.fsPath(): File = getSetting(FsPath)?.invoke(this@Cpp17Generator)
            ?: File(folder, this.name)


    protected open fun Declaration.fsPath(tl: Toplevel, isDefinition: Boolean): File = getSetting(FsPath)?.invoke(this@Cpp17Generator)
            ?: File(tl.fsPath(), fsName(isDefinition))

    private fun Root.targetName(): String {
        return getSetting(TargetName) ?: this.name
    }

    private val Class.isInternRoot: Boolean
        get() = internRootForScopes.isNotEmpty()

    private fun InternScope.hash(): String {
        val s = this.keyName
        return """rd::util::getPlatformIndependentHash("$s")"""
    }

    private fun Class.withInternRootsHere(field: String): String {
        val roots = internRootForScopes/*.map { """rd::util::getPlatformIndependentHash("$it")""" }*/.joinToString { "\"$it\"" }
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
        return this.name.includeWithExtension("h")
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

    protected fun PrettyPrinter.braceBlock(body: PrettyPrinter.() -> Unit) {
        +"{"
        indent(body)
        +"}"
    }

    protected fun PrettyPrinter.titledBlock(title: String, body: PrettyPrinter.() -> Unit) {
        +"$title {"
        indent(body)
        +"};"
    }

    protected fun PrettyPrinter.comment(comment: String) {
        +"${eolKind.value}//$comment"
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

    private fun PrettyPrinter.private() {
        +"private:"
    }

    private fun PrettyPrinter.protected() {
        +"protected:"
    }

    private fun PrettyPrinter.public() {
        +"public:"
    }

    private fun Member.getter() = "get_${this.publicName}"

    private fun PrettyPrinter.surroundWithNamespaces(body: PrettyPrinter.() -> Unit) {
        nestedNamespaces.foldRight(body) { s, acc ->
            {
                titledBlock("namespace $s") {
                    acc()
                }
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
            if (rawType) {
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
                when (itemType) {
                    is PredefinedType.string -> substitutedName.wrapper()
                    is PredefinedType -> substitutedName.optional()
                    is Enum -> substitutedName.optional()
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
        is IArray -> "std::vector<${itemType.substitutedName(scope, false, omitNullability)}>"
        is IImmutableList -> "std::vector<${itemType.substitutedName(scope, false, omitNullability)}>"

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
        is PredefinedType.guid -> "UUID"
        is PredefinedType.uri -> "URI"
        is PredefinedType.secureString -> "RdSecureString"
        is PredefinedType.void -> "rd::Void"
        is PredefinedType -> name.decapitalize()
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
                if (isAbstract) {
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
                ?: isUsage.condstr { "$className::" } + when (this) {
                    is InternedScalar -> "__${name}At${internKey.keyName}Serializer"
                    else -> "__${name}Serializer"
                }
    }

//endregion

    //region Member.
    val Member.Reactive.actualFlow: FlowKind get() = flowTransform.transform(flow)

    protected open val Member.Reactive.intfSimpleName: String
        get() {
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

            }
        }

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    protected open val Member.Reactive.implSimpleName: String
        get() = "rd::" + when (this) {
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
            is Member.Reactive.Stateful.Extension -> fqn(this@Cpp17Generator, flowTransform)

            else -> fail("Unsupported member: $this")
        }


    protected open val Member.Reactive.ctorSimpleName: String
        get() = when (this) {
            is Member.Reactive.Stateful.Extension -> factoryFqn(this@Cpp17Generator, flowTransform)
            else -> implSimpleName
        }

    protected open fun Member.intfSubstitutedName(scope: Declaration) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.templateName(scope)
        is Member.Reactive -> intfSimpleName + (genericParams.toList().map { it.templateName(scope) }).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")
        is Member.Const -> type.templateName(scope)
    }

    protected open fun Member.implSubstitutedName(scope: Declaration) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.substitutedName(scope)
        is Member.Reactive -> {
            implSimpleName + (genericParams.toList().map { it.templateName(scope) } + customSerializers(scope, false)).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")
            /*val isProperty = (this is Member.Reactive.Stateful.Property)
            implSimpleName + (genericParams.toList().map
            { it.substitutedName(scope, omitNullability = isProperty) } + customSerializers(scope)).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")*/
        }
        is Member.Const -> type.substitutedName(scope)
    }

    protected open fun Member.implTemplateName(scope: Declaration) = when (this) {
        is Member.EnumConst -> fail("Code must be unreachable for ${javaClass.simpleName}")
        is Member.Field -> type.templateName(scope)
        is Member.Reactive -> {
            implSimpleName + (genericParams.toList().map { it.templateName(scope) } + customSerializers(scope, false)).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")
            /*val isProperty = (this is Member.Reactive.Stateful.Property)
            implSimpleName + (genericParams.toList().map
            { it.templateName(scope, isProperty) } + customSerializers(scope)).toTypedArray().joinToOptString(separator = ", ", prefix = "<", postfix = ">")*/
        }
        is Member.Const -> type.templateName(scope)
    }


    protected open fun Member.ctorSubstitutedName(scope: Declaration) = when (this) {
        is Member.Reactive.Stateful.Extension -> {
            "rd::" + ctorSimpleName + genericParams.joinToOptString(separator = ", ", prefix = "<", postfix = ">") { it.templateName(scope) }
        }
        else -> implSubstitutedName(scope)
    }


    protected open val Member.isBindable: Boolean
        get() = when (this) {
            is Member.Field -> type is IBindable
            is Member.Reactive -> true

            else -> false
        }


    open val Member.publicName: String get() = name
    open val Member.encapsulatedName: String get() = "${publicName}_"
    open val Member.isEncapsulated: Boolean get() = this is Member.Reactive

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
            is Member.Reactive -> true

            else -> fail("Unsupported member: $this")
        }
    //endregion

    private class FakeDeclaration(val decl: Declaration) : Declaration(null) {
        override val _name: String
            get() = decl.name
        override val cl_name: String
            get() = decl.name
    }

    //region Declaration.
    protected fun Declaration.sanitizedName(scope: Declaration): String {
        return if (scope is FakeDeclaration) {
            this.withNamespace()
        } else {
            val needQualification = namespace != scope.namespace
            needQualification.condstr { "$namespace::" } + platformTypeName
        }
    }

    private fun Declaration.withNamespace(): String {
        return "$namespace::$name"
    }


    protected fun Declaration.scopeResolution(): String {
        return "$name::"
    }

    internal fun bases(declaration: Declaration): MutableList<BaseClass> {
        val baseName = declaration.baseNames()
        return if (declaration.base == null) {
            val result = mutableListOf<BaseClass>()
            if (declaration !is Toplevel) {
//                result.add("rd::IPolymorphicSerializable" + withMembers.condstr { "()" })
                result.add(BaseClass(IPolymorphicSerializable, emptyList()))
            }
//            baseName?.let { result.add(it) }
            result.addAll(baseName)
            result
        } else {
            val result = baseName.toMutableList()
            if (isUnknown(declaration)) {
//                result.add("rd::IUnknownInstance" + withMembers.condstr { "(std::move(unknownId))" })
                result.add(BaseClass(IUnknownInstance, listOf(Member.Field("unknownId", RdId))))
            }
            result
        }
//        return listOf("ISerializable" + withMembers.condstr { "()" }) + (baseName?.let { listOf(it) } ?: emptyList())
    }

    protected fun Declaration.baseNames(): List<BaseClass> {
        /*return this.base?.let {
            it.sanitizedName(this) + withMembers.condstr {
                "(${it.allMembers.joinToString(", ") { member -> "std::move(${member.encapsulatedName})" }})"
            }
        } ?: (
                (if (this is Toplevel) "rd::RdExtBase"
                else if (this is Class || this is Aggregate || this is Toplevel) "rd::RdBindableBase"
//            else if (decl is Struct) p(" : IPrintable")
                else null)?.plus(withMembers.condstr { "()" }))*/
        return this.base?.let {
            mutableListOf(BaseClass(it as IType, it.allMembers))
            /*it.sanitizedName(this) + withMembers.condstr {
                "(${it.allMembers.joinToString(", ") { member -> "std::move(${member.encapsulatedName})" }})"
            }*/
        } ?: (
                if (this is Toplevel)
                    listOf(BaseClass(RdExtBase, emptyList()))
                else if (this is Class || this is Aggregate || this is Toplevel)
                    listOf(BaseClass(RdBindableBase, emptyList()))
//                else if (decl is Struct) p(" : IPrintable")
                else listOf()
                )
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

    private val Declaration.hasSecondaryCtor: Boolean get() = (this is Toplevel || this.isConcrete) && this.allMembers.any { it.hasEmptyConstructor }
//endregion

    private fun File.cmakeLists(targetName: String, fileNames: List<String>, toplevelsDependencies: List<Toplevel> = emptyList(), subdirectories: List<String> = emptyList()) {
        mkdirs()
        File(this, "CMakeLists.txt").run {
            printWriter().use {
                it.apply {
                    println("cmake_minimum_required(VERSION 3.7)")

                    val pchCppFile = "pch.cpp"
                    val onOrOff = if (usingPrecompiledHeaders) "ON" else "OFF"
                    val conditionalVariable = "ENABLE_PCH_HEADERS_FOR_$targetName"

                    println("option($conditionalVariable \"Enable precompiled headers\" $onOrOff)")
                    println("""
                        |if ($conditionalVariable)
                        |    set(PCH_CPP_OPT $pchCppFile)
                        |else ()
                        |    set(PCH_CPP_OPT "")
                        |endif ()""".trimMargin()
                    )
                    val targetFiles = fileNames + listOf(
                            /*"${INSTANTIATION_FILE_NAME}.h",
                            "${INSTANTIATION_FILE_NAME}.cpp",*/
                            "\${PCH_CPP_OPT}")

                    println("add_library($targetName STATIC ${targetFiles.joinToString(separator = eol)})")
                    val toplevelsDirectoryList = toplevelsDependencies.joinToString(separator = eol) { it.name }
//                    val toplevelsLibraryList = toplevelsDependencies.joinToString(separator = " ") { name }
                    println(subdirectories.joinToString(separator = eol) { s -> "add_subdirectory($s)" })
                    println("target_include_directories($targetName PUBLIC \${CMAKE_CURRENT_SOURCE_DIR} $toplevelsDirectoryList)")
                    println("target_link_libraries($targetName PUBLIC rd_framework_cpp)")
//                println("target_link_directories($targetName PUBLIC rd_framework_cpp $toplevelsLibraryList)")
                    println("""
                            |if ($conditionalVariable)
                            |    include(PrecompiledHeader.cmake)
                            |    add_precompiled_header(${targetName} pch.h SOURCE_CXX ${pchCppFile} FORCEINCLUDE)
                            |endif ()""".trimMargin()
                    )
                }
            }
        }
    }

/*
    private fun File.templateInstantiate() {
        val classes = listOf(
//                "rd::optional<rd::SerializationCtx>"
                "rd::Wrapper<std::wstring>"
        )

        File(this, "${INSTANTIATION_FILE_NAME}.cpp").run {
            printWriter().use { writer ->
                PrettyPrinter().apply {
                    +"wrapper".include("h")
                    +"string".include()

                    classes.forEach {
                        +"template class $it;"
                    }
                    writer.write(toString())
                }
            }
        }

        File(this, "${INSTANTIATION_FILE_NAME}.h").run {
            printWriter().use { writer ->
                PrettyPrinter().apply {
                    classes.forEach {
                        +"extern template class $it;"
                    }
                    writer.write(toString())
                }
            }
        }
    }
*/

    override fun generate(root: Root, clearFolderIfExists: Boolean, toplevels: List<Toplevel>) {
        prepareGenerationFolder(folder, clearFolderIfExists)

        val allFilePaths = emptyList<String>().toMutableList()

        toplevels.sortedBy { it.name }.forEach { tl ->
            val directory = tl.fsPath()
            directory.mkdirs()
            val types = (tl.declaredTypes + tl + unknowns(tl.declaredTypes)).filter { !it.isIntrinsic }
            val fileNames = types.map { it.fsName(true) } + types.map { it.fsName(false) }
            allFilePaths += fileNames.map { "${tl.name}/$it" }

            val marshallerHeaders = tl.getSetting(MarshallerHeaders) ?: listOf()
//            directory.cmakeLists(tl.name, fileNames)
            for (type in types) {
                listOf(false, true).forEach { isDefinition ->
                    type.fsPath(tl, isDefinition).run {
                        bufferedWriter().use { writer ->
                            PrettyPrinter().apply {
                                eolKind = Eol.osSpecified
                                step = 4

                                //actual generation

                                if (isDefinition) {
                                    source(type, types)
                                } else {
                                    header(type, marshallerHeaders)
                                }

                                writer.write(toString())
                            }
                        }
                    }
                }

            }


        }

        folder.cmakeLists(root.targetName(), allFilePaths, toplevels/*, toplevels.map { it.name }*/)
//        folder.templateInstantiate()
    }


    //region files
    fun PrettyPrinter.header(decl: Declaration, marshallerHeaders: List<String>) {
        val includeGuardMacro = "${decl.name.toUpperCase()}_H"
        +"#ifndef $includeGuardMacro"
        +"#define $includeGuardMacro"
        println()

        includesDecl(marshallerHeaders)
        println()

        dependenciesDecl(decl)
        println()

        VsWarningsDefault?.let {
            +"#pragma warning( push )"
            it.forEach {
                +"#pragma warning( disable:$it )"
            }
        }

        if (decl is Toplevel && decl.isLibrary) {
            comment("library")
            surroundWithNamespaces {
                libdecl(decl)
            }
        } else {
            typedecl(decl)
        }
        println()

        +"#endif // $includeGuardMacro"
    }

    fun PrettyPrinter.source(decl: Declaration, dependencies: List<Declaration>) {
        +decl.includeWithExtension()

        println()

        if (decl is Enum) {
            surroundWithNamespaces {
                enumToStringTraitDef(decl)
            }
            return
        }

        if (decl is Toplevel) {
            dependencies.filter { !it.isAbstract }.filterIsInstance<IType>().println {
                if (it is Declaration) {
                    val name = it.name
                    "../${it.pointcut!!.name}/$name".includeWithExtension()
                } else {
                    it.includeWithExtension()
                }
            }
        }
        println()
        if (decl is Toplevel) {
            val rootName = decl.root.sanitizedName(decl)
            +"../$rootName/$rootName".includeWithExtension()
        }
        if (decl.isAbstract) {
            +(unknown(decl)!!.includeWithExtension())
        }
        if (decl is Root) {
            decl.toplevels.forEach {
                val name = it.name
                +"../$name/$name".includeWithExtension()
            }
        }
        if (decl is Toplevel && decl.isLibrary) {
            surroundWithNamespaces { libdef(decl, decl.declaredTypes + unknowns(decl.declaredTypes)) }
        } else {
            surroundWithNamespaces { typedef(decl) }
        }
    }
//endregion

    //region declaration
    protected open fun PrettyPrinter.libdecl(decl: Declaration) {
        titledBlock("class ${decl.name}") {
            registerSerializersTraitDecl(decl)
        }
    }

    protected open fun PrettyPrinter.typedecl(decl: Declaration) {
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

        surroundWithNamespaces {
            if (decl is Enum) {
                enum(decl)
                return@surroundWithNamespaces
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

                comment("constants")
                public()
                constantsDecl(decl)

                comment("fields")
                protected()
                fieldsDecl(decl)

                comment("initializer")
                private()
                declare(initializerTraitDecl(decl))

                comment("primary ctor")
                //            +(decl.primaryCtorVisibility)
                public()
                declare(primaryCtorTraitDecl(decl))

                comment("secondary constructor")
                declare(secondaryConstructorTraitDecl(decl))

                ifDefDirective(__cpp_structured_bindings) {
                    comment("deconstruct trait")
                    deconstructTrait(decl)
                }

                comment("default ctors and dtors")
                defaultCtorsDtorsDecl(decl)

                comment("reader")
                declare(readerTraitDecl(decl))

                comment("writer")
                declare(writerTraitDecl(decl))

                comment("virtual init")
                declare(virtualInitTraitDecl(decl))

                comment("identify")
                declare(identifyTraitDecl(decl))

                comment("getters")
                declare(gettersTraitDecl(decl))

                comment("intern")
                declare(internTraitDecl(decl))

                comment("equals trait")
                private()
                declare(equalsTraitDecl(decl))

                comment("equality operators")
                public()
                equalityOperatorsDecl(decl)

                comment("hash code trait")
                declare(hashCodeTraitDecl(decl))

                comment("type name trait")
                declare(typenameTraitDecl(decl))

                comment("static type name trait")
                declare(staticTypenameTraitDecl(decl))

                comment("polymorphic to string")
                private()
                declare(polymorphicToStringTraitDecl(decl))

                comment("external to string")
                public()
                declare(externalToStringTraitDecl(decl))

                /*if (decl.isExtension) {
                    extensionTraitDef(decl as Ext)
                }*/
            }
        }

//        externTemplates(decl)

        VsWarningsDefault?.let {
            println()
            +"#pragma warning( pop )"
            println()
        }

        comment("hash code trait")
        hashSpecialization(decl)

        ifDefDirective(__cpp_structured_bindings) {
            comment("tuple trait")
            tupleSpecialization(decl)
        }
    }

    protected open fun PrettyPrinter.enum(decl: Enum) {
        titledBlock("enum class ${decl.name}") {
            +decl.constants.withIndex().joinToString(separator = ",${eolKind.value}") { (idx, enumConst) ->
                docComment(enumConst.documentation) + enumConst.name.sanitize() + decl.flags.condstr { " = 1 << $idx" }
            }
        }

        if (decl.flags) {
            +"DEFINE_ENUM_FLAG_OPERATORS(${decl.name})"
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
            |${decl.constants.joinToString(separator = eol) { """case ${decl.name}::${it.name.sanitize()}: return "${it.name}";""" }}
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
    protected fun PrettyPrinter.includesDecl(marshallerHeaders: List<String>) {
//        +"class ${decl.name};"

        val standardHeaders = listOf(
                "cstring",
                "cstdint",
                "vector",
                "ctime"
        )


        val frameworkHeaders = listOf(
                //root
                "Protocol",
                //types
                "types/DateTime",
                //impl
                "RdSignal",
                "RdProperty",
                "RdList",
                "RdSet",
                "RdMap",
                //base
                "ISerializable",
                "ISerializersOwner",
                "IUnknownInstance",
                //serialization
                "Polymorphic",
                "NullableSerializer",
                "ArraySerializer",
                "InternedSerializer",
                "SerializationCtx",
                "Serializers",
                //ext
                "RdExtBase",
                //task
                "RdCall",
                "RdEndpoint",
                "RdSymmetricCall",
                //std stubs
                "std/to_string",
                "std/hash",
                //enum
                "enum",
                //gen
                "gen_util"
        )

        +frameworkHeaders.joinToString(separator = eol) { s -> s.includeWithExtension("h") }
        println()
        +standardHeaders.joinToString(separator = eolKind.value, transform = { "#include <$it>" })
        println()
        //third-party
        +"thirdparty".includeWithExtension("hpp")

//        +INSTANTIATION_FILE_NAME.include("h")

        marshallerHeaders.forEach { it.includeAngleBrackets() }
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
            }
            return types.mapNotNull {
                when (it) {
                    is Root -> "../${it.name}/${it.name}.h"
                    is Declaration ->
                        if (!it.isIntrinsic) {
                            "../${it.pointcut!!.name}/${it.name}.h"
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
            val bases = listOfNotNull(decl.base?.name).map { "$it.h" }
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

        val extHeaders = listOfNotNull(if (decl.isExtension) decl.pointcut?.name else null)
        extHeaders.printlnWithBlankLine { it.includeWithExtension("h") }
        dependentTypes(decl).printlnWithBlankLine { it.includeQuotes() }

        decl.getSetting(MarshallerHeaders)?.distinct()?.println { it.includeQuotes() }
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
        +bases(decl).joinToString(separator = ", ", prefix = ": ") { "public ${it.type.name}" }
    }


    private fun createMethodTraitDecl(decl: Toplevel): Signature? {
        if (decl.isExtension) return null
        return MemberFunction("void", "connect(rd::Lifetime lifetime, rd::IProtocol const * protocol)", decl.name)
    }

    fun PrettyPrinter.customSerializersTrait(decl: Declaration) {
        fun IType.serializerBuilder(): String = leafSerializerRef(decl) ?: "rd::" + when (this) {
            is IArray -> "ArraySerializer<${itemType.serializerBuilder()}>"
            is IImmutableList -> "ArraySerializer<${itemType.serializerBuilder()}>"
            is INullable -> "NullableSerializer<${itemType.serializerBuilder()}>"
            is InternedScalar -> """InternedSerializer<${itemType.serializerBuilder()}, ${internKey.hash()}>"""
            else -> fail("Unknown type: $this")
        }

        private()
        val allTypesForDelegation = decl.allMembers
                .filterIsInstance<Member.Reactive>()
                .flatMap { it.genericParams.toList() }
                .distinct()
                .filter { it.leafSerializerRef(decl) == null }

        allTypesForDelegation.println { "using ${it.serializerRef(decl, false, false)} = ${it.serializerBuilder()};" }
    }


    private fun abstractDeclarationTraitDecl(decl: Declaration): MemberFunction {
        return MemberFunction(decl.name.wrapper(), "readUnknownInstance(rd::SerializationCtx& ctx, rd::Buffer &buffer, rd::RdId const &unknownId, int32_t size)", decl.name).override()
    }

    protected fun PrettyPrinter.registerSerializersTraitDecl(decl: Declaration) {
        val serializersOwnerImplName = "${decl.name}SerializersOwner"
        public()
        block("struct $serializersOwnerImplName final : public rd::ISerializersOwner {", "};") {
            declare(MemberFunction("void", "registerSerializersCore(rd::Serializers const& serializers)", decl.name).const().override())
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
        /*if (decl.isAbstract) {
            println()
            declare(abstractDeclarationTraitDecl(decl))
        }*/
        if (decl is Toplevel) {
            println()
            registerSerializersTraitDecl(decl)
            println()
            public()
            declare(createMethodTraitDecl(decl))
            println()
        }
    }


    private fun extensionTraitDecl(decl: Ext): MemberFunction? {
        val pointcut = decl.pointcut ?: return null
//        val lowerName = decl.name.decapitalize()
//        val extName = decl.extName ?: lowerName
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
//        +"explicit ${decl.name}"
        val arguments = secondaryCtorParams(decl)
        return Constructor.Secondary(this, decl, arguments)
    }


    private fun PrettyPrinter.deconstructTrait(decl: Declaration) {
        if (shouldGenerateDeconstruct(decl)) {
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
            decl.isAbstract -> MemberFunction(decl.name.wrapper(), "readUnknownInstance(rd::SerializationCtx& ctx, rd::Buffer & buffer, rd::RdId const& unknownId, int32_t size)", decl.name).static()
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
//        val signature = MemberFunction("bool", "equals(${decl.name} const& other)", decl.name).const()
        val signature = MemberFunction("bool", "equals(rd::ISerializable const& object)", decl.name).const()
        return if (decl is Toplevel || decl.isAbstract) {
            null
        } else {
            signature.override()
        }
        /*return if (decl.isAbstract) {
            signature.abstract(decl)
        } else {
            if (decl is Toplevel) {
                signature
            } else {
                signature.override()
            }
        }*/
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

    private fun polymorphicToStringTraitDecl(decl: Declaration): MemberFunction? {
        val signature = MemberFunction("std::string", "toString()", decl.name).const()
        return when {
            decl is Toplevel -> signature.override()
            decl.isAbstract -> signature.override()
            decl.isConcrete -> signature.override()
            else -> signature
        }
    }

    private fun externalToStringTraitDecl(decl: Declaration): MemberFunction? {
//        if (!(decl is Toplevel)) return null

        return MemberFunction("std::string", "to_string(const ${decl.name} & value)", null).friend()
    }

    protected fun PrettyPrinter.hashSpecialization(decl: Declaration) {
        if (decl !is IScalar) return
        if (decl is Enum) return

        block("namespace rd {", "}") {
            block("template <> struct hash<${decl.withNamespace()}> {", "};") {
                block("size_t operator()(const ${decl.withNamespace()} & value) const noexcept {", "}") {
                    +"return value.hashCode();"
                }
            }
        }
    }

    private fun PrettyPrinter.tupleSpecialization(decl: Declaration) {
        if (shouldGenerateDeconstruct(decl)) {
            val n = decl.ownMembers.size
            titledBlock("namespace std") {
                +"template<>"
                +"class tuple_size<${decl.withNamespace()}> : public integral_constant<size_t, $n> {};"
                println()
                +"""
                    |template<size_t I>
                    |class std::tuple_element<I, ${decl.withNamespace()}> {
                    |public:
                    |    using type = decltype (declval<${decl.withNamespace()}>().get<I>());
                    |};""".trimMargin()
            }
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

    private val Enum.underscoreSetOrEmpty
        get() = flags.condstr { "_set" }

    private fun PrettyPrinter.readerBodyTrait(decl: Declaration) {
        fun IType.reader(): String = when (this) {
            is Enum -> "buffer.read_enum${underscoreSetOrEmpty}<${templateName(decl)}>()"
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
            is PredefinedType -> "buffer.read${name.capitalize()}()"
            is Declaration -> {
                if (isIntrinsic) {
                    "rd::Polymorphic<$name>::read(ctx, buffer)"
                } else {
                    if (isAbstract)
                        "ctx.get_serializers().readPolymorphic<${templateName(decl)}>(ctx, buffer)"
                    else
                        "${templateName(decl)}::read(ctx, buffer)"
                }
            }
            is INullable -> {
                val lambda = lambda(null, "return ${itemType.reader()}")
                """buffer.read_nullable<${itemType.templateName(decl)}>($lambda)"""
            }
            is IArray, is IImmutableList -> { //awaiting superinterfaces' support in Kotlin
                this as IHasItemType
                if (isPrimitivesArray) {
                    "buffer.read_array<${itemType.templateName(decl)}>()"
                } else {
                    """buffer.read_array<${itemType.templateName(decl)}>(${lambda(null, "return ${itemType.reader()}")})"""
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

    private fun PrettyPrinter.abstractDeclarationTraitDef(decl: Declaration) {
        define(abstractDeclarationTraitDecl(decl)) {
            readerBodyTrait(unknown(decl)!!)
        }
    }

    protected fun PrettyPrinter.registerSerializersTraitDef(decl: Toplevel, types: List<Declaration>) {//todo name access
        val serializersOwnerImplName = "${decl.name}SerializersOwner"
        +"${decl.name}::$serializersOwnerImplName const ${decl.name}::serializersOwner;"
        println()
        define(MemberFunction("void", "registerSerializersCore(rd::Serializers const& serializers)", "${decl.name}::${decl.name}SerializersOwner").const().override()) {
            types.filter { !it.isAbstract }
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
            } else if (decl.isAbstract) {
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
        fun IType.writer(field: String): String {
            return when (this) {
                is CppIntrinsicType -> "rd::Polymorphic<$name>::write(ctx, buffer, $field)"
                is Enum -> {
                    "buffer.write_enum${underscoreSetOrEmpty}($field)"
                }
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
                    if (isAbstract)
                        "ctx.get_serializers().writePolymorphic<${templateName(decl)}>(ctx, buffer, $field)"
                    else {
                        "rd::Polymorphic<std::decay_t<decltype($field)>>::write(ctx, buffer, $field)"
                    }
                is INullable -> {
                    val lambda = lambda("${itemType.substitutedName(decl)} const & it", itemType.writer("it"), "void")
                    "buffer.write_nullable<${itemType.templateName(decl)}>($field, $lambda)"
                }
                is IArray, is IImmutableList -> { //awaiting superinterfaces' support in Kotlin
                    this as IHasItemType
                    if (isPrimitivesArray) {
                        "buffer.write_array($field)"
                    } else {
                        val lambda = lambda("${itemType.substitutedName(decl)} const & it", itemType.writer("it"), "void")
                        "buffer.write_array<${itemType.substitutedName(decl)}>($field, $lambda)"
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
                /*if (member is Member.Field) {
                    +"return wrapper::get<${member.implTemplateName(decl)}>(${member.encapsulatedName});"
                }*/
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
                    .filterIsInstance<Member.Reactive.Stateful.Property>()
                    .println { "${it.encapsulatedName}.is_master = ${it.master};" }

            decl.ownMembers
                    .filterIsInstance<Member.Reactive.Stateful.Map>()
                    .println { "${it.encapsulatedName}.is_master = ${it.master};" }

            decl.ownMembers
                    .filterIsInstance<Member.Reactive>()
                    .filter { it.freeThreaded }
                    .println { "${it.encapsulatedName}.async = true;" }

            /*decl.ownMembers
                    .filter { it.isBindable }
                    .println { """bindable_children.emplace_back("${it.name}", &${it.encapsulatedName});""" }*/

            if (decl is Toplevel) {
                +"serializationHash = ${decl.serializationHash(IncrementalHash64()).result}L;"
            }
        }
    }


    private fun PrettyPrinter.equalsTraitDef(decl: Declaration) {
        define(equalsTraitDecl(decl)) {
            +"auto const &other = dynamic_cast<${decl.name} const&>(object);"
            if (decl.isAbstract || decl !is IScalar) {
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
        polymorphicToStringTraitDecl(decl)?.let { function ->
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
        externalToStringTraitDecl(decl)?.let {
            define(it) {
                +"return value.toString();"
            }
        }
    }


    protected fun PrettyPrinter.extensionTraitDef(decl: Ext) {//todo
        define(extensionTraitDecl(decl)) {
            val lowerName = decl.name.decapitalize()
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
            is Member.Reactive.Stateful.Property -> when {
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
                    is Enum -> "${member.type.name}::${value.sanitize()}"
                    else -> value
                }
            }
            //                is Member.Reactive.Stateful.Extension -> member.delegatedBy.sanitizedName(containing) + "()"
            else -> null
        }
    }

    private fun PrettyPrinter.ifDefDirective(feature: String, printer: PrettyPrinter.() -> Unit) {
        +"#ifdef $feature"
        indent(printer)
        +"#endif"
    }

    private fun PrettyPrinter.ifDirective(feature: String, printer: PrettyPrinter.() -> Unit) {
        +"#if $feature"
        indent(printer)
        +"#endif"
    }

    private fun PrettyPrinter.ifDefLanguageVersionAtLeast(version: String, printer: PrettyPrinter.() -> Unit) {
        ifDirective("__cplusplus >= $version", printer)
    }

    override fun toString(): String {
        return "Cpp17Generator(flowTransform=$flowTransform, defaultNamespace='$defaultNamespace', folder=${folder.canonicalPath}, usingPrecompiledHeaders=$usingPrecompiledHeaders)"
    }
}