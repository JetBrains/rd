import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.withId
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.impl.RdSignal
import com.jetbrains.rider.util.ConsoleLoggerFactory
import com.jetbrains.rider.util.LogLevel
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IScheduler
import kotlin.browser.window

object Scheduler : IScheduler {
    override fun flush() {}

    override fun queue(action: () -> Unit) {
        action()
    }

    override val isActive: Boolean
        get() = true
}

@Suppress("unused")
fun main(args: Array<String>) {
    val lifetime = Lifetime.Eternal
    val scheduler = Scheduler
    ConsoleLoggerFactory.level = LogLevel.Info

    val wire = WebSocketWire.Client(lifetime, scheduler, 33333)
    val protocol = Protocol(Serializers(), Identities(IdKind.Client), scheduler, wire)

    val intProperty = property(lifetime, protocol, FrameworkMarshallers.Int, 1, "int", 0)
    val intArray = property(lifetime, protocol, FrameworkMarshallers.IntArray, 2, "intArr", IntArray(0))
    val longArray = property(lifetime, protocol, FrameworkMarshallers.LongArray, 3, "longArr", LongArray(0))
    val string = property(lifetime, protocol, FrameworkMarshallers.String, 4, "string", "")
    val char = property(lifetime, protocol, FrameworkMarshallers.Char, 5, "char", '0')
    val bool = property(lifetime, protocol, FrameworkMarshallers.Bool, 6, "bool", false)
    val byteArray = property(lifetime, protocol, FrameworkMarshallers.ByteArray, 7, "byteArray", ByteArray(0))
    val byteArraySignal = RdSignal(FrameworkMarshallers.ByteArray)
            .withId(RdId(8))
            .apply { bind(lifetime, protocol, "byteArraySignal") }

    byteArraySignal.advise(lifetime, {
        it.size
    });

    var lastSecondReceived = 0
    var lastSecondSend = 0
    window.setInterval({
        println("In "+(wire.bytesReceived - lastSecondReceived).toString() + "b/sec")
        println("Out "+(wire.bytesSend - lastSecondSend).toString() + "b/sec")
        lastSecondReceived = wire.bytesReceived
        lastSecondSend = wire.bytesSend
    }, 1000)

//    window.setInterval({
//        println("queueing")
//        intProperty.set(intProperty.value + 5)
//        intArray.set(intArray.value.map { -it }.toIntArray())
//        longArray.set(longArray.value.map { -it }.toLongArray())
//        string.set("!" + string.value + "!")
//        char.set(char.value + 1)
//        bool.set(!bool.value)
//    }, 5000)
}

private fun<T> property(lifetime: Lifetime, protocol: Protocol, marshaller: ISerializer<T>, protoId: Long, protoName: String, defaultValue : T): RdProperty<T> {
    return RdProperty(defaultValue, marshaller).withId(RdId(protoId))
            .apply {
                bind(lifetime, protocol, protoName)
                //advise(lifetime, { println(it.printToString()) })
            }
}