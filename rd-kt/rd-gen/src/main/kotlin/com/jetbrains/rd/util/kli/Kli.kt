package com.jetbrains.rd.util.kli
import com.jetbrains.rd.util.kli.ParseState.*
import com.jetbrains.rd.util.string.PrettyPrinter
import java.nio.file.Path
import java.nio.file.Paths


internal enum class ParseState {
    Init,
    StartCmd,
    Minus,
    DoubleMinus,
    ShortOption,
    LongOption,
    Argument,
    Fin
}


sealed class Option<T : Any> constructor(val short: Char?, val long: String?, val description: String, val defaultValue: T?){

    internal var _value : T? = defaultValue
    open val value: T? get() = _value
    operator fun timesAssign(v: T) { _value = v }

    internal fun reset() { _value = defaultValue }


    class Flag (
        short: Char?,
        long: String?,
        description: String

    ) : Option<Boolean>(short, long, description, false) {

        override val value: Boolean get() = super.value!!
        operator fun unaryPlus() = value
    }

    class Valued<T : Any> (
        short: Char?,
        long: String?,
        description: String,
        private val deserialize: (String) -> T,
        defaultValue: T?

    ) : Option<T>(short, long, description, defaultValue) {


        fun tryParse(rawValue: String) : Boolean {
            return try {
                parse(rawValue)
                true
            } catch (e: Exception) {
                false
            }
        }

        fun parse(rawValue: String?) {
            if (rawValue == null) return
            _value = deserialize(rawValue)
        }

        operator fun unaryPlus() = value
    }

    init {
        require(short != null || long != null) { "Both 'short' and 'long' are nulls" }
        require(long == null || long.length > 1) { "'long'.length must be greater than 1, but we have '$long'" }
    }
}


class Argument<T> (val name: String, val description: String, private val deserialize: (String) -> T) {
    var value: T? = null
        internal set

    fun parseValue(rawValue: String) : Boolean {
        return try {
            value = deserialize(rawValue)
            true
        } catch (e: Exception) {
            false
        }
    }

    operator fun unaryPlus() = value
    override fun toString(): String = name
}



abstract class Kli() {

    internal class PushBackCharIterator(val str: String) {
        var idx = -1
            private set
        val rem get() = str.substring(idx+1)

        val cur: Char get() = str[idx]
        fun moveNext() = (++idx) in 0..str.lastIndex
        fun pushBack() = --idx

        override fun toString(): String = "PushBackCharIterator(str='$str', idx=$idx)"
    }


    val options = ArrayList<Option<*>>()
    val arguments = ArrayList<Argument<*>>()
    private var state: ParseState = Init

    open val usage : String get() = "java -cp [CLASSPATH] ${javaClass.`package`.name}.MainKt [OPTION]... ${arguments.joinToString( " "){it.name}}"
    abstract val description: String
    abstract val comments: String

    var error: String? = null

    internal fun resetAndParse(vararg cmdline: String) {
        reset()
        parse(arrayOf(*cmdline))
    }


    fun reset() {
        error = null
        options.forEach { it.reset() }
        arguments.forEach { it.value = null }
    }

    fun parse(cmdline: Array<String>): String? {
        require (state == Init) { "Parsed already" }

        var argumentIdx = 0

        val commands = cmdline.iterator()
        var chars = PushBackCharIterator("") //fake

        val err = { msg:String ->
            error = msg
            Fin
        }

        val parseOption = { option: Option<*>?, presentableName: String ->
            if (option == null) err("Unknown option: $presentableName")
            else when(option) {
                is Option.Flag -> {
                    option._value = true
                    ShortOption
                }
                is Option.Valued<*> -> {
                    val rawValue =
                        if (chars.rem.isEmpty())
                            if (commands.hasNext()) commands.next()
                            else null
                        else
                            chars.rem

                    if (rawValue == null) err("No value provided for option $presentableName")
                    else if (!option.tryParse(rawValue)) err("Can't parse value '$rawValue' for option $presentableName")
                    else StartCmd
                }
            }
        }

        cycle@while (true) {

            state = when (state) {

                Init -> StartCmd

                StartCmd ->
                    if (!commands.hasNext()) Fin
                    else {
                        chars = PushBackCharIterator(commands.next())

                        if (!chars.moveNext()) err("Empty argument")
                        else if (chars.cur == '-') Minus
                        else {
                            chars.pushBack()
                            Argument
                        }
                    }

                Minus ->
                    if (!chars.moveNext()) err("Standalone '-' is not allowed")
                    else if (chars.cur == '-') DoubleMinus
                    else {
                        chars.pushBack()
                        ShortOption
                    }

                DoubleMinus ->
                    if (!chars.moveNext()) err("Standalone '--' is not allowed")
                    else if (chars.cur == '-') err("Triple minus '---' is not allowed")
                    else {
                        chars.pushBack()
                        LongOption
                    }

                ShortOption ->
                    if (!chars.moveNext()) StartCmd //if it's not first long option in a row
                    else {
                        val option = options.firstOrNull {it.short == chars.cur}
                        parseOption(option, "'-${chars.cur}'")
                    }

                LongOption -> {
                    val rawName = buildString {
                        while (chars.moveNext()) {
                            if (chars.cur == '=') break
                            else append(chars.cur)
                        }
                    }
                    if (rawName.isEmpty()) err("Invalid option pattern: '${chars.str}'")
                    else {
                        val option = options.firstOrNull {it.long == rawName}
                        parseOption(option, "'--$rawName'")
                    }
                }

                Argument -> {
                    val rawValue = chars.rem
                    if (argumentIdx >= arguments.size) err("Too many arguments: '$rawValue'")
                    else {
                        val argument = arguments[argumentIdx++]
                        if (!argument.parseValue(rawValue)) err("Can't parse value '$rawValue' for argument '${argument.name}'")
                        else StartCmd
                    }
                }


                Fin -> break@cycle
            }
        }

        if (error == null && argumentIdx < arguments.size) error = "Value of argument '${arguments[argumentIdx].name}' undefined"
        return error
    }


    fun help() : String = PrettyPrinter().apply {
        + "Usage: $usage"
        + description
        println()

        + "Options:"
        indent {
            for (o in options) {
                if (o.short != null) {
                    p("-${o.short}")
                    if (o.long != null) p(", ")
                } else {
                    p("    ")
                }

                if(o.long != null) {
                    p("--${o.long}")
                    if (o is Option.Valued<*>) {
                        p("=<${o.long}>")
                    }
                } else if (o is Option.Valued<*>) { //marginal case
                    p(" VALUE")
                }

                pad(28) //+2 for indent
                print(o.description)
                if (o is Option.Valued<*> && o.defaultValue != null)
                    print("; default: ${o.defaultValue}")

                println()
            }
        }

        println()
        + "Arguments:"
        indent {
            for (arg in arguments) {
                p(arg.name)
                pad(28)
                println(description)
            }
        }

        println()
        + comments

    }.toString()

    open fun validate() {}


    fun option_flag(short: Char?, long: String?, description: String) : Option.Flag = Option.Flag(short, long, description).apply { options.add(this) }

    fun <T:Any> option(short: Char?, long: String?, description: String, defaultValue : T?, deserialize: (String) -> T) = Option.Valued(short, long, description, deserialize, defaultValue).apply { options.add(this) }
    fun option_string(short: Char?, long: String?, description: String, defaultValue : String? = null) = option(short, long, description, defaultValue) { it }
    fun option_int(short: Char?, long: String?, description: String, defaultValue : Int? = null)   = option(short, long, description, defaultValue) { it.toInt() }
    fun option_long(short: Char?, long: String?, description: String, defaultValue : Long? = null) = option(short, long, description, defaultValue) { it.toLong() }
    fun option_path(short: Char?, long: String?, description: String, defaultValue : Path? = null) = option(short, long, description, defaultValue) { Paths.get(it) }

    fun <T> arg(name:String, description: String, deserialize: (String) -> T) = Argument(name, description, deserialize).apply { arguments.add(this) }
}



