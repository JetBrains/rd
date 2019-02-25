@file:Suppress("UNCHECKED_CAST")

package com.jetbrains.rd.generator.nova


//todo until https://youtrack.jetbrains.com/issue/KT-15745 is fixed
@Suppress("unused")
interface ISetting<out T : Any, out S: SettingsHolder>


abstract class SettingWithDefault<out T : Any, out S: SettingsHolder>(val default : T) : ISetting<T, S>

open class SettingsHolder {
    internal val settings = mutableMapOf<ISetting<*, *>, Any>()
}

internal var settingCtx: IGenerator? = null
typealias GeneratorPredicate = (IGenerator) -> Boolean

internal val genInstanceKeys = mutableMapOf<Pair<GeneratorPredicate, ISetting<*,*>>, ISetting<*,*>>()

fun <T:Any, S:SettingsHolder> ISetting<T, S>.forGenerator(generator: IGenerator) : ISetting<T, S> =
    genInstanceKeys.getOrPut({ key: IGenerator -> key == generator } to this) { object : ISetting<T,S> {} } as ISetting<T, S>

fun <T:Any, S:SettingsHolder> ISetting<T, S>.forFlowTransform(flowTransform: FlowTransform) : ISetting<T, S> =
    genInstanceKeys.getOrPut({ key: IGenerator -> key.flowTransform == flowTransform } to this) { object : ISetting<T,S> {} } as ISetting<T, S>


fun <T: Any, S : SettingsHolder> S.setting(key: ISetting<T, S>, value: T) = apply { settings[key] = value }
fun <T: Any, S : SettingsHolder> S.setting(key: SettingWithDefault<T, S>, value: T = key.default) = setting(key as ISetting<T, S>, value)

fun <T: Any, S : SettingsHolder>  S.getSetting(key: ISetting<T, S>) : T? {
    val specializedKey = settingCtx?.let { generator ->
        genInstanceKeys.entries.find { it.key.first(generator) && it.key.second == key }?.value as ISetting<T, S>?
    }

    return if (this is Declaration) {
        specializedKey?.let { this.getSettingInHierarchy(specializedKey)}
            ?:this.getSettingInHierarchy(key)
    }
    else {
        specializedKey?.let { settings[specializedKey] as T? }
            ?: settings[key] as T?
    }
}



fun <S : SettingsHolder> S.setting(key: ISetting<Unit, S>) = setting(key, Unit)
fun <S : SettingsHolder> S.hasSetting(key: ISetting<Unit, S>) : Boolean = getSetting(key) != null
