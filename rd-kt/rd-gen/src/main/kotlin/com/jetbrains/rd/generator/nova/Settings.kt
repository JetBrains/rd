@file:Suppress("UNCHECKED_CAST")

package com.jetbrains.rd.generator.nova

/**
 * Additional settings for rd model. Usually they are specific to [IGenerator] (because lead to generated code customization) and declared in generator's implementation
 * [T] type of setting value
 * [S] type of rd model entity to which this setting is applicable, e.g. [Declaration] or [Member]
 *
 * Example: [com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator.Attributes] that specifies kotlin annotations to generate upon class.
 */
interface ISetting<out T : Any, out S: SettingsHolder>

/**
 * [ISetting] that has default value
 */
abstract class SettingWithDefault<out T : Any, out S: SettingsHolder>(val default : T) : ISetting<T, S>

/**
 * Base class for any rd model entity (e.g. [Declaration] or [Member]) that can be customized
 */
open class SettingsHolder {
    internal val settings = mutableMapOf<ISetting<*, *>, Any>()
}

/**
 * Global static that specifies which generator is now executed. Implementation MUST NOT run generators in parallel. Used in [getSetting] to find setting for specific generator instance.
 */
internal var settingCtx : IGenerator? = null

/**
 * Storage for [forGenerator] method
 */
internal val genInstanceKeys = mutableMapOf<Pair<IGenerator, ISetting<*,*>>, ISetting<*,*>>()

/**
 * Creates new setting, specialized for [generator]. In case we have two generator instances with same type
 * for one model (e.g. [com.jetbrains.rd.generator.nova.csharp.CSharp50Generator.FlowTransformProperty] which is different for client and server)
 * you need to distinguish this settings for different generation processes (e.g. client and server).
 *
 * Example of use:
 * setting(com.jetbrains.rd.generator.nova.csharp.CSharp50Generator.FlowTransformProperty.forGenerator(myClientGenerator), FlowTransform.AsIs)
 * setting(com.jetbrains.rd.generator.nova.csharp.CSharp50Generator.FlowTransformProperty.forGenerator(myServerGenerator), FlowTransform.Reversed)
 */
fun <T:Any, S:SettingsHolder> ISetting<T, S>.forGenerator(generator: IGenerator) : ISetting<T, S> =
        genInstanceKeys.getOrPut(generator to this) { object : ISetting<T,S> {} } as ISetting<T, S>


/**
 * Set setting
 */
fun <T: Any, S : SettingsHolder> S.setting(key: ISetting<T, S>, value: T) : S = apply { settings[key] = value }
fun <T: Any, S : SettingsHolder> S.setting(key: SettingWithDefault<T, S>, value: T = key.default) : S = setting(key as ISetting<T, S>, value)

/**
 * Should be called inside generation process ([IGenerator.generate]).
 * Method tries to obtain following  setting from [SettingsHolder.settings] in following order:
 * 1. For current rd model entity [this]:[S]  and current generator [settingCtx] (new setting [key.forGenerator(settingCtx)]
 * 2. For current rd model entity [this]:[S]
 * 3. For [Declaration] it search not specialized property (without [forGenerator]) in parent's [SettingsHolder.settings] of current rd model entity, i.e. [Declaration.pointcut] (e.g. [Ext] or [Root])
 */
fun <T: Any, S : SettingsHolder>  S.getSetting(key: ISetting<T, S>) : T? {
    val specializedKey = settingCtx?.let { key.forGenerator(it) }

    return if (this is Declaration) {
        specializedKey?.let { this.getSettingInHierarchy(specializedKey)}
                ?:this.getSettingInHierarchy(key)
    }
    else {
        specializedKey?.let { settings[specializedKey] as T? }
                ?: settings[key] as T?
    }
}


/**
 * Shortcut to set [Unit] setting. In most cases you should use [Unit] settings instead of [Boolean] ones.
 */
fun <S : SettingsHolder> S.setting(key: ISetting<Unit, S>) = setting(key, Unit)

/**
 * Shortcut to get [Unit] setting. In most cases you should use [Unit] settings instead of [Boolean] ones.
 */

fun <S : SettingsHolder> S.hasSetting(key: ISetting<Unit, S>) : Boolean = getSetting(key) != null
