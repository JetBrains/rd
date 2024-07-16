package com.jetbrains.rd.generator.nova

import java.io.File


interface MarshallersCollector {
    val shouldGenerateRegistrations: Boolean

    fun addMarshaller(fqn: String, rdid: Long)
}

object DisabledMarshallersCollector : MarshallersCollector {
    override val shouldGenerateRegistrations: Boolean
        get() = true

    override fun addMarshaller(fqn: String, rdid: Long) {
    }
}

class RealMarshallersCollector(val marshallersFile: File) : MarshallersCollector {
    private val marshallers = mutableSetOf<String>()

    override val shouldGenerateRegistrations: Boolean
        get() = false // We may want to add a separate setting here, but for now just disable it

    override fun addMarshaller(fqn: String, rdid: Long) {
        marshallers.add("${rdid}:${fqn}")
    }

    fun close() {
        marshallersFile.parentFile.mkdirs()
        marshallersFile.writer().use { writer ->
            marshallers.sorted().forEach {
                writer.append(it).append("\n")
            }
        }
    }
}