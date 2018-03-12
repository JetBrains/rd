@file:Suppress("UNCHECKED_CAST")

package com.jetbrains.rider.generator.nova

//todo until https://youtrack.jetbrains.com/issue/KT-15745 is fixed
@Suppress("unused")
interface ISetting<out T : Any, out S: SettingsHolder>
abstract class SettingWithDefault<out T : Any, out S: SettingsHolder>(val default : T) : ISetting<T, S>

open class SettingsHolder {
    internal val settings = mutableMapOf<ISetting<*, *>, Any>()
}

fun <T: Any, S : SettingsHolder> S.setting(key: ISetting<T, S>, value: T) = apply { settings[key] = value }
fun <T: Any, S : SettingsHolder> S.setting(key: SettingWithDefault<T, S>, value: T = key.default) = setting(key as ISetting<T, S>, value)

fun <T: Any, S : SettingsHolder>  S.getSetting(key: ISetting<T, S>) : T? = if (this is Declaration) this.getInheritedSetting(key) else settings[key] as? T?


fun <S : SettingsHolder> S.setting(key: ISetting<Unit, S>) = setting(key, Unit)
fun <S : SettingsHolder> S.hasSetting(key: ISetting<Unit, S>) : Boolean = getSetting(key) != null
