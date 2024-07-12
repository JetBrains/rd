package com.jetbrains.rd.generator.nova

import com.jetbrains.rd.util.hash.getPlatformIndependentHash
import java.io.File


interface MarshallersCollector {
    val shouldGenerateRegistrations: Boolean

    fun addMarshaller(namespace: String, name: String)
}

object DisabledMarshallersCollector : MarshallersCollector {
    override val shouldGenerateRegistrations: Boolean
        get() = true

    override fun addMarshaller(namespace: String, name: String) {
    }
}

class RealMarshallersCollector(val marshallersFile: File) : MarshallersCollector {
    private val marshallers = mutableSetOf<String>()

    override val shouldGenerateRegistrations: Boolean
        get() = false // We may want to add a separate setting here, but for now just disable it

    override fun addMarshaller(namespace: String, name: String) {
        marshallers.add("${name.getPlatformIndependentHash()}:${namespace}.${name}")
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