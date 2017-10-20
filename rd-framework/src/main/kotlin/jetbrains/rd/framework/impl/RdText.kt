package com.jetbrains.rider.framework.impl

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdReactiveBase
import com.jetbrains.rider.framework.base.withId
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.AddRemove
import com.jetbrains.rider.util.reactive.Signal
import com.jetbrains.rider.util.reactive.ViewableList
import jetbrains.rd.util.logger.logger
import org.apache.commons.logging.LogFactory
import java.io.InputStream
import java.io.OutputStream

class RdTextBuffer(var initialText: String? = null,
                   val origin: OriginKind = OriginKind.Local
) : RdReactiveBase() {

    val isMaster: Boolean = true
    /**
     * Slave of the text buffer supports a list of changes that were introduced locally and can be rolled back when master buffer reports
     * incompatible change
     */
    private val changesToConfirmOrRollback = arrayListOf<RdTextReplace>()
    private val textChanged = Signal<RdTextReplace>()

    val historyChanged: Signal<RdTextReplace> = Signal()
    //optimization to concatenate hold changes into the single one
    private val queuedChanges: ViewableList<RdTextReplace> = ViewableList()

    var bufferVersion: TextBufferVersion = TextBufferVersion(versionInitial,versionInitial)
        private set

    companion object : IMarshaller<RdTextBuffer> {

        private val logger = logger<RdTextBuffer>()

        override val _type: Class<*> get() = throw IllegalStateException("Mustn't be used for polymorphic marshalling")

        override fun read(ctx : SerializationCtx, stream: InputStream): RdTextBuffer {
            val id = RdId.read(stream)
            val text = stream.readString()
            return RdTextBuffer(text, origin = OriginKind.Remote).withId(id)
        }

        override fun write(ctx: SerializationCtx, stream: OutputStream, value: RdTextBuffer) {
            stream.writeRdId(value.id)
            stream.writeString(value.initialText!!)
            value.initialText = null
        }
        //val instance = Logger.getInstance(RdTextBuffer.javaClass)
        //val logger = instance
        private var versionInitial = -1

    }

    override fun init(lifetime: Lifetime) {
        super.init(lifetime)
        queuedChanges.advise(lifetime){ addRemove, rdTextReplace, i ->
            if (addRemove == AddRemove.Add) {
                historyChanged.fire(rdTextReplace)
            }
        }

        textChanged.advise(lifetime){ rdTextReplace ->
            historyChanged.fire(rdTextReplace)
        }

        protocol.wire.advise(lifetime, id) { stream -> changeFromWire(stream) }
    }

    private fun changeFromWire(stream: InputStream) {
        val masterVersionRemote = stream.readInt()
        val slaveVersionRemote = stream.readInt()
        val start = stream.readInt()
        val oldValue = stream.readNullableString()
        val newValue = stream.readString()
        val fullTextLength = stream.readInt()
        val fullText = stream.readNullableString()
        val rdTextReplace = RdTextReplace(start, oldValue, newValue, OriginKind.Remote, fullTextLength, fullText)
        if (isMaster) {
            //logger.assertTrue(changesToConfirmOrRollback.isEmpty())
            if (masterVersionRemote != bufferVersion.master) {
                logger.warn("Rejecting the change '$newValue'")
                // reject the change. we've already sent overriding change.
            } else {
                // apply.
                bufferVersion = TextBufferVersion(masterVersionRemote, slaveVersionRemote)
                textChanged.fire(rdTextReplace)
            }
        } else {
            if (slaveVersionRemote != bufferVersion.slave) {
                // rollback the changes and notify external subscribers
                for (change in changesToConfirmOrRollback.reversed()) {
                    textChanged.fire(change.reverse())
                }

                bufferVersion = TextBufferVersion(masterVersionRemote, slaveVersionRemote)
                textChanged.fire(rdTextReplace)

            } else {
                // confirm the changes queue.
                changesToConfirmOrRollback.clear()
                bufferVersion = TextBufferVersion(masterVersionRemote, slaveVersionRemote)
                textChanged.fire(rdTextReplace)
            }
        }
    }

    fun recover(text: String) {
        val change = RdTextReplace(0, null, text, OriginKind.Local, text.length)
        queueChange(change)
        flushQueuedChanges()
    }

    fun fire(change: RdTextReplace, queueWireDispatch : Boolean){
        assertThreading()

        if (isMaster) {
            if (queueWireDispatch) {
                queueChange(change)
            }
            else {
                require(queuedChanges.isEmpty()){
                    "queuedChanges queue is not empty"
                }
                queueChange(change)
                flushQueuedChanges()
            }
        }
        else {
            bufferVersion = bufferVersion.incrementSlave()
            changesToConfirmOrRollback.add(change)
        }
    }

    fun flushQueuedChanges() {
        try{
            logger.debug("Sending ${queuedChanges.count()} queued changes")
            require(!queuedChanges.isEmpty()) { "queuedChanges should not be empty" }

            for (queuedChange in queuedChanges) {
                doSendOnWire(queuedChange)
            }
        }
        finally{
            queuedChanges.clear()
        }
    }

    private fun doSendOnWire(queuedChange: RdTextReplace) {
        //master version should be increment before during obligatory queueChange step
        val change = queuedChange
        textChanged.fire(change)
        protocol.wire.send(id, { stream ->
            stream.writeInt(bufferVersion.master)
            stream.writeInt(bufferVersion.slave)
            stream.writeInt(change.start)
            stream.writeNullableString(change.oldValue)
            stream.writeString(change.newValue)
            stream.writeInt(change.fullTextLength)
            stream.writeNullableString(change.fullText)
        })
    }

    //the first item is aggregated by subsequent typing, others (i.e. bulk external changes for the doc) - are just queued
    private fun queueChange(newChange: RdTextReplace) {
        if(queuedChanges.isEmpty()) {
            bufferVersion = bufferVersion.incrementMaster()
            queuedChanges.add(newChange)
            return
        }

        val firstQueuedChange = queuedChanges.first()
        //queuedChanges.size > 1 is very important - we have no right to aggregate anything since we have multiple doc changes
        if (queuedChanges.size > 1 || newChange.start != firstQueuedChange.start + firstQueuedChange.newValue.length
            || newChange.newValue.length != 1 || firstQueuedChange.newValue.length != 1) {
            //we got irrelevant document change, don't aggregate it, just queue
            //(we would need range markers to correctly do this useless thing)
            bufferVersion = bufferVersion.incrementMaster()
            queuedChanges.add(newChange)
        }
        else {
            //we got change that can be concatenated to the first one. do it!
            val sb = StringBuilder(firstQueuedChange.newValue)
            val initialOffset = firstQueuedChange.start
            val initialFullLength = firstQueuedChange.fullTextLength + newChange.newValue.length
            sb.append(newChange.newValue)

            queuedChanges.remove(firstQueuedChange)
            queuedChanges.add(RdTextReplace(initialOffset, firstQueuedChange.oldValue, sb.toString(), OriginKind.Local, initialFullLength))
        }
    }

    fun advise(lifetime: Lifetime, change: (RdTextReplace) -> Unit) {
        textChanged.advise(lifetime, change)
    }

    override fun toString(): String {
        return "Text Buffer Stateless: ($id)"
    }
}

data class Range (val start: Int, val end: Int) {
    companion object {
        val invalidRange = Range(-1,-1)
    }

    val isNormalized: Boolean = this.start <= this.end

    fun shift(offset: Int) = Range(this.start + offset, this.end + offset)
}


data class TextBufferVersion(val master: Int, val slave: Int) : Comparable<TextBufferVersion> {
    fun incrementMaster() = TextBufferVersion(master + 1, slave)
    fun incrementSlave() = TextBufferVersion(master, slave + 1)

    override fun compareTo(other: TextBufferVersion): Int {
        val masterCompare = master.compareTo(other.master)
        if (masterCompare != 0) return masterCompare
        return slave.compareTo(other.slave)
    }
}

data class RdTextReplace(
        val start: Int,
        val oldValue: String?,
        val newValue: String,
        val origin: OriginKind,
        val fullTextLength: Int,
        val fullText: String? = null)
{
    override fun toString(): String {
        return "start: '$start', oldValue: '$oldValue', newValue: '$newValue', origin: $origin"
    }
}

fun RdTextReplace.reverse(): RdTextReplace {
    requireNotNull(oldValue) { "old value should not be null to reverse document change" }
    return RdTextReplace(this.start, newValue, oldValue ?: "", OriginKind.Remote, fullTextLength, fullText)
}